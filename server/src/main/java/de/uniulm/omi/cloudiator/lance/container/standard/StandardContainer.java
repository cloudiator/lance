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

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.util.state.StateMachine;
import de.uniulm.omi.cloudiator.lance.util.state.StateMachineBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;

import static de.uniulm.omi.cloudiator.lance.container.standard.StandardContainerHelper.checkForBootstrapParameters;
import static de.uniulm.omi.cloudiator.lance.container.standard.StandardContainerHelper.checkForCreationParameters;
import static de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus.READY;

// FIXME: move status updates to network handler to this class instead of keeping
// them in the individual container classes 

// FIXME: update component instance status each time a state has been reached

// FIXME: introduce error states to life cycle handling
@Deprecated
public final class StandardContainer<T extends ContainerLogic> implements ContainerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerController.class);

    static Logger getLogger() {
        return LOGGER;
    }

    private final StateMachine<ContainerStatus> stateMachine;
    private final GlobalRegistryAccessor accessor;
    final T logic;
    private final ComponentInstanceId containerId;
    private final NetworkHandler network;
    final LifecycleController controller;
    private boolean shouldBeRemoved;

    public StandardContainer(ComponentInstanceId id, T logicParam, NetworkHandler networkParam,
                             LifecycleController controllerParam, GlobalRegistryAccessor accessorParam) {
        containerId = id;
        logic = logicParam;
        network = networkParam;
        controller = controllerParam;
        accessor = accessorParam;
        stateMachine = addDestroyTransition(
                addInitTransition(addBootstrapTransition(
                        addCreateTransition(new StateMachineBuilder<>(ContainerStatus.NEW).
                                addAllState(ContainerStatus.values())))
                )).build();
        this.shouldBeRemoved = false;
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
        return shouldBeRemoved;
    }

    @Override
    public void setShouldBeRemoved(boolean shouldBeRemoved) {
        this.shouldBeRemoved = shouldBeRemoved;
    }

    @Override
    public void create() {
        stateMachine.transit(ContainerStatus.NEW, new Object[]{});
    }

    @Override
    public void awaitCreation() {
        stateMachine.waitForTransitionEnd(ContainerStatus.CREATED);
    }

    @Override
    public void bootstrap() {
        stateMachine.transit(ContainerStatus.CREATED, new Object[]{});
    }

    @Override
    public void awaitBootstrap() {
        stateMachine.waitForTransitionEnd(ContainerStatus.BOOTSTRAPPED);
    }

    @Override
    public boolean isReady() {
      if (stateMachine.getState().equals(READY)) {
        return true;
      }
      return false;
    }

    @Override
    public void startPortUpdaters() {
       network.startPortUpdaters(controller);
    }

  @Override
  public void registerErrorState(ContainerStatus bootstrappingFailed)
      throws ContainerConfigurationException {
    throw  new ContainerConfigurationException("Method not implemented");
  }

  @Override
    public void init(LifecycleStore store) {
        stateMachine.transit(ContainerStatus.BOOTSTRAPPED, new Object[]{store});
    }

    @Override
    public void init() {
        stateMachine.transit(ContainerStatus.BOOTSTRAPPED, new Object[]{});
    }

    @Override
    public void awaitInitialisation() {
        stateMachine.waitForTransitionEnd(READY);
    }

    @Override
    public void tearDown() {
        stateMachine.transit(READY);
    }

    @Override
    public void awaitDestruction(boolean forceRegDeletion) throws ContainerException {
        stateMachine.waitForTransitionEnd(ContainerStatus.DESTROYED);
        if (forceRegDeletion) {
            deleteInRegistry();
        }
    }

    private void deleteInRegistry() throws ContainerException {
        try {
            accessor.deleteComponentInstance();
        } catch (RegistrationException re) {
            throw new ContainerException("cannot access registry.", re);
        }
    }

    private void setNetworking() throws ContainerException {
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
    }

    void preInitAction() throws LifecycleException {
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

    private StateMachineBuilder<ContainerStatus> addCreateTransition(StateMachineBuilder<ContainerStatus> b) {
        return b.addAsynchronousTransition(ContainerStatus.NEW, ContainerStatus.CREATING, ContainerStatus.CREATED,
                new TransitionAction() {
                    @Override
                    public void transit(Object[] params) {
                        try {
                            preCreateAction();
                            checkForCreationParameters(params);
                            logic.doCreate();
                            postCreateAction();
                            registerStatus(ContainerStatus.CREATED);
                        } catch (ContainerException | RegistrationException ce) {
                            getLogger().error("could not create container; FIXME add error state", ce); 
                            /* FIXME: change to error state */
                        }
                    }
                });
    }

    private StateMachineBuilder<ContainerStatus> addBootstrapTransition(StateMachineBuilder<ContainerStatus> b) {
        return b.addAsynchronousTransition(ContainerStatus.CREATED, ContainerStatus.BOOTSTRAPPING, ContainerStatus.BOOTSTRAPPED,
                new TransitionAction() {
                    @Override
                    public void transit(Object[] params) {
                        try {
                            checkForBootstrapParameters(params);
                            logic.doInit(null);
                            postBootstrapAction();
                            registerStatus(ContainerStatus.BOOTSTRAPPED);
                        } catch (ContainerException | RegistrationException ce) {
                            getLogger().error("could not initialise container; FIXME add error state", ce); 
                            /* FIXME: change to error state */
                        }
                    }
                });
    }

    private StateMachineBuilder<ContainerStatus> addInitTransition(StateMachineBuilder<ContainerStatus> b) {
        return b.addAsynchronousTransition(ContainerStatus.BOOTSTRAPPED, ContainerStatus.INITIALISING, READY,
                new TransitionAction() {
                    @Override
                    public void transit(Object[] params) {
                        //TODO: add code for starting from snapshot (skip init and install steps)
                        try {
                            logic.preInit();
                            preInitAction();
                            postInitAction();
                            registerStatus(READY);
                        } catch (ContainerException | LifecycleException | RegistrationException ce) {
                            getLogger().error("could not initialise container; FIXME add error state", ce); 
                            /* FIXME: change to error state */
                        }
                    }
                });
    }

    private StateMachineBuilder<ContainerStatus> addDestroyTransition(StateMachineBuilder<ContainerStatus> b) {
        return b.addAsynchronousTransition(READY, ContainerStatus.SHUTTING_DOWN, ContainerStatus.DESTROYED,
                new TransitionAction() {
                    @Override
                    public void transit(Object[] params) {
                        network.stopPortUpdaters();
                        try {
                            boolean forceShutdown = false;
                            try {
                                preDestroyAction();
                            } catch (Exception ex) {
                                getLogger().error("could not shut down component; trying to force shut down of container", ex);
                                forceShutdown = true;
                            }
                            logic.doDestroy(forceShutdown, shouldBeRemoved);
                            registerStatus(ContainerStatus.DESTROYED);
                        } catch (ContainerException | RegistrationException ce) {
                            getLogger().error("could not shut down container; FIXME add error state", ce); 
                            /* FIXME: change to error state */
                        } finally {
                            try {
                                setNetworking();
                            } catch (ContainerException e) {
                                getLogger().error("could not update networking", e);
                            }
                            try {
                                network.publishLocalData(containerId);
                            } catch (ContainerException e) {
                                getLogger().error("could not publish local data", e);
                            }
                        }

                    }
                });
    }
}
