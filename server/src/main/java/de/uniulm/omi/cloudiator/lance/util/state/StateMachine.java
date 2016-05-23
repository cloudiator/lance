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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateMachine<T extends Enum<?> & State > {

    private final static Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);
    
    private static final Object[] DEFAULT_PARAMS = new Object[0];
    private T status;
    private StateTransition<T> ongoingTransition = null;
    private Future<?> endOfTransition = null;
    private final Map<State, StateTransition<T>> transitions = new HashMap<>();
    
    private final StateSetter<T> setter = new StateSetter<T>() {

        @Override public void setIntermediateState(T state, Future<?> eot) { 
            synchronized(StateMachine.this) {
                status = state;
                endOfTransition = eot;
            } 
        }
        
        @Override public void setFinalState(T state) { 
            synchronized(StateMachine.this) {
                status = state;
                endOfTransition = null;
                ongoingTransition = null;
            } 
        }
    };
    
    // FIXME: introduce error state (and error action)
    StateMachine(T init, @SuppressWarnings("unused") List<T> states, List<StateTransition<T>> st) {
        assert init != null : "init state cannot be null";
        status = init;
        for(StateTransition<T> t : st) {
            StateTransition<T> old = transitions.put(t.getSource(), t);
            if(old != null) {
                throw new IllegalArgumentException("cannot use the same start transaction twice.");
            }
        }
        
        assert ! transitions.isEmpty() : "there should be at least one transition" ; 
    }

    public void waitForTransitionEnd(T endState) {
        final Future<?> f;
        synchronized(this) {
            if(status == null) {
                throw new IllegalStateException();
            }
            if(status == endState) {
                return;
            }
            
            if(ongoingTransition == null) 
                throw new IllegalStateException("no ongoing transition to be finished");
            
            if(!ongoingTransition.hasEndState(endState)) 
                throw new IllegalStateException("ongoing transition has wrong end state");
            
            if(!ongoingTransition.hasIntermediateState(status)) 
                throw new IllegalStateException("ongoing transition has wrong intermediate state");
            
            if(endOfTransition == null) 
                throw new IllegalStateException("no synchronisation entity");
            
            f = endOfTransition;
        }
        waitLoop(f);
    }
    
    private static void waitLoop(Future<?> f) {
        while(true) {
            if(f.isDone()) {
                return;
            }
            
            try { 
                f.get(); 
                return; 
            } catch(InterruptedException ie){
                // we were interrupted; ignore and re-try
                 // FIXME: implement in a correct way
                LOGGER.error("interrupted", ie);
            } catch(CancellationException ce) {
                // task cancelled => state not reached
                // FIXME: set back status or set to error state
                // FIXME: revert changes
                throw new IllegalStateException(ce);
             } catch(ExecutionException ee){
                 // an exception occurred during execution => state not reached
                // FIXME: set back status or set to error state
                 // FIXME: revert changes
                throw new IllegalStateException(ee);
            }
        }
    }

    public void transit(T startState) {
        transit(startState, DEFAULT_PARAMS);
    }
    /**
     * @param startState the start state of the transition 
     * @param params set of application specific parameters to be passed through to the
     * transition
     */
    public synchronized void transit(T startState, Object[] params) {
        StateTransition<T> transition = findTransition(startState);
        if(transition.isIntermediateOrEndState(status)) 
            return ; // we are already done //
        if(!transition.isStartState(status)) 
            throw new IllegalStateException("we are in the wrong state: " + status);
        if(endOfTransition != null) 
            throw new IllegalStateException("we are in the wrong state: endOfTransition is set");
        
        // everything is fine. let's invoke the transition //
        ongoingTransition = transition;
        transition.execute(setter, params);
    }

    private StateTransition<T> findTransition(T startState) {
        StateTransition<T> transition = transitions.get(startState);
        if(transition == null) {
            throw new IllegalStateException("no transition found for startState: " + startState);
        }
        return transition;
    }

    public synchronized T getState() {
        return status;
    }

    public synchronized boolean assertCurrentState(T checkStatus) {
        return status == checkStatus;
    }
}
