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

package de.uniulm.omi.cloudiator.lance.lca.container.port;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

public final class PortDiff<T> {

	private final Map<ComponentInstanceId, HierarchyLevelState<T>> current;
	private final Set<ComponentInstanceId> added;
	private final Set<ComponentInstanceId> removed;
	@SuppressWarnings("unused") private final String portName;
	private final Set<ComponentInstanceId> diffSet;
	
	PortDiff(Map<ComponentInstanceId, HierarchyLevelState<T>> new_sinks,
			Map<ComponentInstanceId, HierarchyLevelState<T>> old_sinks,
			String _portName) {
		
		portName = _portName;
		current = new_sinks;
		added  = inFirstNotInSecond(new_sinks, old_sinks);
		removed = inFirstNotInSecond(old_sinks, new_sinks);
		diffSet = diffPerElement(old_sinks);
	}

	private Set<ComponentInstanceId> diffPerElement(Map<ComponentInstanceId, HierarchyLevelState<T>> old) {
		Set<ComponentInstanceId> _diffSet = new HashSet<ComponentInstanceId>();
		for(Entry<ComponentInstanceId, HierarchyLevelState<T>> entry : current.entrySet()) {
			ComponentInstanceId id = entry.getKey();
			if(added.contains(id)) continue;
			HierarchyLevelState<T> old_elements = old.get(id);
			HierarchyLevelState<T> new_elements = entry.getValue();
			if(diffCrititcalElements(old_elements, new_elements)) _diffSet.add(id);
		}
		return _diffSet;
	}
	
	/**
	 * 
	 * @param old_elements
	 * @param new_elements
	 * @return true if the both elements do differ (are not equal to each other).
	 */
	private boolean diffCrititcalElements(HierarchyLevelState<T> old_elements, HierarchyLevelState<T> new_elements) {
		if(old_elements == null) return new_elements != null;
		return !old_elements.equals(new_elements);
	}
	
	private static<T> Set<T> inFirstNotInSecond(Map<T,?> first, Map<T,?> second) {
		Set<T> elements = new HashSet<T>();
		for(T t : first.keySet()) {
			if(second.containsKey(t)) continue;
			elements.add(t);
		}
		return elements;
	}

	public Map<ComponentInstanceId, HierarchyLevelState<T>> getCurrentSinkSet() {
		return new HashMap<ComponentInstanceId, HierarchyLevelState<T>>(current);
	}

	public boolean hasDiffs() {
		return added.size() > 0 || removed.size() > 0 || diffSet.size() > 0;
	}
}
