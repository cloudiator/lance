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

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

public final class RemoteRegistryImpl implements RmiLcaRegistry {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(LcaRegistry.class);
    private final Map<ApplicationInstanceId,AppInstanceContainer> apps = new HashMap<>();
    
    @Override
    public synchronized Map<ComponentInstanceId, Map<String, String>> dumpComponent(ApplicationInstanceId instId, ComponentId compId) throws RemoteException {
        AppInstanceContainer c = apps.get(instId);
        if(c == null) 
            return Collections.emptyMap();
        
        return c.dumpAll(compId);
    }
    
    @Override
    /**
     * @return true if this application instance has been added successfully. false if it was already contained
             in the registry.
     */
    public synchronized boolean addApplicationInstance(ApplicationInstanceId instId, ApplicationId appId, String name) throws RemoteException {
        if(apps.containsKey(instId)) {
            LOGGER.info("application instance '" + instId + "' already exists.");
            return false;
        }
        apps.put(instId, new AppInstanceContainer(instId, appId, name));
        return true;
    }
    
    @Override
    public synchronized void addComponent(ApplicationInstanceId instId, ComponentId cid, String name)  throws RemoteException {
        AppInstanceContainer c = apps.get(instId);
        if(c == null) 
            throw new IllegalArgumentException("not known: " + instId);
        c.addComponent(cid, name);
    }

    @Override
    public synchronized void addComponentInstance(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId) 
                throws RemoteException {
        AppInstanceContainer c = apps.get(instId);
        if(c == null) 
            throw new IllegalArgumentException("not known: " + instId);
        c.addComponentInstance(cid, cinstId);
    }

    @Override
    public synchronized void addComponentProperty(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId, String property, Object value) throws RemoteException {
        AppInstanceContainer c = apps.get(instId);
        if(c == null) 
            throw new IllegalArgumentException("not known: " + instId);
        c.addComponentProperty(cid, cinstId, property, value);
    }
    
    @Override
    public String getComponentProperty(ApplicationInstanceId appInstId,
            ComponentId compId, ComponentInstanceId myId, String name) {
        
        AppInstanceContainer c = apps.get(appInstId);
        if(c == null) 
            throw new IllegalArgumentException("not known: " + appInstId);
        return c.getComponentProperty(compId, myId, name);
    }

    @Override
    public boolean applicationInstanceExists(ApplicationInstanceId appInstId) throws RemoteException {
        AppInstanceContainer c = apps.get(appInstId);
        return c != null;
    }

    @Override
    public boolean applicationComponentExists(ApplicationInstanceId appInstId, ComponentId compId) throws RemoteException {
        AppInstanceContainer c = apps.get(appInstId);
        return c != null && c.componentExists(compId);
    }

}
