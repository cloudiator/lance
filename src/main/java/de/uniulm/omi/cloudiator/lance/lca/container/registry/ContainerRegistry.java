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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;

public final class ContainerRegistry {
    
    private final Map<ComponentInstanceId, ContainerController> containers = new HashMap<>();
    
    public final synchronized ComponentInstanceId addContainer(ContainerController containerParam) {
        if(containerParam == null) 
            throw new IllegalArgumentException("container is null");
        
        ComponentInstanceId id = containerParam.getId();
        containers.put(id, containerParam);
        return id;
    }

    public synchronized ContainerController getContainer(ComponentInstanceId idParam) {
        return containers.get(idParam);
    }

	public synchronized List<ComponentInstanceId> listComponentInstances() {
		List<ComponentInstanceId> arrayList = new ArrayList<>(containers.keySet());
		return arrayList;
	}
}
