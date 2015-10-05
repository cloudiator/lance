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

package de.uniulm.omi.cloudiator.lance.lca;

import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.*;

import java.util.Map;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.PortReference;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;

public final class GlobalRegistryAccessor {
    
    private final LcaRegistry reg;
    // private final ApplicationId appId;
    private final ApplicationInstanceId appInstId; 
    private final ComponentId compId;
    private final DeploymentContext ctx;
    // private final DeployableComponent comp;
    private final ComponentInstanceId localId;
    
    public GlobalRegistryAccessor(DeploymentContext ctxParam, DeployableComponent compParam, ComponentInstanceId localIdParam) {
        reg = ctxParam.getRegistry();
        // appId = _ctx.getApplicationId();
        appInstId = ctxParam.getApplicationInstanceId();
        compId = compParam.getComponentId();
        ctx = ctxParam;
        // comp = _comp;
        localId = localIdParam;
    }
    
    public final void init(ComponentInstanceId myId) throws RegistrationException {
        reg.addComponentInstance(appInstId, compId, myId);
        reg.addComponentProperty(appInstId, compId, myId, COMPONENT_INSTANCE_STATUS, LifecycleHandlerType.NEW.toString());
    }
    
    public final void updateInstanceState(ComponentInstanceId myId, LifecycleHandlerType type) throws RegistrationException {
        reg.addComponentProperty(appInstId, compId, myId, COMPONENT_INSTANCE_STATUS, type.toString());
    }
    
    public final void updateContainerState(ComponentInstanceId myId, ContainerStatus type) throws RegistrationException {
        reg.addComponentProperty(appInstId, compId, myId, CONTAINER_STATUS, type.toString());
    }
    
    /* 
    @Deprecated
    public final String getProperty(ComponentInstanceId myId, String name) throws RegistrationException {
        try {
            String ret = reg.getComponentProperty(appInstId, compId, myId, name);
            return ret;
        } catch(RemoteException re) {
            throw new RegistrationException("cannot get registered entity: " + name);
        }
    }
    @Deprecated
    public void setProperty(ComponentInstanceId myId, String name, String value) throws RegistrationException {
        try {
            reg.addComponentProperty(appInstId, compId, myId, name, value);
        } catch(RemoteException re) {
            throw new RegistrationException("cannot register entity: " + name);
        }
    }
    @Deprecated
    private void addPort(ComponentInstanceId myId, String name, Integer port) throws RemoteException {
        reg.addComponentProperty(appInstId, compId, myId, "ACCESS_" + name, port.toString());
    } 
    
    @Deprecated
    public String getSinkPortNameAsFullPortName(OutPort the_port) {
        final PortReference sinkReference = (PortReference) ctx.getProperty(the_port.getName(), OutPort.class);
        if(sinkReference == null) throw new IllegalStateException("sink unknown");
        return sinkReference.getPortName();
    }
    @Deprecated
    public void registerPorts(ComponentInstanceId myId, Map<String, Integer> mapping) throws RegistrationException {
        try {
            for(String key : mapping.keySet()) {
                Integer i = mapping.get(key);
                addPort(myId, key, i);
            }
        } catch(RemoteException re) {
            throw new RegistrationException(re);
        }
    }
    @Deprecated
    public Map<ComponentInstanceId, Map<String, String>> findAvailablePortInstances(OutPort the_port) throws RegistrationException {
        final PortReference sinkReference = (PortReference) ctx.getProperty(the_port.getName(), OutPort.class);
        if(sinkReference == null) throw new IllegalStateException("sink unknown: port '" + the_port.getName() + "' not correctly wired.");
        
        Map<ComponentInstanceId, Map<String, String>> dump;
        try { dump = reg.dumpComponent(appInstId, sinkReference.getComponentId()); }
        catch(RemoteException re) { throw new RegistrationException(re); }

        final String fullPortName = buildFullPortName(sinkReference.getPortName());

        for(ComponentInstanceId id : new HashSet<ComponentInstanceId>(dump.keySet())) {
            Map<String,String> values = dump.get(id);
            String portValue = values.get(fullPortName);
            int portNr;
            try { portNr = Integer.parseInt(portValue); }
            catch(NumberFormatException nfe) {portNr = -1;}
            if(portNr == -1) { dump.remove(id); }
        }
        
        return dump;
    }
    
    
    @Deprecated
    public void schedulePortPolling(Runnable runner) {
        hostContext.scheduleAction(runner);
    }
    @Deprecated
    public void run(Runnable runner) {
        hostContext.run(runner);
    }
    
    /*
    @Deprecated
    public static String determineIpaddress(String this_cloudId, Map<String, String> map) {
        String that_cloudId = map.get(CLOUD_PROVIDER_ID);
        if(that_cloudId == null) {
            System.err.println("cloudId not set for remote host");
        } else if(this_cloudId == null) {
            System.err.println("own cloudId not set for");
            throw new IllegalStateException("not set");
        }
        
        if(this_cloudId.equals(that_cloudId)) {
            String ip = map.get(HOST_INTERNAL_IP);
            if(ip != null) { return ip; }
            System.err.println("local ip address not set for remote host");
        } else {
            String ip = map.get(HOST_PUBLIC_IP);
            if(ip != null) { return ip; }
            System.err.println("public ip address not set for remote host"); 
        }
        return null;
    }
    
    @Deprecated
    public static Integer readPortProperty(String portName, Map<String, String> map) {
        String full = buildFullPortName(portName);
        String portVal = map.get(full);
        try {
            int i = Integer.parseInt(portVal);
            if(i < 1 || i > 65536) return null;
            return Integer.valueOf(i);
        } catch(NumberFormatException nfe) {
            return null;
        }
    }
    
    @Deprecated
    public String getLocalCloudProdivder() {
         return hostContext.getCloudIdentifier();
    }
    */
    
    public Map<ComponentInstanceId, Map<String, String>> retrieveComponentDump(PortReference sinkReference) throws RegistrationException {
        return reg.dumpComponent(appInstId, sinkReference.getComponentId());
    }
    
    public void addLocalProperty(String key, String value) throws RegistrationException {
        reg.addComponentProperty(appInstId, compId, localId, key, value);
    }

    public String getComponentInstanceProperty(ComponentInstanceId myId, String key) throws RegistrationException {
        return reg.getComponentProperty(appInstId, compId, myId, key);
    }

    public<T> Object getLocalProperty(String name, Class<T> clazz) throws RegistrationException {
        Object o = null;
        try { 
            o = ctx.getProperty(name, clazz); 
        } catch(Exception ex){
            throw new RegistrationException("invalid property", ex);
        }
        if(o == null) 
            throw new RegistrationException("unknown property");
        return o;
    }
}
