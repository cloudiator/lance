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

import java.rmi.RemoteException;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManagerFactory;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerContainerManagerFactory;

public class LifecycleAgentImpl implements LifecycleAgent {

    private final ContainerManager<?> manager;
    
    LifecycleAgentImpl(HostContext contex) {
        DockerContainerManagerFactory.enableRemoteAccess();
        manager = ContainerManagerFactory.createContainerManager(contex, ContainerType.DOCKER_REMOTE);
    }
    
    synchronized void init() {
        //FIXME: anything to do here? //
    }
    
    @Override
    public synchronized void terminate() {
        //FIXME: terminate all instances //
        LifecycleAgentBooter.unregister(this);
        manager.terminate();
    }

    
    @Override
    public synchronized void stop() {
        LifecycleAgentBooter.unregister(this);
    }

    @Override
    public List<ComponentInstanceId> listContainers() throws RemoteException {
        // manager.getAllContainers();
        return null;
    }

    @Override
    public ComponentInstanceId deployComponent(DeploymentContext ctx, DeployableComponent component, OperatingSystem os) {
        ContainerController cc = manager.createNewContainer(ctx, component, os);
        cc.awaitCreation();
        cc.init(component.getLifecycleStore());
        cc.awaitInitialisation();
        return cc.getId();
    }
}
