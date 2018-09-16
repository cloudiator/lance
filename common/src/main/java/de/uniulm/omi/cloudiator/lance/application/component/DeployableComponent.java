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

package de.uniulm.omi.cloudiator.lance.application.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.PropertyVisitor;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public class DeployableComponent implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployableComponent.class);
    private static final long serialVersionUID = -8008479179768130524L;

    private String name;
    private ComponentId myId;
    private LifecycleStore lifecycle;
    private List<InPort> inPorts;
    private List<OutPort> outPorts;
    private HashMap<String, Class<?>> properties;
    private HashMap<String, ? extends Serializable> defaultValues;

    DeployableComponent(String nameParam, ComponentId idParam, LifecycleStore lifecycleStoreParam, 
            List<InPort> inPortsParam, List<OutPort> outPortsParam, Map<String, Class<?>> propertiesParam, 
            HashMap<String, ? extends Serializable> propertyValuesParam) {
        name = nameParam;
        myId = idParam;
        lifecycle = lifecycleStoreParam;
        inPorts = new ArrayList<>(inPortsParam);
        outPorts = new ArrayList<>(outPortsParam);
        properties = new HashMap<>(propertiesParam);
        defaultValues = propertyValuesParam;
    }

    //Needed for legacy reasons, to make the ComponentBuilder class work before only the DockerComponent and LifecycleComponent classes are used in Cloudiator v2
    public DeployableComponent() {
       LOGGER.warn("Default Constructor call of DeployableComponent is needed for legacy reasons as long as this class is used along with LifecycleComponent and DockerComponent");
    }

    public LifecycleStore getLifecycleStore() {
        return lifecycle;
    }

    public ComponentId getComponentId() { 
    	return myId; 
    }

    public List<InPort> getExposedPorts() {
        List<InPort> ports = new ArrayList<>(inPorts.size());
        for(InPort i : inPorts) {
            ports.add(i);
        }
        return ports;
    }
    
    @Override
    public String toString() {
        return name + ": -> " + inPorts + "@" + myId; 
    }

    public List<OutPort> getDownstreamPorts() {
        return new ArrayList<>(outPorts); 
    }
    
    public void accept(DeploymentContext ctx, PropertyVisitor visitor) {        
        for(Entry<String, Class<?>> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Class<?> type = entry.getValue();
            if(type == OutPort.class) 
                continue;
            Object o = ctx.getProperty(propertyName, type);
            if(o == null) {
                o = defaultValues.get(propertyName);
            }
            if(o == null) {
                LOGGER.warn("propery '" + propertyName + "' has not been defined for the application");
                continue;
            }
            visitor.visit(propertyName, o.toString());
        }
    }

    public String getName() {
        return name;
    }
}
