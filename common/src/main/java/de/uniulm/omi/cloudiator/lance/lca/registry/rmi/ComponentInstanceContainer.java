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

package de.uniulm.omi.cloudiator.lance.lca.registry.rmi;

import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.INSTANCE_NR;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

final class ComponentInstanceContainer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LcaRegistry.class);

    private final AtomicInteger counter = new AtomicInteger(0);
    private final AppInstanceContainer myContainer;
    private final ComponentId cid;
    private final Map<ComponentInstanceId, Map<String,Object>> instances = new HashMap<>();
    
    public ComponentInstanceContainer(AppInstanceContainer cnt, ComponentId cidParam, 
            @SuppressWarnings("unused") String name) { 
        myContainer = cnt; 
        cid = cidParam; 
    }
    
    public String getComponentProperty(ComponentInstanceId myId, String name) {
        Map<String,Object> props = instances.get(myId);
        if(props == null) 
            throw new IllegalArgumentException("not known: " + myId);
        Object old = props.get(name);
        return old == null ? null : old.toString();
    }

    public Map<ComponentInstanceId, Map<String,String>> dumpInstances() {
        Map<ComponentInstanceId, Map<String,String>> copy = new HashMap<>();
        for(Entry<ComponentInstanceId, Map<String, Object>> entry : instances.entrySet()) {
            ComponentInstanceId id = entry.getKey();
            Map<String,Object> content = entry.getValue();
            Map<String,String> innerCopy = new HashMap<>();
            for(Entry<String, Object> innerEntry : content.entrySet()) {
                innerCopy.put(innerEntry.getKey(), innerEntry.getValue().toString());
            }
            copy.put(id, innerCopy);
        }
        return copy;
    }

    public void addComponentProperty(ComponentInstanceId cinstId, String property, Object value) {
        Map<String,Object> props = instances.get(cinstId);
        if(props == null) 
            throw new IllegalArgumentException("not known: " + cinstId);
        Object old = props.put(property, value);
        String add = "";
        if(old != null) { 
            add = " (old value was " + old + ")";
        }
        //FIXME: wake up listeners (?)
        LOGGER.error("TODO: wake up listeners");
        
        LOGGER.info("LcaRegistry: added property: " + this + "/" + cinstId + "." + property + "=" + value + old);
    }

    public void addComponentInstance(ComponentInstanceId cinstId) {
        if(instances.containsKey(cinstId)) 
            throw new IllegalArgumentException("alread exists: " + cinstId);
        Map<String,Object> map = new HashMap<>();
        Integer i = Integer.valueOf(counter.incrementAndGet());
        map.put(LcaRegistryConstants.regEntries.get(INSTANCE_NR), i);
        instances.put(cinstId, map);
        
        LOGGER.info("LcaRegistry: added component instance: " + this + "/" + cinstId);
    }
    
    @Override
    public String toString() {
        return myContainer + "/" + cid;
    }
}
