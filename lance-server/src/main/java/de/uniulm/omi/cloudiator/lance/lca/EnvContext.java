/*
 * Copyright (c) 2014-2015 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.uniulm.omi.cloudiator.lance.lca;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EnvContext implements HostContext {

    private final static Logger LOGGER = LoggerFactory.getLogger(HostContext.class);
    
    public static final String PUBLIC_IP_KEY = "host.ip.public";
    public static final String PRIVATE_IP_KEY = "host.ip.private";
    // public static final String HOST_OS_KEY = "host.os";
    public static final String TENANT_ID_KEY = "host.vm.cloud.tenant.id";
    public static final String VM_ID_KEY = "host.vm.id";
    public static final String CLOUD_ID_KEY = "host.vm.cloud.id";
    public static final String CONTAINER_TYPE = "host.container.type";
    
    private static final String[] VALUES = new String[] {
        PUBLIC_IP_KEY, PRIVATE_IP_KEY, /*HOST_OS_KEY, */ TENANT_ID_KEY, VM_ID_KEY, CONTAINER_TYPE
    };
    
    private final Map<String,String> hostContext;
    private final ScheduledExecutorService periodicExecutor = Executors.newScheduledThreadPool(4);
    private final ExecutorService executor = Executors.newScheduledThreadPool(4);
    
    EnvContext(Map<String,String> ctxParam) {
        hostContext = ctxParam;
    }
    
    private void registerRmiAddress() {
        LOGGER.info("setting RMI server hostname to: " + getPublicIp());
        System.setProperty("java.rmi.server.hostname", getPublicIp());
    }

    static HostContext fromEnvironment() {
        Map<String,String> values = new HashMap<>();
        for(String key : VALUES) {
            String s = System.getProperty(key);
            if(s == null || s.isEmpty()) {
                // s = "<unknown>";
                throw new IllegalStateException("property " + key + " needs to be set before LCA can start.");
            }
            values.put(key, s);
        }
        EnvContext ctx = new EnvContext(values);
        ctx.registerRmiAddress();
        return ctx;
    }

    @Override
    public String getPublicIp() { 
    	return hostContext.get(PUBLIC_IP_KEY); 
    }

    @Override
    public String getInternalIp() { 
    	return hostContext.get(PRIVATE_IP_KEY); 
    }

    @Override
    public String getContainerType() {
        return hostContext.get(CONTAINER_TYPE);
    }

    @Override
    public String toString() {
        return "HostContext: " + hostContext;
    }

    @Override
    public ScheduledFuture<?> scheduleAction(Runnable runner) {
        ScheduledFuture<?> sf = periodicExecutor.scheduleWithFixedDelay(runner, 30L, 60L, TimeUnit.SECONDS);
        return sf;
    }
    
    @Override @Deprecated
    public void run(Runnable runner) {
        executor.execute(runner);
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws InterruptedException {
        executor.shutdownNow();
        while(true) {
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if(executor.isTerminated()) {
                return;
            }
        }
    }

    @Override
    public String getCloudIdentifier() {
        return hostContext.get(CLOUD_ID_KEY);
    }
}
