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

package de.uniulm.omi.cloudiator.lance.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.InitHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.InstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostStartHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreStartHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StartHandler;
import de.uniulm.omi.cloudiator.lance.util.state.StateMachine;
import de.uniulm.omi.cloudiator.lance.util.state.StateMachineBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;

public final class LifecycleController {
    
    static final Logger LOGGER = LoggerFactory.getLogger(LifecycleController.class);

    final LifecycleStore store;
    final ExecutionContext ec;
    private final StateMachine<LifecycleHandlerType> machine;
    
    public LifecycleController(LifecycleStore _store, OperatingSystem _os, ShellFactory _shellFactory) {
        store = _store;
        ec = new ExecutionContext(_os, _shellFactory);
        machine = buildStateMachine();
    }

    public synchronized void blockingInit() {
        machine.transit(LifecycleHandlerType.NEW);            // moves to INIT
    }

    public synchronized void skipInstall() {
        machine.transit(LifecycleHandlerType.INIT);         // moves to PRE_INSTALL
        machine.transit(LifecycleHandlerType.PRE_INSTALL);    // moves to INSTALL
        machine.transit(LifecycleHandlerType.INSTALL);        // moves to POST_INSTALL
    }
    
    public synchronized void blockingInstall() {
        machine.transit(LifecycleHandlerType.INIT);         // moves to PRE_INSTALL
        machine.transit(LifecycleHandlerType.PRE_INSTALL);    // moves to INSTALL
    }

    public synchronized void blockingConfigure() {
        machine.transit(LifecycleHandlerType.INSTALL);        // moves to POST_INSTALL
        machine.transit(LifecycleHandlerType.POST_INSTALL);    // moves to PRE_START 
    }
    
    public synchronized void blockingStart() {
        machine.transit(LifecycleHandlerType.PRE_START);    // moves to START and calls 'start handler'
        // FIXME: establish start detector
        // machine.transit(LifecycleHandlerType.START);        // moves to POST_START
    }
    
    public synchronized void blockingUpdatePorts(PortUpdateHandler handler) {
        handler.execute(ec);
        // FIXME: change to according state //
    }

    private StateMachine<LifecycleHandlerType> buildStateMachine() {
        return addInitTransition(
                addInstallTransitions(
                        addStartTransitions(
                                new  StateMachineBuilder<>(LifecycleHandlerType.NEW).
                                addAllState(LifecycleHandlerType.values())
                    ))).build();
    }
    
    private StateMachineBuilder<LifecycleHandlerType> addInitTransition(StateMachineBuilder<LifecycleHandlerType> b) {
        return b.addSynchronousTransition(LifecycleHandlerType.NEW, LifecycleHandlerType.INIT,
                new TransitionAction() {
                    @Override public void transit(Object[] params) {
                        InitHandler h = store.getHandler(LifecycleHandlerType.INIT, InitHandler.class);
                        h.execute(ec);
                    }
                });
    }
    
    private StateMachineBuilder<LifecycleHandlerType> addInstallTransitions(StateMachineBuilder<LifecycleHandlerType> b) {
        return b.addSynchronousTransition(LifecycleHandlerType.INIT, LifecycleHandlerType.PRE_INSTALL,
                new TransitionAction() {
                    @Override public void transit(Object[] params) {
                        PreInstallHandler h = store.getHandler(LifecycleHandlerType.PRE_INSTALL, PreInstallHandler.class);
                        h.execute(ec);
                    }
                }).
                addSynchronousTransition(LifecycleHandlerType.PRE_INSTALL, LifecycleHandlerType.INSTALL, 
                    new TransitionAction() {
                        @Override public void transit(Object[] params) {
                            InstallHandler h = store.getHandler(LifecycleHandlerType.INSTALL, InstallHandler.class);
                            h.execute(ec);
                        }
                }). 
                addSynchronousTransition(LifecycleHandlerType.INSTALL, LifecycleHandlerType.POST_INSTALL, 
                    new TransitionAction() {
                        @Override public void transit(Object[] params) {
                            PostInstallHandler h = store.getHandler(LifecycleHandlerType.POST_INSTALL, PostInstallHandler.class);
                            h.execute(ec);
                        }
                });        
    }
    
    private StateMachineBuilder<LifecycleHandlerType> addStartTransitions(StateMachineBuilder<LifecycleHandlerType> b) {
        return b.addSynchronousTransition(LifecycleHandlerType.POST_INSTALL, LifecycleHandlerType.PRE_START, 
                new TransitionAction() {
                    @Override public void transit(Object[] params) {
                            PreStartHandler h = store.getHandler(LifecycleHandlerType.PRE_START, PreStartHandler.class);
                            h.execute(ec);
                    }
                }).
                addSynchronousTransition(LifecycleHandlerType.PRE_START, LifecycleHandlerType.START,
                    new TransitionAction() {
                        @Override public void transit(Object[] params) {
                            StartHandler h = store.getHandler(LifecycleHandlerType.START, StartHandler.class);
                            h.execute(ec);
                            // FIXME: run start detector and stop detector //
                            LOGGER.warn("WARNING: run start detector and stop detector");
                        }
                }). 
                addSynchronousTransition(LifecycleHandlerType.START, LifecycleHandlerType.POST_START,
                    new TransitionAction() {
                        @Override public void transit(Object[] params) {
                            PostStartHandler h = store.getHandler(LifecycleHandlerType.POST_START, PostStartHandler.class);
                            h.execute(ec);
                    }
                });
    }
}
