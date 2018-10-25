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

import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.application.component.LifecycleComponent;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStoreBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.DefaultHandlers;
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

//used by both deploy(Lifecycle)Component with ContainerType==DOCKER and deployDockerComponent
public class DockerContainerManager implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManager.class);
    private final boolean isRemote;
    private final HostContext hostContext;
    private final String hostname;
    private final DockerConnector client;
    private final ContainerRegistry registry = new ContainerRegistry();
    private final DockerConfiguration dockerConfig = DockerConfiguration.INSTANCE; 
    
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

    //used by deploy(Lifecycle)Component with ContainerType==DOCKER
    //todo: Replace calls to this method in time when DeployableComponent isn't needed anymore
    @Override
    public ContainerController createNewContainer(DeploymentContext ctx,
        DeployableComponent comp, OperatingSystem os) throws ContainerException {

      final boolean forceRegDel = false;
      ContainerComponents cComponents = new ContainerComponents(comp, hostContext, client, ctx, dockerConfig, os);
      accessorInit(cComponents.accessor, cComponents.id);
        ContainerController dc = new ErrorAwareContainer<>(cComponents.id, cComponents.logic, cComponents.networkHandler, cComponents.controller, cComponents.accessor, forceRegDel);
        registry.addContainer(dc);
        dc.create();
        return dc;
    }

    @Override
    public ContainerController createNewLifecycleContainer(DeploymentContext ctx,
        LifecycleComponent comp, OperatingSystem os) throws ContainerException {

      //todo: implement this also for LifecycleContainers!?
      final boolean forceRegDel = false;
      LifecycleContainerComponents cComponents = new LifecycleContainerComponents(comp, hostContext, client, ctx, dockerConfig, os);
      accessorInit(cComponents.accessor, cComponents.id);
      ContainerController dc = new ErrorAwareContainer<>(cComponents.id, cComponents.logic, cComponents.networkHandler, cComponents.controller, cComponents.accessor, forceRegDel);
      registry.addContainer(dc);
      dc.create();
      return dc;
    }

    //used by deployDockerComponent
    public ContainerController createNewDockerContainer(DeploymentContext ctx,
        DockerComponent comp, boolean forceRegDel) throws ContainerException {

      DockerContainerComponents cComponents = new DockerContainerComponents(comp, hostContext, client, ctx, dockerConfig);
      accessorInit(cComponents.accessor, cComponents.id);
      ContainerController dc = new ErrorAwareContainer<>(cComponents.id, cComponents.logic, cComponents.networkHandler, cComponents.controller, cComponents.accessor, forceRegDel);
      registry.addContainer(dc);
      dc.create();
      return dc;
    }

    private static void accessorInit(GlobalRegistryAccessor acc, ComponentInstanceId cId) throws ContainerException {
      try {
        acc.init(cId);
      } catch(RegistrationException re) {
        throw new ContainerException("cannot start container, because registry not available", re);
      }
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

  private static class DefaultContainerComponents {
    protected final ComponentInstanceId id;
    protected final DockerShellFactory shellFactory;

    DefaultContainerComponents() {
      this.id = new ComponentInstanceId();
      this. shellFactory = new DockerShellFactory();

    }
  }

  //todo: keep this as long as legacy DeployableComponent exists
  private static class ContainerComponents extends DefaultContainerComponents {
    private final NetworkHandler networkHandler;
    private final LifecycleController controller;
    private final GlobalRegistryAccessor accessor;
    private final AbstractDockerContainerLogic logic;

    ContainerComponents(DeployableComponent comp, HostContext hostContext, DockerConnector client, DeploymentContext ctx, DockerConfiguration dockerConfig, OperatingSystem os) {
      super();

      this.accessor = new GlobalRegistryAccessor(ctx, comp, id);
      this.networkHandler = new NetworkHandler(accessor, comp, hostContext);
      LegacyDockerContainerLogic.Builder builder = new LegacyDockerContainerLogic.Builder();
      this.logic = builder.cInstId(id).dockerConnector(client).deplComp(comp).deplContext(ctx).osParam(os).
          nwHandler(networkHandler).dockerShellFac(shellFactory).dockerConfig(dockerConfig).hostContext(hostContext).build();

      ExecutionContext ec = new ExecutionContext(os, shellFactory);
      this.controller = new LifecycleController(comp.getLifecycleStore(), logic, accessor, ec, hostContext);
    }
  }


  private static class LifecycleContainerComponents extends DefaultContainerComponents {
    private final NetworkHandler networkHandler;
    private final LifecycleController controller;
    private final GlobalRegistryAccessor accessor;
    private final AbstractDockerContainerLogic logic;

    LifecycleContainerComponents(LifecycleComponent comp, HostContext hostContext, DockerConnector client, DeploymentContext ctx, DockerConfiguration dockerConfig, OperatingSystem os) {
      super();

      this.accessor = new GlobalRegistryAccessor(ctx, comp, id);
      this.networkHandler = new NetworkHandler(accessor, comp, hostContext);
      LifecycleDockerContainerLogic.Builder builder = new LifecycleDockerContainerLogic.Builder();
      this.logic = builder.cInstId(id).dockerConnector(client).deplComp(comp).deplContext(ctx).osParam(os).
          nwHandler(networkHandler).dockerShellFac(shellFactory).dockerConfig(dockerConfig).hostContext(hostContext).build();

      ExecutionContext ec = new ExecutionContext(os, shellFactory);
      this.controller = new LifecycleController(comp.getLifecycleStore(), logic, accessor, ec, hostContext);
    }
  }

  private static class DockerContainerComponents extends DefaultContainerComponents {
    private final NetworkHandler networkHandler;
    //todo: kick LCC member-var
    private final LifecycleController controller;
    private final GlobalRegistryAccessor accessor;
    private final AbstractDockerContainerLogic logic;

    DockerContainerComponents(DockerComponent comp, HostContext hostContext, DockerConnector client, DeploymentContext ctx, DockerConfiguration dockerConfig) {
      super();

      this.accessor = new GlobalRegistryAccessor(ctx, comp, id);
      this.networkHandler = new NetworkHandler(accessor, comp, hostContext);
      DockerContainerLogic.Builder builder = new DockerContainerLogic.Builder();
      //todo: kick os parameter
      OperatingSystem os = OperatingSystem.UBUNTU_14_04;
      this.logic = builder.cInstId(id).dockerConnector(client).deplComp(comp).deplContext(ctx).osParam(os).
          nwHandler(networkHandler).dockerShellFac(shellFactory).dockerConfig(dockerConfig).hostContext(hostContext).build();
      //todo: kick LCS parameter
      LifecycleStoreBuilder lsBuilder = new LifecycleStoreBuilder();
      lsBuilder.setStartDetector(DefaultHandlers.DEFAULT_START_DETECTOR);
      LifecycleStore store = lsBuilder.build();
      //todo: kick ExecutionContext parameter
      ExecutionContext ec = new ExecutionContext(os, shellFactory);
      //todo: kick LCC member-var
      this.controller =
          new LifecycleController(lsBuilder.build(), this.logic, this.accessor,
              ec, hostContext);
    }
  }
}
