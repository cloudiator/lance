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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortReference;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortHierarchy.PortHierarchyBuilder;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;

public final class PortRegistryTranslator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PortRegistryTranslator.class);

    private static final String PORT_HIERARCHY_0_NAME = "PUBLIC";
    private static final String PORT_HIERARCHY_1_NAME = "CLOUD";
    private static final String PORT_HIERARCHY_2_NAME = "CONTAINER";
        
    public static final PortHierarchyLevel PORT_HIERARCHY_0 = PortHierarchyLevel.create(PORT_HIERARCHY_0_NAME);
    public static final PortHierarchyLevel PORT_HIERARCHY_1 = PortHierarchyLevel.create(PORT_HIERARCHY_1_NAME);
    public static final PortHierarchyLevel PORT_HIERARCHY_2 = PortHierarchyLevel.create(PORT_HIERARCHY_2_NAME);
    
    public static final PortHierarchy PORT_HIERARCHY = new PortHierarchyBuilder().addLevel(PortRegistryTranslator.PORT_HIERARCHY_0).
            addLevel(PortRegistryTranslator.PORT_HIERARCHY_1).addLevel(PORT_HIERARCHY_2).build();

    
    public static final Integer UNSET_PORT = Integer.valueOf(-1);
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
        if(i == null) 
            return false;
        final int j = i.intValue();
        if(j < 1 || j > 65535) 
            return false;
        return true;
    }
    
    private final HostContext hostContext;
    private final GlobalRegistryAccessor accessor;
    
    public PortRegistryTranslator(GlobalRegistryAccessor accessorParam, HostContext context) {
        accessor = accessorParam;
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
            Integer i = Integer.valueOf(value);
            if(isValidPortOrUnset(i)) {
                return i;
            }
        } catch(NumberFormatException nfe) {
            throw new RegistrationException("value was not an expected number", nfe);
        }
        return UNSET_PORT;
    }

    public void shareHostAddresses(NetworkHandler portHandler) {
        portHandler.registerAddress(PORT_HIERARCHY_0, hostContext.getPublicIp());
        portHandler.registerAddress(PORT_HIERARCHY_1, hostContext.getInternalIp());
    }

    public void registerLocalAddressAtLevel(PortHierarchyLevel level, String value) throws RegistrationException {
        String key = buildFullHostName(level);
        accessor.addLocalProperty(key, value);
    }
    
    /* define 3 levels of hierarchy throughout the applicatin */
    public Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> findDownstreamInstances(OutPort out, PortHierarchy portHierarchy) throws RegistrationException {
        PortReference sinkReference = null;        
        Object o = accessor.getLocalProperty(out.getName(), OutPort.class);
        try { 
            sinkReference = (PortReference) o;
        } catch(ClassCastException cce) {
            throw new IllegalStateException("sink unknown: port '" + out.getName() + "' not correctly wired.", cce);
        }
        
        Map<ComponentInstanceId, Map<String, String>> dump = accessor.retrieveComponentDump(sinkReference);
        return getHierarchicalPorts(sinkReference, dump, portHierarchy);
    }
    
    private static boolean isValidPortOrUnset(Integer i) {
        if(i == null) 
            return false;
        if(isValidPort(i)) 
            return true;
        return i.intValue() == UNSET_PORT.intValue();
    }
    
    private static Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> getHierarchicalPorts(PortReference sinkReference, Map<ComponentInstanceId, Map<String, String>> dump, PortHierarchy portHierarchy) throws RegistrationException {
        Map<ComponentInstanceId,HierarchyLevelState<DownstreamAddress>> addresses = new HashMap<>(); 
        for(Entry<ComponentInstanceId, Map<String, String>> entry : dump.entrySet()) {
            ComponentInstanceId id = entry.getKey();
            Map<String,String> map = entry.getValue();
            boolean isReady = GlobalRegistryAccessor.dumpMapHasContainerStatus(map, ContainerStatus.READY);
            if(!isReady) {
            	LOGGER.info("dropping data (ports and ips of component instance " + id + " as it is not in ready state");
            	continue;
            }
            HierarchyLevelState<DownstreamAddress> state = new HierarchyLevelState<>(id.toString(), portHierarchy);
            boolean forAll = true;
            for(PortHierarchyLevel level : portHierarchy.levels()) {
                Integer i = getHierarchicalPort(sinkReference, map, level);
                String ip = getHierarchicalHostname(level, map);
                if(i == null || ip == null) {
                	forAll = false;
                    continue;
                }
                state.registerValueAtLevel(level, new DownstreamAddress(ip, i));
            }
            if(forAll) { // only pass on when we found sth for all levels.
            	addresses.put(id, state);
            } else {
            	// drop values to avoid inconsistencies
            }
        }
        return addresses;
    }

	private static Integer getHierarchicalPort(PortReference sinkReference, Map<String, String> dump, PortHierarchyLevel level) throws RegistrationException {
        String key = buildFullPortName(sinkReference.getPortName(), level);
        String value = dump.get(key);
        try {
        	if(value == null) {
        		// we only check when other component is in state "READY"
        		// hence, port has to be set.
        		throw new RegistrationException("port with '" + key + "' has not been found. Value was null.");
        	}
            Integer i = Integer.valueOf(value);
            if(isValidPortOrUnset(i)) {
                return i;
            }
            throw new RegistrationException("(" + key + ") " + " received an unexpected result");
        } catch(NumberFormatException nfe) {
            throw new RegistrationException("(" + key + ") " + " value was not an expected number: " + value, nfe);
        }
    }
    
    private static String getHierarchicalHostname(PortHierarchyLevel level, Map<String, String> dump) throws RegistrationException {
        String key = buildFullHostName(level);
        String value = dump.get(key);
        if(value == null) {
        	throw new RegistrationException("ipaddress for '" + key + "' has not been found. Value was null.");
        }
        if(NetworkHandler.UNKNOWN_ADDRESS.equals(value.trim())) {
        	return null;
        }
        try { 
            InetAddress.getByName(value); 
        } catch(UnknownHostException uhe) { 
            throw new RegistrationException("illegal IP address: " + value, uhe);
        } return value;
    }
}
