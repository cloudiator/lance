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

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystemFamily;


public enum ContainerType {
    
    DOCKER("dockering") {

		@Override
		public boolean supportsOsFamily(OperatingSystemFamily family) {
			switch(family){
				case LINUX:
					return true;
				case BSD:
				case WINDOWS:
				case OTHER:
					return false;
				default: 
					throw new IllegalArgumentException("OS type: " + family + " is not known");
			}
		}
    },
    
    DOCKER_REMOTE("docker-remote") {

		@Override
		public boolean supportsOsFamily(OperatingSystemFamily family) {
			return DOCKER.supportsOsFamily(family);
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
		public boolean supportsOsFamily(OperatingSystemFamily family) {
			switch(family){
				case LINUX:
				case WINDOWS:
					return true;
				case BSD:
				case OTHER:
					return false;
				default: 
					throw new IllegalArgumentException("OS type: " + family + " is not known");
			}
		}
    }
    ;
    
    private final String myName;
    
    private ContainerType(String myNameParam) {
        myName = myNameParam;
    }

    /**
     * @param containername the name of the containers
     * @return returns the ContainerType matching the parameter. Null otherwise.
     */
    public static ContainerType fromString(String containername) {
        for(ContainerType t : ContainerType.values()) {
            if(t.myName.equalsIgnoreCase(containername)) {
                return t;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return myName;
    }

	public abstract boolean supportsOsFamily(OperatingSystemFamily family);
}
