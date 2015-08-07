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

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManagerFactory;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerContainerManagerFactory;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;

public class LifecycleAgentImpl implements LifecycleAgent {

	private volatile AgentStatus status = AgentStatus.NEW;
    private final ContainerManager manager;
    
    LifecycleAgentImpl(HostContext contex) {
        DockerContainerManagerFactory.enableRemoteAccess();
        manager = ContainerManagerFactory.createContainerManager(contex, ContainerType.DOCKER_REMOTE);
        status = AgentStatus.CREATED;
    }
    
    synchronized void init() {
    	status = AgentStatus.READY;
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
        return manager.getAllContainers();
    }

    @Override
    public ComponentInstanceId deployComponent(DeploymentContext ctx, DeployableComponent component, OperatingSystem os) throws LcaException, RegistrationException, ContainerException {
        applicationRegistered(ctx);
        componentPartOfApplication(ctx, component);
        
        ContainerController cc = manager.createNewContainer(ctx, component, os);
        cc.awaitCreation();
        cc.init(component.getLifecycleStore());
        cc.awaitInitialisation();
        return cc.getId();
    }
    
    private static void applicationRegistered(DeploymentContext ctx) throws LcaException, RegistrationException {
        LcaRegistry reg = ctx.getRegistry();
        ApplicationInstanceId appInstId = ctx.getApplicationInstanceId();
        
        if(reg.applicationInstanceExists(appInstId)) 
            return;
        
        throw new LcaException("cannot proceed: application instance is not known.");
    }
    
    private static void componentPartOfApplication(DeploymentContext ctx, DeployableComponent component) throws RegistrationException, LcaException {
        LcaRegistry reg = ctx.getRegistry();
        ApplicationInstanceId appInstId = ctx.getApplicationInstanceId();
        
        if(reg.applicationComponentExists(appInstId, component.getComponentId())) 
            return;
        
        throw new LcaException("cannot proceed: component is not known within application instance.");
    }

	@Override
	public AgentStatus getAgentStatus() {
		return status;
	}

	@Override
	public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) {
		return manager.getComponentContainerStatus(cid);
	}
}
