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

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.util.state.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LifecycleController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleController.class);

    static Logger getLogger() {
        return LOGGER;
    }

    final LifecycleStore store;
    final ExecutionContext ec;
    private final StateMachine<LifecycleHandlerType> machine;
    private final LifecycleActionInterceptor interceptor;
    private final GlobalRegistryAccessor accessor;

    public LifecycleController(LifecycleStore storeParam,
        LifecycleActionInterceptor interceptorParam, GlobalRegistryAccessor accessorParam,
        ExecutionContext ecParam) {
        store = storeParam;
        ec = ecParam;
        machine = LifecycleControllerTransitions.buildStateMachine(store, ec);
        interceptor = interceptorParam;
        accessor = accessorParam;

    }

    private void preRun(HandlerType type) throws ContainerException {
        interceptor.prepare(type);
    }

    private void postRun(HandlerType type) throws ContainerException {
        interceptor.postprocess(type);
    }

    void run(LifecycleHandlerType type) {
        try {
            preRun(type);
            machine.transit(type);
            postRun(type);
            updateStateInRegistry(type);
        } catch (ContainerException ce) {
            LOGGER
                .warn("Exception when executing state transition. this is not thoroughly handled.",
                    ce);
            throw new IllegalStateException("wrong state: should be in error state", ce);
            // set error state
            // updateStateInRegistry(type);
        }
    }

    private void updateStateInRegistry(LifecycleHandlerType type) {
        try {
            accessor.updateInstanceState(interceptor.getComponentInstanceId(), type);
        } catch (RegistrationException ex) {
            LOGGER.warn("could not update status in registry.", ex);
        }
    }

    public synchronized void blockingInit() {
        run(LifecycleHandlerType.NEW);            // moves to INIT
    }

    public synchronized void skipInstall() {
        run(LifecycleHandlerType.INIT);         // moves to PRE_INSTALL
        run(LifecycleHandlerType.PRE_INSTALL);    // moves to INSTALL
        run(LifecycleHandlerType.INSTALL);        // moves to POST_INSTALL
    }

    public synchronized void blockingInstall() {
        run(LifecycleHandlerType.INIT);         // moves to PRE_INSTALL
        run(LifecycleHandlerType.PRE_INSTALL);    // moves to INSTALL
    }

    public synchronized void blockingConfigure() {
        run(LifecycleHandlerType.INSTALL);        // moves to POST_INSTALL
        run(LifecycleHandlerType.POST_INSTALL);    // moves to PRE_START 
    }

    public synchronized void blockingStart() throws LifecycleException {
        run(LifecycleHandlerType.PRE_START);    // moves to START and calls 'start handler'
        StartDetectorHandler.runStartDetector(interceptor, store.getStartDetector(), ec);
        // FIXME: establish periodic invocation of stop detector
        getLogger().warn("TODO: periodically run stop detector");
 
        run(LifecycleHandlerType.START);        // moves to POST_START
    }

    public synchronized void blockingStop() {
        //throw new UnsupportedOperationException(
          //  "not calling stop handler; this is not part of the state machine (yet).");
        //machine.transit(LifecycleHandlerType.PRE_STOP);
        getLogger().warn("running POST_START state!");
        run(LifecycleHandlerType.POST_START);

        getLogger().warn("running PRE_STOP state!");
        run(LifecycleHandlerType.PRE_STOP);
        //FIXME: is a stop detector action required at this point?
        getLogger().warn("Stopping instance, running PRE_STOP state!");
        run(LifecycleHandlerType.STOP);
        //machine.transit(LifecycleHandlerType.STOP);
    }


    public synchronized void blockingUpdatePorts(@SuppressWarnings("unused") OutPort port,
        PortUpdateHandler handler, PortDiff<DownstreamAddress> diff) throws ContainerException {
        boolean preprocessed = false;
        try {
            interceptor.preprocessPortUpdate(diff);
            preprocessed = true;
            LOGGER.info("updating ports via port handler.");
            handler.execute(ec);
        } catch (ContainerException ce) {
            LOGGER
                .warn("Exception when executing state transition. this is not thoroughly handled.",
                    ce);
            throw new IllegalStateException("wrong state: should be in error state?", ce);
            // set error state
            // updateStateInRegistry(LifecycleHandlerType.START);
        } finally {
            if (preprocessed) {
                interceptor.postprocessPortUpdate(diff);
                updateStateInRegistry(LifecycleHandlerType.START);
            }
        }
    }
}
