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
import java.util.concurrent.atomic.AtomicInteger;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

public final class RemoteRegistryImpl implements RmiLcaRegistry {

	private final Map<ApplicationInstanceId,AppInstanceContainer> apps = new HashMap<ApplicationInstanceId,AppInstanceContainer>();
	
	@Override
	public synchronized Map<ComponentInstanceId, Map<String, String>> dumpComponent(ApplicationInstanceId instId, ComponentId compId) throws RemoteException {
		AppInstanceContainer c = apps.get(instId);
		if(c == null) return Collections.emptyMap();
		
		return c.dumpAll(compId);
	}
	
	@Override
	public synchronized void addApplicationInstance(ApplicationInstanceId instId, ApplicationId appId, String name) throws RemoteException {
		if(apps.containsKey(instId)) throw new IllegalArgumentException("alread exists: " + instId);
		apps.put(instId, new AppInstanceContainer(instId, appId, name));
	}
	
	@Override
	public synchronized void addComponent(ApplicationInstanceId instId, ComponentId cid, String name)  throws RemoteException {
		AppInstanceContainer c = apps.get(instId);
		if(c == null) throw new IllegalArgumentException("not known: " + instId);
		c.addComponent(cid, name);
	}

	@Override
	public synchronized void addComponentInstance(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId) 
				throws RemoteException {
		AppInstanceContainer c = apps.get(instId);
		if(c == null) throw new IllegalArgumentException("not known: " + instId);
		c.addComponentInstance(cid, cinstId);
	}

	@Override
	public synchronized void addComponentProperty(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId, String property, Object value) throws RemoteException {
		AppInstanceContainer c = apps.get(instId);
		if(c == null) throw new IllegalArgumentException("not known: " + instId);
		c.addComponentProperty(cid, cinstId, property, value);
	}
	
	@Override
	public String getComponentProperty(ApplicationInstanceId appInstId,
			ComponentId compId, ComponentInstanceId myId, String name) {
		
		AppInstanceContainer c = apps.get(appInstId);
		if(c == null) throw new IllegalArgumentException("not known: " + appInstId);
		return c.getComponentProperty(compId, myId, name);
	}

	static final class AppInstanceContainer {

		private final ApplicationId appId;
		private final ApplicationInstanceId appInstId;
		private final Map<ComponentId,ComponentInstanceContainer> comps = new HashMap<ComponentId,ComponentInstanceContainer>();
		
		public AppInstanceContainer(ApplicationInstanceId instId, ApplicationId _appId, String name) { appId = _appId; appInstId = instId; }

		public String getComponentProperty(ComponentId compId, ComponentInstanceId myId, String name) {
			ComponentInstanceContainer c = comps.get(compId);
			if(c == null) throw new IllegalArgumentException("not known: " + compId);
			return c.getComponentProperty(myId, name);
		}

		public Map<ComponentInstanceId, Map<String, String>> dumpAll(ComponentId compId) {
			ComponentInstanceContainer c = comps.get(compId);
			if(c == null) return Collections.emptyMap();
			
			return c.dumpInstances();
		}

		public void addComponentProperty(ComponentId cid, ComponentInstanceId cinstId, String property, Object value) {
			ComponentInstanceContainer c = comps.get(cid);
			if(c == null) throw new IllegalArgumentException("not known: " + cid);
			c.addComponentProperty(cinstId, property, value);
		}

		public void addComponent(ComponentId cid, String name) {
			if(comps.containsKey(cid)) throw new IllegalArgumentException("alread exists: " + cid);
			comps.put(cid, new ComponentInstanceContainer(this, cid, name));
		}

		public void addComponentInstance(ComponentId cid, ComponentInstanceId cinstId) {
			ComponentInstanceContainer c = comps.get(cid);
			if(c == null) throw new IllegalArgumentException("not known: " + cid);
			c.addComponentInstance(cinstId);
		}
		
		@Override
		public String toString() {
			return appInstId.toString();
		}
	}
	
	static final class ComponentInstanceContainer {

		private final AtomicInteger counter = new AtomicInteger(0);
		private final AppInstanceContainer myContainer;
		private final ComponentId cid;
		private final Map<ComponentInstanceId, Map<String,Object>> instances = new HashMap<ComponentInstanceId, Map<String,Object>>();
		
		public ComponentInstanceContainer(AppInstanceContainer cnt, ComponentId _cid, String name) { myContainer = cnt; cid = _cid; }
		
		public String getComponentProperty(ComponentInstanceId myId, String name) {
			Map<String,Object> props = instances.get(myId);
			if(props == null) throw new IllegalArgumentException("not known: " + myId);
			Object old = props.get(name);
			return old == null ? null : old.toString();
		}

		public Map<ComponentInstanceId, Map<String,String>> dumpInstances() {
			Map<ComponentInstanceId, Map<String,String>> copy = new HashMap<ComponentInstanceId, Map<String,String>>();
			for(ComponentInstanceId id : instances.keySet()) {
				Map<String,Object> content = instances.get(id);
				Map<String,String> inner_copy = new HashMap<String, String>();
				for(String inner_key : content.keySet()) {
					Object value = content.get(inner_key);
					inner_copy.put(inner_key, value.toString());
				}
				copy.put(id, inner_copy);
			}
			return copy;
		}

		public void addComponentProperty(ComponentInstanceId cinstId, String property, Object value) {
			Map<String,Object> props = instances.get(cinstId);
			if(props == null) throw new IllegalArgumentException("not known: " + cinstId);
			Object old = props.put(property, value);
			if(old != null) System.err.println("warning: overriding value!");
			//FIXME: wake up listeners (?)
			System.err.println("TODO: wake up listeners");
			
			System.err.println("LcaRegistry: added property: " + this + "/" + cinstId + "." + property + "=" + value);
		}

		public void addComponentInstance(ComponentInstanceId cinstId) {
			if(instances.containsKey(cinstId)) throw new IllegalArgumentException("alread exists: " + cinstId);
			Map<String,Object> map = new HashMap<String,Object>();
			map.put(LcaRegistryConstants.INSTANCE_NR, counter.incrementAndGet());
			instances.put(cinstId, map);
			
			System.err.println("LcaRegistry: added component instance: " + this + "/" + cinstId);
		}
		
		@Override
		public String toString() {
			return myContainer + "/" + cid;
		}
	}

}
