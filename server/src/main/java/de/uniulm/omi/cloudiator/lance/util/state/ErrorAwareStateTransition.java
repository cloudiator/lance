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

package de.uniulm.omi.cloudiator.lance.util.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

// FIXME: add some nice, generic callback mechanism that can be
// exploited to update the registry on the fly.
public final class ErrorAwareStateTransition<T extends Enum<?> & State> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ErrorAwareStateMachine.class);

    private final T from;
    private final T intermediate;
    private final T to;
    private final T error;
    private final TransitionAction action;
    private final TransitionErrorHandler<T> errorHandler;
    private final boolean isAsynchronous;

    ErrorAwareStateTransition(T fromParam, T intermediateParam, T toParam, T errorParam,
        TransitionAction actionParam, boolean asynchronous,
        TransitionErrorHandler<T> errorHandlerParam) {
        if (toParam == null)
            throw new NullPointerException();
        if (fromParam == null)
            throw new NullPointerException();
        to = toParam;
        from = fromParam;
        action = actionParam;
        error = errorParam;
        intermediate = intermediateParam;
        isAsynchronous = asynchronous;
        errorHandler = errorHandlerParam;
    }

    State getSource() {
        return from;
    }

    boolean isIntermediateOrEndState(T status) {
        if (status == null)
            throw new NullPointerException();
        return status == to || status == intermediate;
    }

    boolean isStartState(T status) {
        if (status == null)
            throw new NullPointerException();
        return status == from;
    }

    Future<?> triggerTransitionExecution(ErrorAwareTransitionState<T> state, Object[] params,
        ExecutorService executor) {
    	Future<?> f;
    	
        TransitionRunner runner = new TransitionRunner(params, state);

        if (isSynchronous()) {
            FutureTask<T> ft = new FutureTask<>(runner, null);
            runner.registerStartAtState(ft);
            // attention: the transition action is not being executed here
            f = ft;
        } else {
            Future<?> actual = executor.submit(runner);
            runner.registerStartAtState(actual);
            f = actual;
        }
        return f;
    }
    
    void postprocessExecutionTrigger(Future<?> f){
    	if(isSynchronous()) {
    		FutureTask<T> ft = (FutureTask) f;
	    	ft.run();
	        try {
	            ft.get();
	        } catch (InterruptedException e) {
	            LOGGER.error(String.format("%s got interrupted", ft), e);
	            Thread.currentThread().interrupt();
	        } catch (ExecutionException e) {
	            LOGGER.error(String.format("Exception during execution of %s", ft), e.getCause());
	        }
    	} else {
    		// do nothing: transition is already running //
    	}
    }
    

    private boolean isSynchronous() {
        return !isAsynchronous;
    }

    public boolean hasEndState(T endState) {
        return to == endState;
    }

    public boolean hasIntermediateState(T status) {
        return intermediate == status;
    }

    private class TransitionRunner implements Runnable {

        final Object[] params;
        final ErrorAwareTransitionState<T> state;
        final CountDownLatch startSignal = new CountDownLatch(1);

        TransitionRunner(Object[] params, ErrorAwareTransitionState<T> state) {
            this.params = params;
            this.state = state;
        }

        void registerStartAtState(Future<?> ft) {
            if (intermediate != null) {
                state.transitionStarted(intermediate, ft);
            } else {
                state.transitionStarted(from, ft);
            }
            startSignal.countDown();
        }

        void waitForLatch() {
            while (true) {
                try {
                    startSignal.await();
                    return;
                } catch (InterruptedException e) {
                    // TODO implement properly
                }
            }
        }

        @Override public void run() {
            T stateToSet = null;
            TransitionException exception = null;
            try {
                waitForLatch();
                action.transit(params);
                stateToSet = to;
            } catch (TransitionException te) {
                LOGGER.debug(
                    "transition runner failed: cannot execute transition: " + TransitionRunner.this,
                    te);
                if (errorHandler != null) {
                    errorHandler.run(te, from, to);
                }
                if (error != null) {
                    stateToSet = error;
                }
                exception = te;
            } finally {
                state.transitionComplete(stateToSet, exception);
            }
        }
    }

    @Override public String toString() {
        return from + (intermediate != null ? ("--> [" + intermediate + "]") : "") + "-->" + to;
    }
}
