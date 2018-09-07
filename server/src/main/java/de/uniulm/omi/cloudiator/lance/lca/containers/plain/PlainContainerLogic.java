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

package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.StaticEnvVars;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.PowershellExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.PropertyVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
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

/**
 * Created by Daniel Seybold on 10.08.2015.
 */
public class PlainContainerLogic implements ContainerLogic, LifecycleActionInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlainContainerLogic.class);

  private final ComponentInstanceId myId;
  private final DeployableComponent deployableComponent;
  private final DeploymentContext deploymentContext;
  private final OperatingSystem os;
  private final NetworkHandler networkHandler;
  private final PlainShellFactory plainShellFactory;
  private final HostContext hostContext;
  private boolean stopped = false;
  private final StaticEnvVars instVars;
  private final StaticEnvVars hostVars;

  private final Map<String, String> envVarsStatic;
  private final Map<String, String> envVarsDynamic;

  //todo: not needed in post-colosseum version, as the environment-var names should be set correctly then
  private static final Map<String, String> translateMap;
  static {
    Map<String, String> tmpMap = new HashMap<>();
    tmpMap.put("host.vm.id", "VM_ID");
    tmpMap.put("INSTANCE_ID", "INSTANCE_ID");
    translateMap = Collections.unmodifiableMap(tmpMap);
  }

  PlainContainerLogic(ComponentInstanceId id, DeployableComponent deployableComponent,
      DeploymentContext deploymentContext, OperatingSystem os, NetworkHandler networkHandler,
      PlainShellFactory plainShellFactory, HostContext hostContext) {

    this.myId = id;
    this.instVars = this.myId;
    this.deployableComponent = deployableComponent;
    this.deploymentContext = deploymentContext;
    this.os = os;
    this.networkHandler = networkHandler;
    this.plainShellFactory = plainShellFactory;
    this.hostContext = hostContext;
    this.hostVars = this.hostContext;
    this.envVarsStatic = new HashMap<String, String>(instVars.getEnvVars());

    for(Map.Entry<String, String> kv : hostVars.getEnvVars().entrySet()) {
      envVarsStatic.put(kv.getKey(),kv.getValue());
    }

    this.envVarsDynamic = new HashMap<>();
    //todo: fill dynamic map with appropriate env-vars
  }

  @Override
  public void doCreate() throws ContainerException {
    LOGGER.info("Creating shell for operating system: " + this.os.toString());

    PlainShell plainShell = this.plainShellFactory.createAndinstallPlainShell(os);
    LOGGER.debug("Java System user.dir value: " + System.getProperty("user.home"));
    final String plainContainerFolder =
        System.getProperty("user.home") + System.getProperty("file.separator") + this.myId
            .toString();
    LOGGER.info("creating new plain container with foldername " + plainContainerFolder);
    plainShell.executeCommand("mkdir " + plainContainerFolder);

    LOGGER.info("Switching to plain container: " + plainContainerFolder);
    plainShell.setDirectory(plainContainerFolder);

    this.plainShellFactory.closeShell();
  }

  @Override
  public void doInit(LifecycleStore store) throws ContainerException {
    //probably not needed for plain container, except setting the environment
    setStaticEnvironment(false);
  }

  @Override
  public void completeInit() throws ContainerException {
    this.plainShellFactory.closeShell();
  }

  @Override
  public void completeShutDown() throws ContainerException {
    this.plainShellFactory.closeShell();
  }

  @Override
  public void doDestroy(boolean forceShutdown) throws ContainerException {
    //TODO: maybe remember pid of start, then kill this pid or gracefully kill pid.
    LOGGER.warn("doDestroy not fully implemented!");
    setStaticEnvironment(true);
  }

  @Override
  public String getLocalAddress() throws ContainerException {
    if (!stopped) {
      return hostContext.getInternalIp();
    }
    return null;
  }

  /**
   * loops the port numbers through (a,b) --- ((a,a) , (b,b))
   *
   * @return the inport accessor
   */
  @Override
  public InportAccessor getPortMapper() {
    return ((portName, clientState) -> {

      Integer portNumber = (Integer) deploymentContext.getProperty(portName, InPort.class);
      clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_0, portNumber);
      clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_1, portNumber);
      clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_2, portNumber);

    });
  }

  //todo: where to throw exception
  @Override
  public boolean setStaticEnvironment(boolean useExistingShell) {
    if(!useExistingShell)
      this.plainShellFactory.createAndinstallPlainShell(this.os);

    PlainShellWrapper plainShellWrapper = this.plainShellFactory.createShell();

    if (this.os.getFamily().equals(OperatingSystemFamily.WINDOWS)) {
      PowershellExportBasedVisitor visitor =
          new PowershellExportBasedVisitor(plainShellWrapper.plainShell);

      for(Entry<String, String> entry: envVarsStatic.entrySet()) {
        visitor.visit(entry.getKey(), entry.getValue());
      }
    } else if (this.os.getFamily().equals(OperatingSystemFamily.LINUX)) {
      BashExportBasedVisitor visitor = new BashExportBasedVisitor(plainShellWrapper.plainShell);

      for(Entry<String, String> entry: envVarsStatic.entrySet()) {

        //todo: not needed in post-colosseum version, as the environment-var names should be set correctly then
        if(!translateMap.containsKey(entry.getKey()))
          continue;

        visitor.visit(translateMap.get(entry.getKey()), entry.getValue());
      }
    }
    return true;
  }

  @Override
  public boolean setDynamicEnvironment(boolean useExistingshell) {
    //todo: implement
    return true;
  }

  @Override
  public void prepare(HandlerType type) throws ContainerException {

    if(!setStaticEnvironment(false)) {
      throw new ContainerException("cannot create shell for " + type + " in prepare method ");
    }

    if (type == LifecycleHandlerType.INSTALL) {
      preInstallAction();
    }
    if (type == LifecycleHandlerType.PRE_STOP) {
      stopped = true;
    }

  }

  private void preInstallAction() {
    PlainShellWrapper plainShellWrapper = this.plainShellFactory.createShell();

    //TODO: move os switch to a central point (currently here and in PlainShellImpl)
    if (this.os.getFamily().equals(OperatingSystemFamily.WINDOWS)) {

      PowershellExportBasedVisitor visitor =
          new PowershellExportBasedVisitor(plainShellWrapper.plainShell);
      networkHandler.accept(visitor, null);
      this.deployableComponent.accept(this.deploymentContext, visitor);
    } else if (this.os.getFamily().equals(OperatingSystemFamily.LINUX)) {
      BashExportBasedVisitor visitor =
          new BashExportBasedVisitor(plainShellWrapper.plainShell);

      //visitor.addEnvironmentVariable();
      networkHandler.accept(visitor, null);
      this.deployableComponent.accept(this.deploymentContext, visitor);
    } else {
      throw new RuntimeException("Unsupported Operating System: " + this.os.toString());
    }
  }

  @Override
  public void postprocess(HandlerType type) {
    if (type == LifecycleHandlerType.PRE_INSTALL) {
      postPreInstall();
    } else if (type == LifecycleHandlerType.POST_INSTALL) {
      // TODO: how should we snapshot the folder? //
    }
    plainShellFactory.closeShell();
  }

  private void postPreInstall() {
    // TODO: empty method?
  }

  @Override
  public ComponentInstanceId getComponentInstanceId() {
    return this.myId;
  }

  @Override
  public void postprocessPortUpdate(PortDiff<DownstreamAddress> diff) {
    plainShellFactory.closeShell();
  }

  @Override
  public void preprocessPortUpdate(PortDiff<DownstreamAddress> diff)
      throws ContainerException {

    //TODO: again duplicated code, needs refactoring
    plainShellFactory.createAndinstallPlainShell(this.os);
    PlainShellWrapper plainShellWrapper = this.plainShellFactory.createShell();

    if (this.os.getFamily().equals(OperatingSystemFamily.WINDOWS)) {

      PowershellExportBasedVisitor visitor =
          new PowershellExportBasedVisitor(plainShellWrapper.plainShell);
      networkHandler.accept(visitor, diff);
      this.deployableComponent.accept(this.deploymentContext, visitor);
      setStaticEnvironment(true);
    } else if (this.os.getFamily().equals(OperatingSystemFamily.LINUX)) {
      BashExportBasedVisitor visitor =
          new BashExportBasedVisitor(plainShellWrapper.plainShell);

      networkHandler.accept(visitor, diff);
      this.deployableComponent.accept(this.deploymentContext, visitor);
      setStaticEnvironment(true);
    } else {
      throw new RuntimeException("Unsupported Operating System: " + this.os.toString());
    }
  }

  @Override
  public void preDestroy() throws ContainerException {
    setStaticEnvironment(false);
  }

  @Override
  public void postprocessDetector(DetectorType type) {
    LOGGER.error("postprocessDetector is not implemented for plain container");
  }

  @Override
  public void preprocessDetector(DetectorType type) throws ContainerException {
    LOGGER.error("preprocessDetector is not implemented for plain container");
  }
}
