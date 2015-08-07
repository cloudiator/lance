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

import java.util.concurrent.Future;

import de.uniulm.omi.cloudiator.lance.util.AsyncCallback;
import de.uniulm.omi.cloudiator.lance.util.AsyncRunner;
import de.uniulm.omi.cloudiator.lance.util.AsyncRunner.Setter;

// FIXME: add some nice, generic callback mechanism that can be
// exploited to update the registry on the fly.
public final class StateTransition<T extends Enum<?> & State > {

    private final T from;
    private final T intermediate;
    private final T to;
    private final TransitionAction action;
    
    static<T extends Enum<?> & State> StateTransition<T> synchronousTransition(T fromParam, T toParam, TransitionAction actionParam) {
        return new StateTransition<>(fromParam, null, toParam, actionParam);
    }
    
    static <T extends Enum<?> & State> StateTransition<T> asynchronousTransition(T fromParam, T intermediateParam, T toParam, TransitionAction actionParam) {
        return new StateTransition<>(fromParam, intermediateParam, toParam, actionParam);
    }
    
    private StateTransition(T fromParam, T intermediateParam, T toParam, TransitionAction actionParam) {
        to = toParam;
        from = fromParam;
        action = actionParam;
        intermediate = intermediateParam;
    }

    State getSource() { return from; }

    boolean isIntermediateOrEndState(T status) {
        if(status == null) 
            throw new NullPointerException();
        return status == to || status == intermediate;
    }

    boolean isStartState(T status) {
        if(status == null) 
            throw new NullPointerException();
        return status == from;
    }

    void execute(final StateSetter<T> setter, final Object[] params) {
        if(isSynchronous()) {
            action.transit(params);
            setter.setFinalState(to);
        } else {
            Thread t = AsyncRunner.createWrappedStateRunner(
                    new Setter() { 
                        @Override public void set() { 
                            action.transit(params);
                            setter.setFinalState(to);
                        } 
                    },
                    new AsyncCallback() { @Override public void call(Future future) {
                        setter.setIntermediateState(intermediate, future);
                        }
                    });
            
            t.start();
        }
    }
    
    private boolean isSynchronous() {
        return intermediate == null;
    }

    public boolean hasEndState(T endState) {
        return to == endState;
    }

    public boolean hasIntermediateState(T status) {
        return intermediate == status;
    }
}
