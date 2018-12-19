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

public final class ErrorAwareStateMachineBuilder<T extends Enum<?> & State> {

  private final T init;
  private final T errorState;
  private final Set<T> states = new HashSet<>();
  private final List<ErrorAwareStateTransition<T>> transitions = new ArrayList<>();

  public ErrorAwareStateMachineBuilder(T initState, T genericErrorState) {
    init = initState;
    errorState = genericErrorState;
    states.add(initState);
    states.add(genericErrorState);
  }

  public ErrorAwareStateMachineBuilder<T> addState(T state) {
    states.add(state);
    return this;
  }

  public ErrorAwareStateMachineBuilder<T> addTransition(ErrorAwareStateTransition<T> t) {
    transitions.add(t);
    return this;
  }

  public ErrorAwareStateMachine<T> build() {
    return new ErrorAwareStateMachine<>(init, errorState, new ArrayList<>(states), transitions);
  }

  public ErrorAwareStateMachineBuilder<T> addAllState(T[] values) {
    for (T v : values) {
      addState(v);
    }
    return this;
  }

  public ErrorAwareTransitionBuilder<T> getTransitionBuilder() {
    return new ErrorAwareTransitionBuilder<>(this);
  }
}
