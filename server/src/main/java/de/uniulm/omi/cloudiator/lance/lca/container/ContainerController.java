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

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public interface ContainerController {

  ComponentInstanceId getId();

  ContainerStatus getState();

  boolean shouldBeRemoved();

  void setShouldBeRemoved(boolean shouldBeRemoved);

  void create() ;

	void bootstrap() ;

  void init(LifecycleStore store) ;

  void init();

  void tearDown() ;

  void awaitInitialisation() throws ContainerException;

  void awaitCreation() throws ContainerException;

  void awaitDestruction(boolean forceRegDeletion) throws ContainerException;

	void awaitBootstrap() throws ContainerException;

	boolean isReady();

	void startPortUpdaters();

  void registerErrorState(ContainerStatus failedState) throws ContainerConfigurationException;
}