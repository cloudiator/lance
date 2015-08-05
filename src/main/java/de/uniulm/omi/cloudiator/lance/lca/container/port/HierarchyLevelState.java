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
import java.util.Iterator;
import java.util.Map;

public final class HierarchyLevelState<T> implements Iterable<PortHierarchyLevel> {

	private final String name;
	private final Map<PortHierarchyLevel, T> mapping = new HashMap<PortHierarchyLevel, T>(); 
	
	public HierarchyLevelState(String _name, PortHierarchy portHierarchy) {
		name = _name;
		for(PortHierarchyLevel level : portHierarchy.levels()) {
			mapping.put(level, null);
		}
	}

	public void registerValueAtLevel(PortHierarchyLevel level, T value) {
		if(value == null) throw new NullPointerException("cannot register a hierarchy state that has the value null");
		T i = mapping.put(level, value);
		if(i != null) {
			System.err.println("updating hierarchy state '" + name + "' for level '" + level + "': changin from " + i + " to " + value);
		}
	}
	
	public Iterator<PortHierarchyLevel> iterator(){
		return mapping.keySet().iterator();
	}

	T valueAtLevel(PortHierarchyLevel level) {
		T t = mapping.get(level);
		if(t == null) throw new NullPointerException("value at level '" + level + "' is not known");
		return t;
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mapping == null) ? 0 : mapping.hashCode());
		// result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof HierarchyLevelState<?>)) return false;
		final HierarchyLevelState<?> that = (HierarchyLevelState<?>) o;
		for(PortHierarchyLevel level : this) {
			if(! containsEqualElementAtSameLevel(level, that)) return false;
		}
		// now we know that it contains all values that we have as well. 
		// make sure, there are not more than that.
		return this.mapping.size() == that.mapping.size();
	}

	private boolean containsEqualElementAtSameLevel(PortHierarchyLevel level, HierarchyLevelState<?> state) {
		T myElement = mapping.get(level);
		if(! state.mapping.containsKey(level)) return false;
		
		Object that = state.mapping.get(level);
		
		if(myElement == null && that == null) return true;
		if(myElement == null && that != null) return false;
		return myElement.equals(that);
	}
}
