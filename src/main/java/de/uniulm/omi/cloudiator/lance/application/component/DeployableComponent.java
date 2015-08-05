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

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.PropertyVisitor;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public final class DeployableComponent implements Serializable {

	private static final long serialVersionUID = 5544457717632275252L;
	private final String name;
	private final ComponentId myId;
	private final LifecycleStore lifecycle;
	private final List<InPort> in_ports;
	private final List<OutPort> out_ports;
	private final HashMap<String, Class<?>> properties;
	private final HashMap<String, ? extends Serializable> defaultValues;

	DeployableComponent(String _name, ComponentId _id, LifecycleStore _lifecycleStore, List<InPort> _in_ports,
			List<OutPort> _out_ports, Map<String, Class<?>> _ingoing_properties, 
			HashMap<String, ? extends Serializable> _propertyValues) {
		name = _name;
		myId = _id;
		lifecycle = _lifecycleStore;
		in_ports = new ArrayList<InPort>(_in_ports);
		out_ports = new ArrayList<OutPort>(_out_ports);
		properties = new HashMap<String, Class<?>>(_ingoing_properties);
		defaultValues = _propertyValues;
	}

	public LifecycleStore getLifecycleStore() {
		return lifecycle;
	}

	public ComponentId getComponentId() { return myId; }

	public List<InPort> getExposedPorts() {
		List<InPort> ports = new ArrayList<InPort>(in_ports.size());
		for(InPort i : in_ports) {
			ports.add(i);
		}
		return ports;
	}
	
	@Override
	public String toString() {
		return name + ": -> " + in_ports + "@" + myId; 
	}

	public List<OutPort> getDownstreamPorts() {
		return new ArrayList<OutPort>(out_ports); 
	}
	
	public void accept(DeploymentContext ctx, PropertyVisitor visitor) {		
		for(Entry<String, Class<?>> entry : properties.entrySet()) {
			String propertyName = entry.getKey();
			Class<?> type = entry.getValue();
			if(type == OutPort.class) continue;
			Object o = ctx.getProperty(propertyName, type);
			if(o == null) o = defaultValues.get(propertyName);
			if(o == null) {
				System.err.println("Warning: propery '" + propertyName + "' has not been defined for the application");
				continue;
			}
			visitor.visit(propertyName, o.toString());
		}
	}
}
