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

import java.util.List; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.LcaConstants;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ErrorAwareContainer;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.registry.ContainerRegistry;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.ConnectorFactory;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;

public class DockerContainerManager implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManager.class);
    private final boolean isRemote;
    private final HostContext hostContext;
    private final String hostname;
    private final DockerConnector client;
    private final ContainerRegistry registry = new ContainerRegistry();
    
    public DockerContainerManager(HostContext vmId) {
        this(vmId, LcaConstants.LOCALHOST_IP, false);
        LOGGER.debug("using local host " + LcaConstants.LOCALHOST_IP + " as host name");
    }

    DockerContainerManager(HostContext vmId, String host) {
        this(vmId, host, true);
    }
    
    private DockerContainerManager(HostContext vmId, String host, boolean remote) {
        hostContext = vmId;
        hostname = host;
        client = ConnectorFactory.INSTANCE.createConnector(hostname);
        // translator = createAndInitTranslator();
        isRemote = remote;
    }
    
    @Override
    public ContainerController getContainer(ComponentInstanceId idParam) {
        return registry.getContainer(idParam);
    }

    @Override
    public ContainerType getContainerType() {
        if(isRemote) 
            return ContainerType.DOCKER_REMOTE;
        return ContainerType.DOCKER;
    }

    @Override
    public ContainerController createNewContainer(DeploymentContext ctx, DeployableComponent comp, OperatingSystem os) throws ContainerException {
        ComponentInstanceId id = new ComponentInstanceId();
        DockerShellFactory shellFactory = new DockerShellFactory();
        GlobalRegistryAccessor accessor = new GlobalRegistryAccessor(ctx, comp, id);

        NetworkHandler networkHandler = new NetworkHandler(accessor, comp, hostContext);
        DockerContainerLogic logic = new DockerContainerLogic(id, client, comp, ctx, os, networkHandler, shellFactory);
        // DockerLifecycleInterceptor interceptor = new DockerLifecycleInterceptor(accessor, id, networkHandler, comp, shellFactory);
        ExecutionContext ec = new ExecutionContext(os, shellFactory);
        LifecycleController controller = new LifecycleController(comp.getLifecycleStore(), logic, accessor, ec, hostContext);
        
        try { 
            accessor.init(id); 
        } catch(RegistrationException re) { 
            throw new ContainerException("cannot start container, because registry not available", re); 
        }
        
        ContainerController dc = new ErrorAwareContainer<>(id, logic, networkHandler, controller, accessor);
        registry.addContainer(dc);
        dc.create();
        return dc;
    }

    @Override
    public void terminate() {
        LOGGER.error("terminate has not been fully implemented; not terminating containers.");
        // FIXME: add other parts to shut down //
    }

    @Override
    public List<ComponentInstanceId> getAllContainers() {
        return registry.listComponentInstances();
    }

    @Override
    public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) {
        ContainerController dc = registry.getContainer(cid);
        return dc.getState();
    }
}
