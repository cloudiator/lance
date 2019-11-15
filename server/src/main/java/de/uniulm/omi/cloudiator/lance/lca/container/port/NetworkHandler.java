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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.DynamicEnvVars;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.DynamicEnvVarsImpl;

public final class NetworkHandler implements DynamicEnvVars {

	public static final String UNKNOWN_ADDRESS = "<unknown>";
	
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);
    private volatile ScheduledFuture<?> updateFuture = null;
    
    private final PortHierarchy portHierarchy;
    private final AbstractComponent myComponent;
    private final PortRegistryTranslator portAccessor;
    
    private final HierarchyLevelState<String> ipAddresses;
    private final HostContext hostContext;
    private final Map<String,HierarchyLevelState<Integer>> inPorts = new HashMap<>();
    
    private final OutPortHandler outPorts;
    private Set<DynamicEnvVarsImpl> currentEnvVarsDynamic;

    public NetworkHandler(GlobalRegistryAccessor accessorParam, AbstractComponent myComponentParam, HostContext hostContextParam) {
        
        portHierarchy = PortRegistryTranslator.PORT_HIERARCHY;
        myComponent = myComponentParam;
        hostContext = hostContextParam;
        portAccessor = new PortRegistryTranslator(accessorParam, hostContext);
        ipAddresses = new HierarchyLevelState<>("ip_address", portHierarchy);
        outPorts =  new OutPortHandler(myComponent);
        currentEnvVarsDynamic = new HashSet<>();
    }

  public PortRegistryTranslator getPortAccessor() {
    return portAccessor;
  }

  //todo: exception handling if wrong enum type
    public void injectDynamicEnvVars(DynamicEnvVarsImpl vars) throws ContainerException {
        this.currentEnvVarsDynamic.add(vars);
    }

    //todo: exception handling if wrong enum type
    public void removeDynamicEnvVars(DynamicEnvVars vars) throws ContainerException {
        this.currentEnvVarsDynamic.remove(vars);
    }

    public void initPorts(String address) throws RegistrationException {
    	String valueParam = (address == null ? UNKNOWN_ADDRESS : address); 
        portAccessor.shareHostAddresses(this);
        registerAddress(PortRegistryTranslator.PORT_HIERARCHY_2, valueParam);
        initInPorts();
        outPorts.initPortStates(portAccessor, portHierarchy);
        portAccessor.registerLocalAddressAtLevel(PortRegistryTranslator.PORT_HIERARCHY_2, valueParam);
    }

    private void initInPorts() {
        List<InPort> inports = myComponent.getExposedPorts();
        for(InPort in : inports) {
            final String name = in.getPortName();
            HierarchyLevelState<Integer> state = new HierarchyLevelState<>(name, portHierarchy);
            inPorts.put(name, state);
            for(PortHierarchyLevel level : state) { 
                state.registerValueAtLevel(level, PortRegistryTranslator.UNSET_PORT); 
            }
        }
    }

    public Map<Integer,Integer> findPortsToSet(DeploymentContext deploymentContext) {
        List<InPort> exposedPorts = myComponent.getExposedPorts();
        Map<Integer,Integer> portsToSet = new HashMap<>();
        for(InPort in : exposedPorts) {
            Integer portNumber = (Integer) deploymentContext.getProperty(in.getPortName(), InPort.class);
            if(portNumber == null) 
                throw new IllegalArgumentException("ports have to have a number");
            Integer value = in.isPublic() ? portNumber : PortRegistryTranslator.UNSET_PORT;
            Integer old = portsToSet.put(portNumber, value);
            if(old != null) 
                throw new IllegalArgumentException("same port number set twice");
        }
        return portsToSet;
    }
    
    void registerAddress(PortHierarchyLevel level, String address) {
        ipAddresses.registerValueAtLevel(level, address);
    }
    
    public void iterateOverInPorts(InportAccessor accessor) throws ContainerException {
         List<InPort> inPortsTmp = myComponent.getExposedPorts();
         for(InPort in : inPortsTmp) {
             String portName = in.getPortName();
             HierarchyLevelState<Integer> clientState = new HierarchyLevelState<>(portName, portHierarchy);
             accessor.accessPort(portName, clientState);
                                 
             HierarchyLevelState<Integer> state = inPorts.get(portName);
             if(state == null) 
                 throw new IllegalStateException("something went terribly wrong when initialising NetworkHandler");
             for(PortHierarchyLevel level : state) {
                 // the way the loop is constructed makes sure that  
                 // the client has set all needed levels 
                 state.registerValueAtLevel(level, clientState.valueAtLevel(level));
             }
         }
    }

    /*
    private void registerInPort(PortHierarchyLevel level, String portName, Integer portNumber) {
        HierarchyLevelState<Integer> state = inPorts.get(portName);
        if(state == null) { 
            throw new IllegalStateException("attempt to register an unknown port '" + portName + "': " + inPorts); 
        }
        state.registerValueAtLevel(level, portNumber);
    }*/

    public void publishLocalData(ComponentInstanceId myId) throws ContainerException {
        publishLocalAddresses(myId, portAccessor);
        publishLocalPorts(myId, portAccessor);
    }
    

    /** this method loops until information from all required external
     * connection is available (e.g. an application server may require 
     * that the database is up and running). */
    public void pollForNeededConnections() {
        DownstreamPortUpdater.pollForNeededConnections(outPorts, portAccessor, portHierarchy);
    }
    
    private void publishLocalAddresses(ComponentInstanceId myId, PortRegistryTranslator registryAccessor) throws ContainerException {
        List<String> failed = new LinkedList<>();
        
        for(PortHierarchyLevel level : ipAddresses) {
            try { 
                registryAccessor.registerLocalAddressAtLevel(level, ipAddresses.valueAtLevel(level)); 
            } catch(RegistrationException de) {
                LOGGER.info("problem when accessing registry", de); 
                failed.add(de.getLocalizedMessage());
            }        
        }
        
        if(failed.isEmpty()) {
            return;
        }
        throw new ContainerException("could register all ports: " + myId + "[" + failed.toString() + "]");
    }
    
    private void publishLocalPorts(ComponentInstanceId myId, PortRegistryTranslator registryAccessor) throws ContainerException {
        List<String> failed = new LinkedList<>();
        
        for(Map.Entry<String, HierarchyLevelState<Integer>> entry : inPorts.entrySet()) {
            HierarchyLevelState<Integer> state = entry.getValue();
            for(PortHierarchyLevel level : state) {
                try { 
                    registryAccessor.registerLocalPortAtLevel(entry.getKey(), level, state.valueAtLevel(level)); 
                } catch(RegistrationException de) {
                    LOGGER.info("problem when accessing registry", de); 
                    failed.add(de.getLocalizedMessage());
                }        
            }
        }
        if(failed.isEmpty()) {
            return;
        }
        throw new ContainerException("could register all ports: " + myId + "[" + failed.toString() + "]");
    }

    public void startPortUpdaters(LifecycleController controller) {
        DownstreamPortUpdater updater = new DownstreamPortUpdater(outPorts, portAccessor, portHierarchy, controller);
        ScheduledFuture<?> sf = hostContext.scheduleAction(updater);
        updateFuture = sf;
    }
    
    public void stopPortUpdaters() {
        ScheduledFuture<?> sf = updateFuture;
        if(sf == null) {
            LOGGER.warn("updateFuture has not been set.");
        } else {
            sf.cancel(false);
        }
    }

    public void accept(NetworkVisitor visitor) {
        for (Iterator<DynamicEnvVarsImpl> it = currentEnvVarsDynamic.iterator(); it.hasNext(); ) {
            DynamicEnvVarsImpl impl = it.next();
            if (impl.equals(DynamicEnvVarsImpl.NETWORK_ADDR)) {
                for(Entry<String, String> entry : impl.getEnvVars().entrySet()) {
                  visitor.visitNetworkAddress(entry.getKey(), entry.getValue());
              }
            }
            else if (impl.equals(DynamicEnvVarsImpl.NETWORK_PORT)) {
                for(Entry<String, String> entry : impl.getEnvVars().entrySet()) {
                    visitor.visitInPort(entry.getKey(), entry.getValue());
                }
            }
            else
                LOGGER.warn("Variables of type: " + impl + "not known.");
        }

        outPorts.accept(visitor);
    }

    public void updateAddress(PortHierarchyLevel level2Param, String containerIp) {
        registerAddress(level2Param, containerIp);
    }

    private static Map<String,String> buildSingleElementMap(String key, String value) {
        Map<String,String> map = new HashMap<>();
        map.put(key,value);

        return map;
    }

    //OutPortHandler vars included
    //copies and not the originals of currentEnvVarsDynamic of NWHandler and outports are returned
    @Override
    public Map<String, String> getEnvVars() {
        Map<String,String> completeDynamicEnvVars = new HashMap<>();
        for(DynamicEnvVars vars: currentEnvVarsDynamic)
            completeDynamicEnvVars.putAll(vars.getEnvVars());

        Map<String,String> outPDynVars = new HashMap<>(outPorts.getEnvVars());
        completeDynamicEnvVars.putAll(outPDynVars);
        return completeDynamicEnvVars;
    }

    @Override
    public void generateDynamicEnvVars() {
       generateDynamicEnvVars(null);
    }

    public void generateDynamicEnvVars(PortDiff<DownstreamAddress> diff) {
        for(PortHierarchyLevel level : ipAddresses) {
            String name = level.getName().toUpperCase() + "_IP";
            String address = ipAddresses.valueAtLevel(level);
            DynamicEnvVarsImpl addrVar = DynamicEnvVarsImpl.NETWORK_ADDR;
            addrVar.setEnvVars(buildSingleElementMap(name,address));
            try {
              injectDynamicEnvVars(addrVar);
            } catch (ContainerException ex) {
                LOGGER.error("Cannot inject variable " + addrVar + "as it has a wrong type");
            }
        }
        for(Entry<String, HierarchyLevelState<Integer>> entry : inPorts.entrySet()) {
            String portName = entry.getKey();
            HierarchyLevelState<Integer> state = entry.getValue();
            for(PortHierarchyLevel level : state) {
                String fullPortName = level.getName().toUpperCase() + "_" + portName.toUpperCase();
                String portNumber = state.valueAtLevel(level).toString();
                DynamicEnvVarsImpl portVar = DynamicEnvVarsImpl.NETWORK_PORT;
                portVar.setEnvVars(buildSingleElementMap(fullPortName,portNumber));
                try {
                  injectDynamicEnvVars(portVar);
                } catch (ContainerException ex) {
                    LOGGER.error("Cannot inject variable " + portVar + "as it has a wrong type");
                }
            }
        }

        outPorts.generateDynamicEnvVars(diff);
    }
}
