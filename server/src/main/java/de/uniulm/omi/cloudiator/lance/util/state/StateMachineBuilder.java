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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Deprecated
public final class StateMachineBuilder<T extends Enum<?> & State > {

    private final T init;
    private final Set<T> states = new HashSet<>();
    private final List<StateTransition<T>> transitions = new ArrayList<>();
    
    public StateMachineBuilder(T initState) {
        init = initState;
        states.add(initState);
    }
    
    public StateMachineBuilder<T> addState(T state) {
        states.add(state);
        return this;
    }

    public StateMachineBuilder<T> addSynchronousTransition(T fromParam, T toParam, TransitionAction aParam){
        StateTransition<T> st = StateTransition.synchronousTransition(fromParam, toParam, aParam);
        transitions.add(st);
        return this;
    }
    
    public StateMachineBuilder<T> addAsynchronousTransition(T fromParam, T intermediateParam, T toParam, TransitionAction aParam){
        StateTransition<T> st = StateTransition.asynchronousTransition(fromParam, intermediateParam, toParam, aParam);
        transitions.add(st);
        return this;
    }
    
    public StateMachine<T> build() {
        return new StateMachine<>(init, new ArrayList<>(states), transitions);
    }

    public StateMachineBuilder<T> addAllState(T[] values) {
        for(T v : values) { 
            addState(v); 
        }
        return this;
    }

}
