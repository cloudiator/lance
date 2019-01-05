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

package de.uniulm.omi.cloudiator.lance.lca.container;

import com.google.common.collect.Sets;
import de.uniulm.omi.cloudiator.lance.util.state.State;
import java.util.Set;

public enum ContainerStatus implements State {
  /** it is known to the system that there shall be container instance. */
  NEW,
  CREATING,
  /**
   * the container has been created on a physical level. yet, the concrete information about e.g.
   * internal IP addresses and port mappings (if applicable are not know yet.
   */
  CREATED,
  BOOTSTRAPPING,
  /**
   * the container has been bootstrapped. it has acquired all necessary phyiscal resources and is
   * ready for operation. Port mappings and IP addresses are known. Only now will the lifecycle
   * handling be able to run the lifecycle for the component instances.
   */
  BOOTSTRAPPED,
  /** the management platform is running the lifecycle handling of the component instance. */
  INITIALISING,
  /** the component instance has been created and is availalble for use */
  READY,
  SHUTTING_DOWN,
  DESTROYED,

  CREATION_FAILED,
  BOOTSTRAPPING_FAILED,
  INITIALISATION_FAILED,

  UNKNOWN,
  ;

  public static Set<ContainerStatus> errorStates() {
    return Sets.newHashSet(
        DESTROYED, CREATION_FAILED, BOOTSTRAPPING_FAILED, INITIALISATION_FAILED, UNKNOWN);
  }
}
