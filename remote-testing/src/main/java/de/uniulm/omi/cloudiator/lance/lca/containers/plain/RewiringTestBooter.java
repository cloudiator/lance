/*
 * Copyright (c) 2014-2018 University of Ulm
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

package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.LcaConstants;
import de.uniulm.omi.cloudiator.lance.lca.RewiringTestAgent;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.RewiringTestImpl;
import de.uniulm.omi.cloudiator.lance.util.Registrator;
import de.uniulm.omi.cloudiator.lance.util.Version;

//todo: get a base class out of it
//modified LcABooter.java
public class RewiringTestBooter {

    public final static String REWT_REGISTRY_KEY = "RewiringTestAgent";
    private final static Registrator<RewiringTestAgent> reg = Registrator.create(RewiringTestAgent.class);
    private static volatile RewiringTestImpl rwt = createRWImplementation();
    private static volatile RewiringTestAgent stub = reg.export(rwt, LcaConstants.AGENT_RMI_PORT);

    public static void main(String[] args) {
        if (stub != null && reg.addToRegistry(stub, REWT_REGISTRY_KEY)) {
            // from here on RMI takes over //
            Thread idle = new Thread(new IdleRunnable());
            idle.setDaemon(true);
            idle.start();
            Runtime.getRuntime().addShutdownHook(new Thread(
                    () -> {
                        unregister(rwt);
                        idle.interrupt();
                    }));
        } else {
            Runtime.getRuntime().exit(-128);
        }
    }

    private static RewiringTestImpl createRWImplementation() {
        RewiringTestImpl impl = new RewiringTestImpl();
        return impl;
    }

    public static void unregister(RewiringTestImpl impl) {
        reg.unregister(impl);
    }

    private static class IdleRunnable implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                Object obj = new Object();
                synchronized (obj) {
                    try {
                        obj.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
