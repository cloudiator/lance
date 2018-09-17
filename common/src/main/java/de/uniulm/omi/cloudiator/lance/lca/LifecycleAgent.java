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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;

public interface LifecycleAgent extends Remote {

    AgentStatus getAgentStatus() throws RemoteException;
    
    ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) throws RemoteException;
    
    void stop() throws RemoteException;

    void terminate() throws RemoteException;

    List<ComponentInstanceId> listContainers() throws RemoteException;

    ComponentInstanceId deployComponent(DeploymentContext ctx, DeployableComponent component, 
                OperatingSystem os, ContainerType containerType) throws RemoteException, LcaException, RegistrationException, ContainerException;
    
    boolean stopComponentInstance(ContainerType containerType, ComponentInstanceId instanceId) throws RemoteException, LcaException, ContainerException;

    public String getHostEnv() throws RemoteException;
}
