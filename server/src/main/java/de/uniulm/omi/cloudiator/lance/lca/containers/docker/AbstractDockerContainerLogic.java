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

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.StaticEnvVars;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//install shell in doInit (todo: rename to doBootstrap as it is called in BootstrapTransitionAction), close shell in preInit
//install shell before every LifeCycleTransition, close shell after eacgy LifeCycleTransition
//install shell in preDestroy, close shell in completeShutDown
//install shell in preprocessDetector, close shell in postProcessDetector
//install shell in preprocessPortUpdate, close shell in postprocessPortUpdate
abstract class AbstractDockerContainerLogic implements ContainerLogic, LifecycleActionInterceptor {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerManager.class);
        
  protected final ComponentInstanceId myId;
  protected final DockerConnector client;

  protected final DockerShellFactory shellFactory;
  protected final DeploymentContext deploymentContext;

  protected final NetworkHandler networkHandler;

  protected final HostContext hostContext;

  protected final Map<String, String> envVarsStatic;

  //todo: not needed in post-colosseum version, as the environment-var names should be set correctly then
  protected static final Map<String, String> translateMap;
  static {
    Map<String, String> tmpMap = new HashMap<>();
    tmpMap.put("host.vm.id", "VM_ID");
    tmpMap.put("INSTANCE_ID", "INSTANCE_ID");
    translateMap = Collections.unmodifiableMap(tmpMap);
  }

  protected AbstractDockerContainerLogic(AbstractBuilder builder) {

    if (builder.osParam == null) {
      throw new NullPointerException("operating system has to be set.");
    }

    this.myId = builder.myId;
    final StaticEnvVars instVars = this.myId;
    this.client = builder.client;
    this.deploymentContext = builder.deploymentContext;
    this.shellFactory = builder.shellFactory;
    this.networkHandler = builder.networkHandler;
    this.hostContext = builder.hostContext;
    final StaticEnvVars hostVars = this.hostContext;
    this.envVarsStatic = new HashMap<String, String>(instVars.getEnvVars());

    for(Map.Entry<String, String> kv : hostVars.getEnvVars().entrySet()) {
      envVarsStatic.put(kv.getKey(),kv.getValue());
    }
  }

	@Override
	public ComponentInstanceId getComponentInstanceId() {
		return myId;
	}
        
  @Override
  public abstract void doCreate() throws ContainerException;

  @Override
  public void preDestroy() throws ContainerException{
    setStaticEnvironment();
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
  public void setStaticEnvironment() throws ContainerException {
    DockerShell shell = getShell();
    BashExportBasedVisitor visitor = new BashExportBasedVisitor(shell);
    doVisit(shell,visitor);
  }

  protected BashExportBasedVisitor setupCompleteStaticEnvironment(PortDiff<DownstreamAddress> diff) throws ContainerException {
    DockerShell shell = getShell();
    BashExportBasedVisitor visitor = new BashExportBasedVisitor(shell);
    doVisit(shell,visitor);
    networkHandler.accept(visitor, diff);

    return visitor;
  }

  abstract void setCompleteStaticEnvironment(PortDiff<DownstreamAddress> diff) throws ContainerException;

  private void doVisit(DockerShell shell, BashExportBasedVisitor visitor) {
    visitor.visit("TERM", "DUMB");

    for(Entry<String, String> entry: envVarsStatic.entrySet()) {
      //todo: not needed in post-colosseum version, as the environment-var names should be set correctly then
      if(!translateMap.containsKey(entry.getKey()))
        continue;

      visitor.visit(translateMap.get(entry.getKey()), entry.getValue());
    }
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

  //close shell, before opening them again in prepare(...) via LifeCycle-Actions
	@Override
	public void preInit() throws ContainerException {
    closeShell();
	}

  @Override
  public void completeShutDown() throws ContainerException {
    closeShell();
  }

  @Override
  public void prepare(HandlerType type) throws ContainerException {
    setStaticEnvironment();
    if (type == LifecycleHandlerType.INSTALL) {
      preInstallAction();
    }
  }

  @Override
  public abstract void doInit(LifecycleStore store) throws ContainerException;

  @Override
  public abstract void doDestroy(boolean force) throws ContainerException;

  @Override
  public void preprocessPortUpdate(PortDiff<DownstreamAddress> diffSet) throws ContainerException {
    prepareEnvironment(diffSet);
  }

  @Override
  public void postprocessPortUpdate(PortDiff<DownstreamAddress> diffSet) {
    closeShell();
  }

  @Override
  public void postprocess(HandlerType type) throws ContainerException {
    if (type == LifecycleHandlerType.PRE_INSTALL) {
      postPreInstall();
    } else if (type == LifecycleHandlerType.POST_INSTALL) {
      // TODO: do we have to make a snapshot after this? //
    }
    closeShell();
  }

  @Override
  public void preprocessDetector(DetectorType type) throws ContainerException {
    // nothing special to do; just create a shell and prepare an environment //
    prepareEnvironment();
  }

  @Override
  public void postprocessDetector(DetectorType type) {
    // nothing special to do; just create a shell //
    closeShell();
  }

  protected void doStartContainer() throws ContainerException {
    final DockerShell dshell;
    try {
      dshell = client.startContainer(myId);
      setStaticEnvironment();
    } catch (DockerException de) {
      throw new ContainerException("cannot start container: " + myId, de);
    }
  }

  private void preInstallAction() throws ContainerException {
    prepareEnvironment();
  }

  abstract void postPreInstall();

  private void prepareEnvironment() throws ContainerException {
    prepareEnvironment(null);
  }

  private void prepareEnvironment(PortDiff<DownstreamAddress> diff) throws ContainerException {
    setCompleteStaticEnvironment(diff);
  }

  protected void executeCreation(String target) throws DockerException {
    Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
    //@SuppressWarnings("unused") String dockerId =
    client.createContainer(target, myId, portsToSet);
  }

  //used for setting the environment
  private DockerShell getShell() throws ContainerException {
    DockerShell shell;

    try {
      shell = client.getSideShell(myId);
      shellFactory.installDockerShell(shell);
    } catch (DockerException de) {
      throw new ContainerException("Cannot get side shell");
    }

    return shell;
  }

  private void closeShell() {
    shellFactory.closeShell();
  }


  abstract static class AbstractBuilder<T extends DeployableComponent> {
    protected ComponentInstanceId myId;
    protected DockerConnector client;

    protected OperatingSystem osParam;
    protected DockerShellFactory shellFactory;

    protected T myComponent;
    protected DeploymentContext deploymentContext;

    protected NetworkHandler networkHandler;
    protected DockerConfiguration dockerConfig;

    protected HostContext hostContext;

    public AbstractBuilder(){}

    public AbstractBuilder cInstId(ComponentInstanceId myId) {
      this.myId = myId;
      return this;
    }

    public AbstractBuilder dockerConnector(DockerConnector connector) {
      this.client = connector;
      return this;
    }

    public AbstractBuilder osParam(OperatingSystem os) {
      this.osParam = os;
      return this;
    }

    public AbstractBuilder dockerShellFac(DockerShellFactory shellFactory) {
      this.shellFactory = shellFactory;
      return this;
    }

    public AbstractBuilder<T> deplComp(T comp) {
      this.myComponent = comp;
      return this;
    }

    public AbstractBuilder deplContext(DeploymentContext dContext) {
      this.deploymentContext = dContext;
      return this;
    }

    public AbstractBuilder nwHandler(NetworkHandler nwHandler) {
      this.networkHandler = nwHandler;
      return this;
    }

    public AbstractBuilder dockerConfig(DockerConfiguration config) {
      this.dockerConfig = config;
      return this;
    }

    public AbstractBuilder hostContext(HostContext hostContext) {
      this.hostContext = hostContext;
      return this;
    }

    abstract AbstractDockerContainerLogic build();
  }
}
