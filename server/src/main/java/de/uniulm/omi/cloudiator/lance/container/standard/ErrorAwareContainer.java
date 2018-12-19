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

package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerConfigurationException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerOperationException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.UnexpectedContainerStateException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareStateMachine;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareStateMachineBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: move status updates to network handler to this class instead of keeping
// them in the individual container classes

// FIXME: update component instance status each time a state has been reached
public final class ErrorAwareContainer<T extends ContainerLogic> implements ContainerController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorAwareContainer.class);
  final T logic;
  final ComponentInstanceId containerId;
  final NetworkHandler network;
  final LifecycleController controller;
  private final ErrorAwareStateMachine<ContainerStatus> stateMachine;
  private final GlobalRegistryAccessor accessor;
  private boolean shouldBeRemovedParam;
  private boolean isReady;

  public ErrorAwareContainer(
      ComponentInstanceId id,
      T logicParam,
      NetworkHandler networkParam,
      LifecycleController controllerParam,
      GlobalRegistryAccessor accessorParam,
      boolean shouldBeRemovedParam) {
    containerId = id;
    logic = logicParam;
    network = networkParam;
    controller = controllerParam;
    accessor = accessorParam;
    stateMachine = buildUpStateMachine();
    this.shouldBeRemovedParam = shouldBeRemovedParam;
    isReady = false;
  }

  static Logger getLogger() {
    return LOGGER;
  }

  private ErrorAwareStateMachine<ContainerStatus> buildUpStateMachine() {
    ErrorAwareStateMachineBuilder<ContainerStatus> builder =
        new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);

    CreateTransitionAction.create(builder.getTransitionBuilder(), this);
    BootstrapTransitionAction.create(builder.getTransitionBuilder(), this);
    InitTransitionAction.create(builder.getTransitionBuilder(), this);
    DestroyTransitionAction.create(builder.getTransitionBuilder(), this);

    return builder.build();
  }

  @Override
  public ComponentInstanceId getId() {
    return containerId;
  }

  @Override
  public ContainerStatus getState() {
    return stateMachine.getState();
  }

  @Override
  public boolean shouldBeRemoved() {
    return shouldBeRemovedParam;
  }

  @Override
  public void setShouldBeRemoved(boolean shouldBeRemoved) {
    this.shouldBeRemovedParam = shouldBeRemoved;
  }

  @Override
  public void create() {
    stateMachine.transit(ContainerStatus.NEW, ContainerStatus.CREATED, new Object[] {});
  }

  @Override
  public void awaitCreation()
      throws ContainerOperationException, ContainerConfigurationException,
          UnexpectedContainerStateException {
    ContainerStatus stat = stateMachine.waitForEndOfCurrentTransition();
    if (CreateTransitionAction.isSuccessfullEndState(stat)) {
      return;
    }
    if (CreateTransitionAction.isKnownErrorState(stat)) {
      throw new ContainerOperationException(
          "container creation failed. container is now in error state: " + stat,
          stateMachine.collectExceptions());
    }
    throwExceptionIfGenericErrorStateOrOtherState(stat);
  }

  @Override
  public void bootstrap() {
    stateMachine.transit(ContainerStatus.CREATED, ContainerStatus.BOOTSTRAPPED, new Object[] {});
  }

  @Override
  public void awaitBootstrap()
      throws ContainerOperationException, ContainerConfigurationException,
          UnexpectedContainerStateException {
    ContainerStatus stat = stateMachine.waitForEndOfCurrentTransition();
    if (BootstrapTransitionAction.isSuccessfullEndState(stat)) {
      return;
    }
    if (BootstrapTransitionAction.isKnownErrorState(stat)) {
      throw new ContainerOperationException(
          "container bootstrap failed. container is now in error state: " + stat,
          stateMachine.collectExceptions());
    }
    throwExceptionIfGenericErrorStateOrOtherState(stat);
  }

  @Override
  public boolean isReady() {
    return isReady;
  }

  @Override
  public void init(LifecycleStore store) {
    stateMachine.transit(ContainerStatus.BOOTSTRAPPED, ContainerStatus.READY, new Object[] {store});
  }

  @Override
  public void init() {
    stateMachine.transit(ContainerStatus.BOOTSTRAPPED, ContainerStatus.READY, null);
  }

  @Override
  public void awaitInitialisation()
      throws ContainerOperationException, ContainerConfigurationException,
          UnexpectedContainerStateException {
    ContainerStatus stat = stateMachine.waitForEndOfCurrentTransition();
    if (InitTransitionAction.isSuccessfullEndState(stat)) {
      return;
    }
    if (InitTransitionAction.isKnownErrorState(stat)) {
      throw new ContainerOperationException(
          "container initialisation failed. container is now in error state: " + stat,
          stateMachine.collectExceptions());
    }
    throwExceptionIfGenericErrorStateOrOtherState(stat);
  }

  @Override
  public void tearDown() {
    stateMachine.transit(ContainerStatus.READY, ContainerStatus.DESTROYED, new Object[] {});
  }

  @Override
  public void awaitDestruction(boolean forceRegDeletion)
      throws ContainerOperationException, ContainerConfigurationException,
          UnexpectedContainerStateException {
    ContainerStatus stat = stateMachine.waitForEndOfCurrentTransition();
    if (DestroyTransitionAction.isSuccessfullEndState(stat)) {
      if (forceRegDeletion) {
        deleteInRegistry();
      }
      return;
    }
    if (DestroyTransitionAction.isKnownErrorState(stat)) {
      throw new ContainerOperationException(
          "container deletion failed. container is now in error state: " + stat,
          stateMachine.collectExceptions());
    }
    throwExceptionIfGenericErrorStateOrOtherState(stat);
  }

  @Override
  public void startPortUpdaters() {
    network.startPortUpdaters(controller);
  }

  private void deleteInRegistry() throws ContainerConfigurationException {
    try {
      accessor.deleteComponentInstance();
    } catch (RegistrationException e) {
      throw new ContainerConfigurationException(
          "Cannot delete container " + containerId + "out of registry");
    }
  }

  void setNetworking() throws ContainerException {
    String address = logic.getLocalAddress();
    try {
      network.initPorts(address);
    } catch (RegistrationException re) {
      throw new ContainerException("cannot access registry.", re);
    }
  }

  void preCreateAction() throws ContainerException {
    setNetworking();
  }

  void postCreateAction() throws ContainerException {
    // add dummy values so that other components are aware of this instance,
    // but can see that it is not ready for use yet.
    network.publishLocalData(containerId);
  }

  void postBootstrapAction() throws ContainerException {
    String address = logic.getLocalAddress();
    if (address == null)
      throw new ContainerException("container has no IP address set after bootstrapping.");
    network.updateAddress(PortRegistryTranslator.PORT_HIERARCHY_2, address);
    network.iterateOverInPorts(logic.getPortMapper());
    network.pollForNeededConnections();
    // dependencies fullfilled
    isReady = true;
  }

  void preInitAction() throws LifecycleException {
    if (controller == null) {
      LOGGER.info("Skipping Lifecycle Actions for component %s", containerId);
      return;
    }

    controller.blockingInit();
    controller.blockingInstall();
    controller.blockingConfigure();
    controller.blockingStart();
  }

  void postInitAction() throws ContainerException {
    // only now that we have started, can the ports be
    // retrieved and then registered at the registry //
    network.publishLocalData(containerId);
    network.startPortUpdaters(controller);
  }

  void preDestroyAction() throws ContainerException {
    controller.blockingStop();
  }

  void registerStatus(ContainerStatus status) throws RegistrationException {
    accessor.updateContainerState(containerId, status);
  }

  void throwExceptionIfGenericErrorStateOrOtherState(ContainerStatus stat)
      throws ContainerConfigurationException, UnexpectedContainerStateException {
    if (stateMachine.isGenericErrorState(stat)) {
      throw new ContainerConfigurationException(
          "generic error state reached: " + stat, stateMachine.collectExceptions());
    }
    throw new UnexpectedContainerStateException(
        "unexpected state reached: " + stat, stateMachine.collectExceptions());
  }
}
