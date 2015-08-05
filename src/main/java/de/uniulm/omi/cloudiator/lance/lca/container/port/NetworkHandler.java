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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.PortUpdateCallback;

public final class NetworkHandler implements PortUpdateCallback {

	private final PortHierarchy portHierarchy;
	private final DeployableComponent myComponent;
	private final PortRegistryTranslator portAccessor;
	
	private final HierarchyLevelState<String> ipAddresses;
	private final Map<String,HierarchyLevelState<Integer>> inPorts = new HashMap<String,HierarchyLevelState<Integer>>();
	
	private final OutPortHandler outPorts;
	
	public NetworkHandler(PortRegistryTranslator _portAccessor, PortHierarchy _portHierarchy,  DeployableComponent _myComponent) {
		portHierarchy = _portHierarchy;
		myComponent = _myComponent;
		portAccessor = _portAccessor;
		ipAddresses = new HierarchyLevelState<String>("ip_address", portHierarchy);
		outPorts =  new OutPortHandler(myComponent);
	}

	public void initPorts(PortHierarchyLevel hierarchy_level_2, String value) throws RegistrationException {
		portAccessor.shareHostAddresses(this);
		registerAddress(hierarchy_level_2, value);
		initInPorts();
		outPorts.initPortStates(portAccessor, portHierarchy);
		portAccessor.registerLocalAddressAtLevel(hierarchy_level_2, value);
	}

	private void initInPorts() {
		List<InPort> inports = myComponent.getExposedPorts();
		for(InPort in : inports) {
			final String name = in.getPortName();
			HierarchyLevelState<Integer> state = new HierarchyLevelState<Integer>(name, portHierarchy);
			inPorts.put(name, state);
			for(PortHierarchyLevel level : state) { state.registerValueAtLevel(level, PortRegistryTranslator.UNSET_PORT); }
		}
	}

	public Map<Integer,Integer> findPortsToSet(DeploymentContext deploymentContext) {
		List<InPort> exposedPorts = myComponent.getExposedPorts();
		Map<Integer,Integer> portsToSet = new HashMap<Integer, Integer>();
		for(InPort in : exposedPorts) {
			Integer portNumber = (Integer) deploymentContext.getProperty(in.getPortName(), InPort.class);
			if(portNumber == null) throw new IllegalArgumentException("ports have to have a number");
			Integer value = in.isPublic() ? portNumber : PortRegistryTranslator.UNSET_PORT;
			Integer old = portsToSet.put(portNumber, value);
			if(old != null) throw new IllegalArgumentException("same port number set twice");
		}
		return portsToSet;
	}
	
	void registerAddress(PortHierarchyLevel level, String address) {
		ipAddresses.registerValueAtLevel(level, address);
	}

	public void registerInPort(PortHierarchyLevel level, String portName, Integer portNumber) {
		HierarchyLevelState<Integer> state = inPorts.get(portName);
		if(state == null) { throw new IllegalStateException("attempt to register an unknown port '" + portName + "': " + inPorts); }
		state.registerValueAtLevel(level, portNumber);
	}

	public void publishLocalData(ComponentInstanceId myId) throws ContainerException {
		publishLocalAddresses(myId, portAccessor);
		publishLocalPorts(myId, portAccessor);
	}
	

	/** this method loops until information from all required external
	 * connection is available (e.g. an application server may require 
	 * that the database is up and running). */
	public void pollForNeededConnections() {
		while(true) {
			try { 
				outPorts.updateDownstreamPorts(portAccessor, portHierarchy);
				if(outPorts.requiredDownstreamPortsSet()) return;
			}
			catch (RegistrationException e) {
				e.printStackTrace();
			}
			System.err.println("did not find initial values for all required out ports; sleeping for some time... ");
			try { Thread.sleep(3000); } 
			catch(InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		// throw new IllegalStateException();
	}
	
	private void publishLocalAddresses(ComponentInstanceId myId, PortRegistryTranslator registryAccessor) throws ContainerException {
		List<String> failed = new LinkedList<String>();
		
		for(PortHierarchyLevel level : ipAddresses) {
			try { registryAccessor.registerLocalAddressAtLevel(level, ipAddresses.valueAtLevel(level)); } 
			catch(RegistrationException de) {
				de.printStackTrace(); 
				failed.add(de.getLocalizedMessage());
			}		
		}
		
		if(failed.isEmpty()) return;
		throw new ContainerException("could register all ports: " + myId + "[" + failed.toString() + "]");
	}
	
	private void publishLocalPorts(ComponentInstanceId myId, PortRegistryTranslator registryAccessor) throws ContainerException {
		List<String> failed = new LinkedList<String>();
		
		for(Map.Entry<String, HierarchyLevelState<Integer>> entry : inPorts.entrySet()) {
			HierarchyLevelState<Integer> state = entry.getValue();
			for(PortHierarchyLevel level : state) {
				try { registryAccessor.registerLocalPortAtLevel(entry.getKey(), level, state.valueAtLevel(level)); } 
				catch(RegistrationException de) {
					de.printStackTrace(); 
					failed.add(de.getLocalizedMessage());
				}		
			}
		}
		if(failed.isEmpty()) return;
		throw new ContainerException("could register all ports: " + myId + "[" + failed.toString() + "]");
	}

	@Override
	public void handleUpdate(OutPort port, PortDiff<?> diff) {
	/*	//FIXME: ensure that we are in running state 
		// if(!controller.isRunning()) return;
		
		// updating is rather easy. step 1: we get the update handler 
		// for this port from the deployable component and then either
		// do nothing or restart the application 
		try {
			DockerShell dshell = client.getSideShell(myId);
			flushEnvironmentVariables(dshell);
			flushSinglePort(dshell, port, diff.getCurrentSinkSet());
			shellFactory.installDockerShell(dshell);
			PortUpdateHandler handler = port.getUpdateHandler();
			controller.blockingUpdatePorts(handler);
		} catch(DockerException de) {
			de.printStackTrace();
		} catch (RegistrationException e) {
			e.printStackTrace();
		} finally {
			shellFactory.closeShell();
		}
		//FIXME: only *now* update the set in the OutPortState
		System.out.println("update the set in the OutPortState => ..."); //.printStackTrace();
		*/
	}

	public void startPortUpdaters() {
		outPorts.startPortUpdaters();
	}

	public void accept(NetworkVisitor visitor) {
		for(PortHierarchyLevel level : ipAddresses) {
			visitor.visitNetworkAddress(level, ipAddresses.valueAtLevel(level));
		}
		
		for(Entry<String, HierarchyLevelState<Integer>> entry : inPorts.entrySet()) {
			String portName = entry.getKey();
			HierarchyLevelState<Integer> state = entry.getValue();
			for(PortHierarchyLevel level : state) {
				visitor.visitInPort(portName, level, state.valueAtLevel(level));
			}	
		}
		
		outPorts.accept(visitor);
	}

	public void updateAddress(PortHierarchyLevel hierarchy_level_2, String containerIp) {
		registerAddress(hierarchy_level_2, containerIp);
	}
}
