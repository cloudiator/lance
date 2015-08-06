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

import java.io.Serializable; 
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;

/** interface to the global registry where 
 * properties are being stored; encapsulates 
 * the actual implementation */
public interface LcaRegistry extends Serializable {

	/**
	 * @param instId
	 * @param appId
	 * @param name
	 * @return @return true if this application instance has been added successfully. false if it was already contained
	 * in the registry.
	 * @throws RegistrationException
	 */
    public boolean addApplicationInstance(ApplicationInstanceId instId, ApplicationId appId, String name) throws RegistrationException;
    public void addComponent(ApplicationInstanceId instId, ComponentId cid, String name) throws RegistrationException;
    public void addComponentInstance(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId) throws RegistrationException;
    void addComponentProperty(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId, String property, Object value) throws RegistrationException;
    public Map<ComponentInstanceId, Map<String, String>> dumpComponent(ApplicationInstanceId instId, ComponentId compId) throws RegistrationException;
    public String getComponentProperty(ApplicationInstanceId appInstId,
            ComponentId compId, ComponentInstanceId myId, String name) throws RegistrationException;
	public boolean applicationInstanceExists(ApplicationInstanceId appInstId) throws RegistrationException;
	boolean applicationComponentExists(ApplicationInstanceId appInstId, ComponentId compId) throws RegistrationException;
}
