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

package de.uniulm.omi.cloudiator.lance.lca.container.environment;

import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

public abstract class EnvironmentHandler {

    private final GlobalRegistryAccessor registryAccessor;
    private final ComponentInstanceId myId;
    
    public EnvironmentHandler(ComponentInstanceId _myId, GlobalRegistryAccessor accessor) {
        myId = _myId;
        registryAccessor = accessor;
    }
    
    /*
    abstract void exportPort();
    
    /** exposes id, local host IP, ... * /
    abstract void exportDefaultProperties();
    
    public void flushEnvironmentVariables(DockerShell _dshell) throws RegistrationException {
        String containerIp = registryAccessor.getProperty(myId, LcaRegistryConstants.LOCAL_IP);
        addEnvironmentVariable(_dshell, "TERM", "dumb");
        addEnvironmentVariable(_dshell, LcaRegistryConstants.LOCAL_IP.toUpperCase(), containerIp);
    }

    public void prepareConfgurationScript(String localIp) {
        try {
            registryAccessor.setProperty(myId, LcaRegistryConstants.LOCAL_IP, localIp);
            flushEnvironmentVariables(_dshell);
            flushOutgoingPorts(_dshell);
        } catch(RegistrationException re) {
            throw new RuntimeException(re);
        }
    }
    
    private void addEnvironmentVariables(DockerShell dshell, Map<String,String> map) {
        for(String key : map.keySet()) {
            String value = map.get(key);
            addEnvironmentVariable(dshell, key, value);
        }
    }
    */

}
