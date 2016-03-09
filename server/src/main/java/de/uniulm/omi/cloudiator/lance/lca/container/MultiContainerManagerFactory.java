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

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;


/**
 * Created by Daniel Seybold on 23.09.2015.
 */
public final class MultiContainerManagerFactory {

    public static ContainerManager createContainerManager(HostContext contex, OperatingSystem operatingSystem, ContainerType containerType){

        switch (operatingSystem.getFamily()){
            case WINDOWS: return createWindowsContainerManager(contex);

            case LINUX: return createLinuxContainerManager(contex, containerType);

            default: throw new IllegalStateException("No matching Operating System found: " + operatingSystem.toString());
        }



    }

    private static ContainerManager createWindowsContainerManager(HostContext hostContext){
        return ContainerManagerFactory.createContainerManager(hostContext, ContainerType.PLAIN);
    }

    private static ContainerManager createLinuxContainerManager(HostContext hostContext, ContainerType containerType){

        switch (containerType){
            case DOCKER: return ContainerManagerFactory.createContainerManager(hostContext, ContainerType.DOCKER);

            case PLAIN:  return ContainerManagerFactory.createContainerManager(hostContext, ContainerType.PLAIN);

            default: throw new IllegalStateException("No matching Container " + containerType.toString() + " for Operating System Linux found!");
        }

    }
}
