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

package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.PowershellExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.PropertyVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public interface ContainerLogic {

    //void doCreate(OperatingSystem  o) ;
    void doCreate() throws ContainerException;

    void doInit(LifecycleStore store) throws ContainerException;
    
    void completeInit() throws ContainerException;
    void completeShutDown() throws ContainerException;

    /**
     * 
     * @param forceShutdown if false, the 'stop' command was successful and the container can shut down gracefully. 
     * 				Otherwise, it does its best to terminate the application if possible.
     * @throws ContainerException when shutting down the container was not possible
     */
    void doDestroy(boolean forceShutdown) throws ContainerException;

    // ComponentInstanceId getId();
    /**
     * 
     * @return null if no address is available;
     * otherwise a stringified form of an IP address
     * @throws ContainerException when a container-specific exception occurs
     */

    void preDestroy() throws ContainerException;

    String getLocalAddress() throws ContainerException;

    InportAccessor getPortMapper();

    boolean setStaticEnvironment(boolean useExistingShell);

    boolean setDynamicEnvironment(boolean useExistingshell);
}
