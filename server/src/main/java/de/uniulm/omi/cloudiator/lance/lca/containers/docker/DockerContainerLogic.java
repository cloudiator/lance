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

import static de.uniulm.omi.cloudiator.lance.application.component.ComponentType.DOCKER;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentType;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.HandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleActionInterceptor;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniulm.omi.cloudiator.lance.application.component.ComponentType.DOCKER;

public class DockerContainerLogic implements ContainerLogic, LifecycleActionInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerManager.class);
        
    private final ComponentInstanceId myId;
    private final DockerConnector client;
    
    private final DockerShellFactory shellFactory;
    private final DeploymentContext deploymentContext;
    
    private final DockerImageHandler imageHandler;
    private final NetworkHandler networkHandler;
    
    private final DeployableComponent myComponent;
    private final HostContext hostContext;

  DockerContainerLogic(ComponentInstanceId id, DockerConnector client, DeployableComponent comp,
      DeploymentContext ctx, OperatingSystem os, NetworkHandler network,
      DockerShellFactory shellFactoryParam, DockerConfiguration dockerConfig,
      HostContext hostContext) {
    this(id, client, os, ctx, comp, network, shellFactoryParam, dockerConfig, hostContext);
  }

  private DockerContainerLogic(ComponentInstanceId id, DockerConnector clientParam,
      OperatingSystem osParam,
      DeploymentContext ctx, DeployableComponent componentParam,
      NetworkHandler networkParam, DockerShellFactory shellFactoryParam,
      DockerConfiguration dockerConfigParam, HostContext hostContext) {

    if (osParam == null) {
      throw new NullPointerException("operating system has to be set.");
    }

    myId = id;
    client = clientParam;
    imageHandler = new DockerImageHandler(osParam, new DockerOperatingSystemTranslator(),
        clientParam, componentParam, dockerConfigParam);
    deploymentContext = ctx;
    shellFactory = shellFactoryParam;
    myComponent = componentParam;

    networkHandler = networkParam;
    this.hostContext = hostContext;
  }

	@Override
	public ComponentInstanceId getComponentInstanceId() {
		return myId;
	}
        
    @Override
    public synchronized void doCreate() throws ContainerException {
        try {
            ComponentType type = myComponent.getType();
            if (type == DOCKER) {
                String imageName = myComponent.getName();
                executeCreation(imageName);
            }
            else
                executeCreation();
        } catch(DockerException de) {
            throw new ContainerException("docker problems. cannot create container " + myId, de);
        }
    }

    @Override
    public void preDestroy() {
        //todo: mb implement
        //shellFactory.installDockerShell(shell);
    }

    @Override
    public InportAccessor getPortMapper() {
        return ( (portName, clientState) -> {
            try {
                Integer portNumber = (Integer) deploymentContext.getProperty(portName, InPort.class);
                int mapped = client.getPortMapping(myId, portNumber);
                Integer i = Integer.valueOf(mapped);
                clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_0, i);
                clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_1, i);
                clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_2, portNumber);
            } catch(DockerException de) {
                throw new ContainerException("coulnd not register all port mappings", de);
            }
        });
    }

    @Override
    public String getLocalAddress() {
        try {
            return client.getContainerIp(myId);
        } catch(DockerException de) {
            // this means that that the container is not
            // up and running; hence, no IP address is
            // available. it is up to the caller to figure
            // out the semantics of this state 
        }
        return null;
    }
    
	@Override
	public void completeInit() throws ContainerException {
		shellFactory.closeShell();
	}

    @Override
    public void completeShutDown() throws ContainerException {
        //todo: mb implement
        //shellFactory.closeShell();
    }

  @Override
  public void prepare(HandlerType type) throws ContainerException {
    if (type == LifecycleHandlerType.INSTALL) {
      preInstallAction();
    }
  }

  @Override
  public void doInit(LifecycleStore store) throws ContainerException {
    try {
      DockerShell shell = doStartContainer();
      shellFactory.installDockerShell(shell);
    } catch (ContainerException ce) {
      throw ce;
    } catch (Exception ex) {
      throw new ContainerException(ex);
    }
  }

  @Override
  public void doDestroy(boolean force) throws ContainerException {
    /* currently docker ignores the flag */
    try {
      client.stopContainer(myId);
    } catch (DockerException de) {
      throw new ContainerException(de);
    }
  }

  @Override
  public void preprocessPortUpdate(PortDiff<DownstreamAddress> diffSet) throws ContainerException {
    try {
      DockerShell shell = client.getSideShell(myId);
      prepareEnvironment(shell, diffSet);
      shellFactory.installDockerShell(shell);
    } catch (DockerException de) {
      throw new ContainerException("cannot create shell for port updates.", de);
    }
  }

  @Override
  public void postprocessPortUpdate(PortDiff<DownstreamAddress> diffSet) {
    shellFactory.closeShell();
  }

  @Override
  public void postprocess(HandlerType type) {
    if (type == LifecycleHandlerType.PRE_INSTALL) {
      postPreInstall();
    } else if (type == LifecycleHandlerType.POST_INSTALL) {
      // TODO: do we have to make a snapshot after this? //
    }
  }

  @Override
  public void preprocessDetector(DetectorType type) throws ContainerException {
    // nothing special to do; just create a shell and prepare an environment //
    try {
      DockerShell shell = client.getSideShell(myId);
      prepareEnvironment(shell);
      shellFactory.installDockerShell(shell);
    } catch (DockerException de) {
      throw new ContainerException("cannot create shell for " + type + " detector.", de);
    }
  }

  @Override
  public void postprocessDetector(DetectorType type) {
    // nothing special to do; just create a shell //
    shellFactory.closeShell();
  }

  private DockerShell doStartContainer() throws ContainerException {
    final DockerShell dshell;
    try {
      dshell = client.startContainer(myId);
    } catch (DockerException de) {
      throw new ContainerException("cannot start container: " + myId, de);
    }
    shellFactory.installDockerShell(dshell);
    return dshell;
  }

  private void preInstallAction() {
    DockerShellWrapper w = shellFactory.createShell();
    prepareEnvironment(w.shell);
  }

  private void postPreInstall() {
    try {
      imageHandler.runPostInstallAction(myId);
    } catch (DockerException de) {
      LOGGER.warn("could not update finalise image handling.", de);
    }
  }

  private void prepareEnvironment(DockerShell dshell) {
    prepareEnvironment(dshell, null);
  }

  private void prepareEnvironment(DockerShell dshell, PortDiff<DownstreamAddress> diff) {
    BashExportBasedVisitor visitor = new BashExportBasedVisitor(dshell);
    visitor.visit("TERM", "dumb");
    visitor.visit("VM_ID_KEY", hostContext.getVMIdentifier());
    visitor.visit("INSTANCE_ID", myId.toString());

    networkHandler.accept(visitor, diff);
    myComponent.accept(deploymentContext, visitor);
  }

  private void executeCreation() throws DockerException {
    String target = imageHandler.doPullImages(myId);
    Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
    //@SuppressWarnings("unused") String dockerId =
    client.createContainer(target, myId, portsToSet);
  }

  private void executeCreation(String imageName) throws DockerException {
    String target = imageHandler.doPullImages(myId, imageName);
    Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
    //@SuppressWarnings("unused") String dockerId =
    client.createContainer(target, myId, portsToSet);
  }
}
