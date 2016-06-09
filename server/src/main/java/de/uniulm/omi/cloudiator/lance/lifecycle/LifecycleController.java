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
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareStateMachine;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareStateMachineBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LifecycleController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleController.class);

    static Logger getLogger() {
        return LOGGER;
    }

    final LifecycleStore store;
    final ExecutionContext ec;
    private final ErrorAwareStateMachine<LifecycleHandlerType> machine;
    private final LifecycleActionInterceptor interceptor;
    private final GlobalRegistryAccessor accessor;
    private final StopDetectorHandler stopDetector;
    private final StopDetectorCallback callback;
    private final HostContext hostContext;

    public LifecycleController(LifecycleStore storeParam,
        LifecycleActionInterceptor interceptorParam, GlobalRegistryAccessor accessorParam,
        ExecutionContext ecParam, HostContext hostContextParam) {
        store = storeParam;
        ec = ecParam;
        interceptor = interceptorParam;
        accessor = accessorParam;
        machine = initStateMachine();
        callback = new StopDetectorCallbackImpl();
        hostContext = hostContextParam;
        stopDetector = StopDetectorHandler.create(interceptorParam, store.getStopDetector(), ecParam, callback);
    }
    
    private ErrorAwareStateMachine<LifecycleHandlerType> initStateMachine() {
    	ErrorAwareStateMachineBuilder<LifecycleHandlerType> builder = 
    			new ErrorAwareStateMachineBuilder<>(LifecycleHandlerType.NEW, LifecycleHandlerType.UNKNOWN);
    	LifecycleTransitionHelper.createInitAction(builder.getTransitionBuilder(), store, ec);
    	
    	LifecycleTransitionHelper.createPreInstallAction(builder.getTransitionBuilder(), store, ec);
    	LifecycleTransitionHelper.createInstallAction(builder.getTransitionBuilder(), store, ec);
    	LifecycleTransitionHelper.createSkipInstallAction(builder.getTransitionBuilder());
    	
    	LifecycleTransitionHelper.createPostInstallAction(builder.getTransitionBuilder(), store, ec);
    	LifecycleTransitionHelper.createPreStartAction(builder.getTransitionBuilder(), store, ec);
    	StartTransitionAction.createStartAction(builder.getTransitionBuilder(), store, ec, interceptor);
    	LifecycleTransitionHelper.createPostStartAction(builder.getTransitionBuilder(), store, ec);

    	LifecycleTransitionHelper.createPreStopAction(builder.getTransitionBuilder(), store, ec);
    	LifecycleTransitionHelper.createStopAction(builder.getTransitionBuilder(), store, ec);
    	LifecycleTransitionHelper.createSkipStopAction(builder.getTransitionBuilder());

    	LifecycleTransitionHelper.createPostStopAction(builder.getTransitionBuilder(), store, ec);
    	return builder.build();
    }

    private void preRun(HandlerType from, LifecycleHandlerType to) throws ContainerException {
        interceptor.prepare(from);
    }

    private void postRun(HandlerType from, LifecycleHandlerType to) throws ContainerException {
        interceptor.postprocess(from);
    }

    private void run(LifecycleHandlerType from, LifecycleHandlerType to) {
        try {
            preRun(from, to);
            machine.transit(from, to);
            postRun(from, to);
            updateStateInRegistry(from);
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
        run(LifecycleHandlerType.NEW, LifecycleHandlerType.INIT); // executes INIT handler
    }

    public synchronized void skipInstall() {
    	// moves from INIT to INSTALL without running any handlers 
        run(LifecycleHandlerType.INIT, LifecycleHandlerType.PRE_INSTALL);  
    }

    public synchronized void blockingInstall() {
        run(LifecycleHandlerType.INIT, LifecycleHandlerType.PRE_INSTALL); // executes pre_install handler
        run(LifecycleHandlerType.PRE_INSTALL, LifecycleHandlerType.INSTALL); // executes INSTALL handler
    }

    public synchronized void blockingConfigure() {
        run(LifecycleHandlerType.INSTALL, LifecycleHandlerType.POST_INSTALL); // executes POST_INSTALL handler
        run(LifecycleHandlerType.POST_INSTALL, LifecycleHandlerType.PRE_START); // executes PRE_START handler 
    }

    public synchronized void blockingStart() throws LifecycleException {
    	// calls 'start handler' and verifies start-up via start detector
        run(LifecycleHandlerType.PRE_START, LifecycleHandlerType.START); 
        
        stopDetector.scheduleDetection(hostContext);
        
        // invokes POST_START handler and installs stop detector
        run(LifecycleHandlerType.START, LifecycleHandlerType.POST_START);
    }

    public synchronized void blockingStop() {
    	stopDetector.clearSchedule();
    	if(callback.getDetectedState() == DetectorState.NOT_DETECTED) {
    		run(LifecycleHandlerType.POST_START, LifecycleHandlerType.PRE_STOP);
    		run(LifecycleHandlerType.PRE_STOP, LifecycleHandlerType.STOP);
    		try { 
    			stopDetector.waitForFinalShutdown();
    		} catch(LifecycleException lce) {
    			// FIXME: here, external resources should be cleared and 
    			// and instance should be moved to error state 
    		}
    		run(LifecycleHandlerType.STOP, LifecycleHandlerType.POST_STOP);
    	} else {
    		// this remains to be discussed //
    	}
    }
    
    synchronized void accidentalStop() {
		// directly move to STOP. TODO: do we need to force the application?
		run(LifecycleHandlerType.POST_START, LifecycleHandlerType.UNEXPECTED_EXECUTION_STOP);
    }

    public synchronized void blockingUpdatePorts(OutPort port, PortUpdateHandler handler, PortDiff<DownstreamAddress> diff) throws ContainerException {
        boolean preprocessed = false;
        try {
            interceptor.preprocessPortUpdate(diff);
            preprocessed = true;
            getLogger().info("updating ports via port handler.");
            handler.execute(ec);
        } catch (ContainerException ce) {
        	getLogger()
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
    
    class StopDetectorCallbackImpl implements StopDetectorCallback {

    	private DetectorState myState = DetectorState.NOT_DETECTED;
    	
		@Override
		public synchronized void state(DetectorState state) {
			myState = state;
			switch(state){
    			case DETECTION_FAILED: 
    				getLogger().debug("stop detection failed.");
	    		case DETECTED: 
	    			return;
	    		case NOT_DETECTED:
	    			 return;
	    		default:
	    			throw new IllegalStateException("state " + state + " not captured");
			}
		}

		@Override
		public synchronized DetectorState getDetectedState() {
			return myState;
		}

		@Override
		public synchronized void exceptionOccurred(Exception ex) {
			getLogger().debug("exception when running stop detector; quitting.", ex);
			myState = DetectorState.DETECTION_FAILED;
			stopDetector.clearSchedule();
			accidentalStop();
		}
    	
    }
}
