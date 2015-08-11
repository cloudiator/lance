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

import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandSequence;

/** an implementation of this interface
 * controls all containers asscociated 
 * with a particular lifecycle agent
 * (there is one LifecycleAgent per
 * node and one ContainerManager per
 * lifecycle agent)
 * 
 * @author Joerg Domaschka
 *
 */
public interface ContainerManager {

    ContainerType getContainerType();
    ContainerController getContainer(ComponentInstanceId id);
    //void runApplication(ComponentInstanceId id, LifecycleStore store);
    //CommandSequence addDeploymentSequence();
    ContainerController createNewContainer(DeploymentContext ctx, DeployableComponent component, OperatingSystem os) throws ContainerException;
    void terminate();
    List<ComponentInstanceId> getAllContainers();
    ContainerStatus getComponentContainerStatus(ComponentInstanceId cid);
}
