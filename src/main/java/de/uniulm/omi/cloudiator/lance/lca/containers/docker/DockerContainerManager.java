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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.StandardContainer;
import de.uniulm.omi.cloudiator.lance.deployment.self.DockerDeployment;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.container.registry.ContainerRegistry;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.ConnectorFactory;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandSequence;

public class DockerContainerManager implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManager.class);
    private final boolean isRemote;
    private final HostContext hostContext;
    private final String hostname;
    private final DockerConnector client;
    // private final DockerOperatingSystemTranslator translator;
    private final ContainerRegistry registry = new ContainerRegistry();
    
    public DockerContainerManager(HostContext vmId) {
        this(vmId, "127.0.0.1", false);
        LOGGER.debug("using local host (127.0.0.1) as host name");
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
    public void runApplication(ComponentInstanceId idParam, LifecycleStore store) {
        ContainerController c = getContainer(idParam);
        c.init(store);
    }

    @Override
    public CommandSequence addDeploymentSequence() {
        return DockerDeployment.create();
    }

    @Override
    public ContainerType getContainerType() {
        if(isRemote) 
        	return ContainerType.DOCKER_REMOTE;
        return ContainerType.DOCKER;
    }

    @Override
    public ContainerController createNewContainer(DeploymentContext ctx, DeployableComponent comp, OperatingSystem os) {
        ComponentInstanceId id = new ComponentInstanceId();
        GlobalRegistryAccessor accessor = new GlobalRegistryAccessor(ctx, comp, id);
        PortRegistryTranslator portAccessor = new PortRegistryTranslator(accessor, hostContext);
        NetworkHandler handler = new NetworkHandler(portAccessor, DockerContainerManagerFactory.DOCKER_PORT_HIERARCHY, comp);
        
        
        DockerContainerLogic l = new DockerContainerLogic(id, client, accessor, comp, ctx, os, handler);
        ContainerController dc = new StandardContainer<>(id, l);
        registry.addContainer(dc);
        dc.create();
        return dc;
    }

    @Override
    public void terminate() {
        try { 
        	hostContext.close(); 
        } catch(InterruptedException ie) {
            LOGGER.warn("shutting down interrupted");
        }
        LOGGER.error("terminate has not been fully implemented");
        // FIXME: add other parts to shut down //
    }
}
