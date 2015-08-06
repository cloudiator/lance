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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class PortHierarchy {

    private final List<PortHierarchyLevel> levels;
    
    private PortHierarchy(List<PortHierarchyLevel> _levels) { levels = _levels; }
    
    public static class PortHierarchyBuilder {

        private final List<PortHierarchyLevel> levels = new LinkedList<PortHierarchyLevel>(); 
        
        public PortHierarchyBuilder() {}
        
        public PortHierarchyBuilder addLevel(PortHierarchyLevel level) { levels.add(level); return this; }
        
        public PortHierarchy build() {
            if(levels.size() < 2) throw new IllegalStateException("at least two levels are required");
            if(levels.get(0) == null || !levels.get(0).equals(PortRegistryTranslator.PORT_HIERARCHY_0))
                throw new IllegalStateException("hierarchy level 0 must be " + PortRegistryTranslator.PORT_HIERARCHY_0);
            if(levels.get(1) == null || !levels.get(1).equals(PortRegistryTranslator.PORT_HIERARCHY_1))
                throw new IllegalStateException("hierarchy level 1 must be " + PortRegistryTranslator.PORT_HIERARCHY_1);
            return new PortHierarchy(levels);
        }
    }

    public List<PortHierarchyLevel> levels() {
        return Collections.unmodifiableList(levels);
    }
}
