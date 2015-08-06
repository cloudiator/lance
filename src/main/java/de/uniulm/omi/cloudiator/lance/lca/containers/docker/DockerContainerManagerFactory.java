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

package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.container.SpecificContainerManagerFactory;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortHierarchy;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortHierarchy.PortHierarchyBuilder;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortHierarchyLevel;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;

public enum DockerContainerManagerFactory implements SpecificContainerManagerFactory {

    INSTANCE {
        @Override
        public ContainerManager createContainerManager(HostContext vmId) {
            return new DockerContainerManager(vmId);
        }
    },
    
    REMOTE {
        @Override
        public ContainerManager createContainerManager(HostContext vmId) {
            if(! isRemoteAccessenabled()) 
            	throw new IllegalArgumentException("remote docker not supported"); 
            throw new UnsupportedOperationException("remote IP address needs to be defined.");
        }
    };
    
    static boolean isRemoteAccessenabled() {
        return remote_enabled;
    }
    
    public static void enableRemoteAccess() {
        remote_enabled = true;
    }

    private static volatile boolean remote_enabled = false;     


    private static final String PORT_HIERARCHY_2_NAME = "CONTAINER";
    static final PortHierarchyLevel PORT_HIERARCHY_2 = PortHierarchyLevel.create(PORT_HIERARCHY_2_NAME);
    public static final PortHierarchy DOCKER_PORT_HIERARCHY = new PortHierarchyBuilder().addLevel(PortRegistryTranslator.PORT_HIERARCHY_0).
                                                            addLevel(PortRegistryTranslator.PORT_HIERARCHY_1).addLevel(PORT_HIERARCHY_2).build();
}
