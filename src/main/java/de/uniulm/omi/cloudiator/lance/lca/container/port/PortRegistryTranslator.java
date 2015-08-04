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

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortReference;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;

public final class PortRegistryTranslator {

	private static String PORT_HIERARCHY_0_NAME = "PUBLIC";
	private static String PORT_HIERARCHY_1_NAME = "CLOUD";
	
	public static PortHierarchyLevel PORT_HIERARCHY_0 = PortHierarchyLevel.create(PORT_HIERARCHY_0_NAME);
	public static PortHierarchyLevel PORT_HIERARCHY_1 = PortHierarchyLevel.create(PORT_HIERARCHY_1_NAME);
	
	public static final Integer unsetPort = Integer.valueOf(-1);
	public static final String PORT_PREFIX = "ACCESS_";
	public static final String HOST_PREFIX = "HOST_";
	
	private static final String buildFullPortName(String portName){
		return PORT_PREFIX + portName;
	}
	
	public static final String buildFullPortName(String portName, PortHierarchyLevel level){
		return buildFullPortName(level.getName().toUpperCase() + "_" + portName);
	}
	
	public static final String buildFullHostName(PortHierarchyLevel level){
		return HOST_PREFIX + level.getName().toUpperCase() + "_IP";
	}
	
	public static boolean isValidPort(Integer i) {
		if(i == null) return false;
		final int j = i.intValue();
		if(j < 1 || j > 65535) return false;
		return true;
	}
	
	private final HostContext hostContext;
	private final GlobalRegistryAccessor accessor;
	
	public PortRegistryTranslator(GlobalRegistryAccessor _accessor, HostContext context) {
		accessor = _accessor;
		hostContext = context;
	}

	public void registerLocalPortAtLevel(String portName, PortHierarchyLevel level, Integer value) throws RegistrationException {
		String key = buildFullPortName(portName, level);
		accessor.addLocalProperty(key, value.toString());
	}
	
	public Integer findPortAtLevel(ComponentInstanceId myId, String portName, PortHierarchyLevel level) throws RegistrationException {
		String key = buildFullPortName(portName, level);
		try {
			String value = accessor.getComponentInstanceProperty(myId, key);
			Integer i = Integer.parseInt(value);
			if(isValidPortOrUnset(i)) return i;
		} catch(NumberFormatException nfe) {
			throw new RegistrationException("value was not an expected number", nfe);
		}
		return unsetPort;
	}

	public void shareHostAddresses(NetworkHandler portHandler) {
		portHandler.registerAddress(PORT_HIERARCHY_0, hostContext.getPublicIp());
		portHandler.registerAddress(PORT_HIERARCHY_1, hostContext.getInternalIp());
	}

	public void registerLocalAddressAtLevel(PortHierarchyLevel level, String value) throws RegistrationException {
		String key = buildFullHostName(level);
		accessor.addLocalProperty(key, value);
	}
	
	/* FIXME: this is bullshit: we cannot use the portHierarchy of the searching host, but have to use the one of the target 
	 * probably this information has to be added to the registry for retrieval;
	 * alternatively, define 3 levels of hierarchy throughout the applicatin */
	public Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> findDownstreamInstances(OutPort out, PortHierarchy portHierarchy) throws RegistrationException {
		PortReference sinkReference = null;		
		Object o = accessor.getLocalProperty(out.getName(), OutPort.class);
		try { sinkReference = (PortReference) o;}
		catch(ClassCastException cce) {
			throw new IllegalStateException("sink unknown: port '" + out.getName() + "' not correctly wired.");
		}
		
		Map<ComponentInstanceId, Map<String, String>> dump = accessor.retrieveComponentDump(sinkReference);
		return getHierarchicalPorts(sinkReference, dump, portHierarchy);
	}
	
	private static boolean isValidPortOrUnset(Integer i) {
		if(i == null) return false;
		if(isValidPort(i)) return true;
		if(i.intValue() == unsetPort) return true;
		return false;
	}
	
	private static Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> getHierarchicalPorts(PortReference sinkReference, Map<ComponentInstanceId, Map<String, String>> dump, PortHierarchy portHierarchy) throws RegistrationException {
		Map<ComponentInstanceId,HierarchyLevelState<DownstreamAddress>> addresses = new HashMap<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>>(); 
		for(ComponentInstanceId id : dump.keySet()) {
			HierarchyLevelState<DownstreamAddress> state = new HierarchyLevelState<DownstreamAddress>(id.toString(), portHierarchy);
			addresses.put(id, state);
			for(PortHierarchyLevel level : portHierarchy.levels()) {
				Map<String,String> map = dump.get(id);
				Integer i = getHierarchicalPort(sinkReference, map, level);
				String ip = getHierarchicalHostname(level, map);
				if(i == null || ip == null) continue;
				state.registerValueAtLevel(level, new DownstreamAddress(ip, i));
			}
		}
		return addresses;
	}
	
	private static Integer getHierarchicalPort(PortReference sinkReference, Map<String, String> dump, PortHierarchyLevel level) throws RegistrationException {
		String key = buildFullPortName(sinkReference.getPortName(), level);
		String value = dump.get(key);
		try {
			Integer i = Integer.parseInt(value);
			if(isValidPortOrUnset(i)) return i;
			throw new RegistrationException("received an unexpected result");
		} catch(NumberFormatException nfe) {
			throw new RegistrationException("value was not an expected number", nfe);
		}
	}
	
	private static String getHierarchicalHostname(PortHierarchyLevel level, Map<String, String> dump) throws RegistrationException {
		String key = buildFullHostName(level);
		String value = dump.get(key);
		if(value == null) throw new RegistrationException("ip address not found.");
		try { Inet4Address.getByName(value); }
		catch(UnknownHostException uhe) { throw new RegistrationException("illegal IP address", uhe);}
		return value;
	}
}
