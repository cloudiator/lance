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

public final class PortHierarchyLevel {

	private final String levelName;
	
	private PortHierarchyLevel(String name) {
		if(name == null) throw new NullPointerException("level name needs to be set.");
		if(name.indexOf(" ") > -1) throw new IllegalStateException("hierarchy level must not contain spaces");
		if(name.isEmpty()) throw new IllegalStateException("hierarchy level must not be empty");
		if(name.indexOf(":") > -1) throw new IllegalStateException("hierarchy level must not contain colons");
		levelName = name;
	}
	
	@Override
	public int hashCode() {
		return 31 + levelName.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(! (o instanceof PortHierarchyLevel)) return false;
		PortHierarchyLevel that = (PortHierarchyLevel) o;
		return this.levelName.equals(that.levelName);
	}
	
	public static PortHierarchyLevel create(String string) {
		return new PortHierarchyLevel(string);
	}

	public String getName() {
		return levelName;
	}
	
	public String toString() {
		return "HierachyLevel: " + levelName;
	}
}
