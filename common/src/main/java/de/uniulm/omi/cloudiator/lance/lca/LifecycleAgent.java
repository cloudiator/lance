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

import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.RemoteDockerComponent;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

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

    ComponentInstanceId deployDeployableComponent(DeploymentContext ctx, DeployableComponent component,
        OperatingSystemImpl os, ContainerType containerType) throws RemoteException, LcaException, RegistrationException, ContainerException;

    ComponentInstanceId deployDockerComponent(DeploymentContext ctx, DockerComponent component) throws RemoteException, LcaException, RegistrationException, ContainerException;

    ComponentInstanceId deployRemoteDockerComponent(DeploymentContext ctx, RemoteDockerComponent component) throws RemoteException, LcaException, RegistrationException, ContainerException;

    boolean stopComponentInstance(ComponentInstanceId instanceId, boolean forceRegDel) throws RemoteException, LcaException, ContainerException;

    boolean componentInstanceIsReady(ComponentInstanceId instanceId) throws RemoteException, LcaException, ContainerException;

    void updateDownStreamPorts() throws RemoteException, LcaException, ContainerException;
}
