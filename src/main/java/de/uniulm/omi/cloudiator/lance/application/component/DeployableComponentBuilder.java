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

import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;

public final class DeployableComponentBuilder {

    private final String name;
    private final ComponentId componentId;
    private final List<InPort> inports = new ArrayList<>();
    private final List<OutPort> outports = new ArrayList<>();
    private volatile LifecycleStore store;
    // private volatile boolean deploySequentially = false;
    
    private final HashMap<String, Class<?>> properties = new HashMap<>();
    private final HashMap<String, Serializable> propertyValues = new HashMap<>();
    
    private DeployableComponentBuilder(String _name, ComponentId _id) {
        name = _name;
        componentId = _id;
    }
    
    public final void addInport(String portname, PortType type, int cardinality) {
        inports.add(new InPort(portname, type, cardinality));
        addProperty(portname, InPort.class);
    }
    
    public final void addInport(String portname, PortType type, int cardinality, int defaultPortNr) {
        inports.add(new InPort(portname, type, cardinality));
        addProperty(portname, InPort.class, Integer.valueOf(defaultPortNr));
    }
    
    public final void addOutport(String portname, PortUpdateHandler handler, int cardinality) {
        outports.add(new OutPort(portname, handler, cardinality, OutPort.NO_SINKS, OutPort.INFINITE_SINKS));
        addProperty(portname, OutPort.class);
    }
    
    public final void addOutport(String portname, PortUpdateHandler handler, int cardinality, int minSinks) {
        outports.add(new OutPort(portname, handler, cardinality, minSinks, OutPort.INFINITE_SINKS));
        addProperty(portname, OutPort.class);
    }
    
    public final void addOutport(String portname, PortUpdateHandler handler, int cardinality, int minSinks, int maxSinks) {
        if(minSinks < 0 || maxSinks < 1 || minSinks > maxSinks) {
            throw new IllegalArgumentException("minSinks and maxSinks have non-fitting values: " + minSinks + ", " + maxSinks);
        }
        outports.add(new OutPort(portname, handler, cardinality, minSinks, maxSinks));
        addProperty(portname, OutPort.class);
    }
    
    public static DeployableComponentBuilder createBuilder(String name, ComponentId componentId) {
        return new DeployableComponentBuilder(name, componentId);
    }

    public void addLifecycleStore(LifecycleStore lifecycleStore) {
        store = lifecycleStore;
    }

    public void addProperty(String propertyName, Class<?> propertyType, Serializable defaultValue) {
        addProperty(propertyName, propertyType);
        propertyValues.put(propertyName, defaultValue);
    }
    
    public void addProperty(String propertyName, Class<?> propertyType) {
        properties.put(propertyName, propertyType);
    }
    
    public DeployableComponent build() {
        return new DeployableComponent(name, componentId, store, inports, outports, properties, propertyValues);
    }

    @SuppressWarnings("static-method")
	public void deploySequentially(@SuppressWarnings("unused") boolean b) {
        // deploySequentially = b;
        throw new UnsupportedOperationException("parallel deployment not supported yet.");
    }
}
