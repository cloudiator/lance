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

package de.uniulm.omi.cloudiator.lance.lca.container;

import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerContainerManagerFactory;


public enum ContainerType {
    
    DOCKER("dockering") {
        @Override
        public SpecificContainerManagerFactory getContainerFactory() {
            return DockerContainerManagerFactory.INSTANCE;
        }
    },
    
    DOCKER_REMOTE("docker-remote") {
        @Override
        public SpecificContainerManagerFactory getContainerFactory() {
            return DockerContainerManagerFactory.REMOTE;
        }
    },

    /*DUMMY("dummy") {
        @Override
        public SpecificContainerManagerFactory getContainerFactory() {
            return DummyContainerManagerFactory.INSTANCE;
        }
    },*/
    /**
     * will install the component in a dedicated directory, but will 
     * not use any other approach to ensure isolation between two 
     * components.
     */
    PLAIN("plain"){
        @Override
        public SpecificContainerManagerFactory getContainerFactory() {
            return UnsupportedTypeFactory.INSTANCE;
        }
    }
    ;
    
    private final String myName;
    
    private ContainerType(String _myName) {
        myName = _myName;
    }
    
    public abstract SpecificContainerManagerFactory getContainerFactory();

    /**
     * @param containername
     * @return returns the ContainerType matching the parameter. Null otherwise.
     */
    public static ContainerType fromString(String containername) {
        for(ContainerType t : ContainerType.values()) {
            if(t.myName.equalsIgnoreCase(containername)) return t;
        }
        return null;
    }
    
    @Override
    public String toString() {
        return myName;
    }
}
