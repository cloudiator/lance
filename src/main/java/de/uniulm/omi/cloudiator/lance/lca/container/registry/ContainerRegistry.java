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

package de.uniulm.omi.cloudiator.lance.lca.container.registry;

import java.util.HashMap;

import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;

public final class ContainerRegistry<T extends ContainerLogic> {
    
    private HashMap<ComponentInstanceId, ContainerController> containers = new HashMap<ComponentInstanceId, ContainerController>();
    
    public final ComponentInstanceId addContainer(ContainerController _container) {
        if(_container == null) throw new IllegalArgumentException("container is null");
        
        ComponentInstanceId id = _container.getId();
        containers.put(id, _container);
        return id;
    }

    public ContainerController getContainer(ComponentInstanceId _id) {
        return containers.get(_id);
    }
}
