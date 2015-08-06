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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;

public final class RmiWrapper implements LcaRegistry, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final RmiLcaRegistry delegate;
    
    RmiWrapper(RmiLcaRegistry _delegate) {
        delegate = _delegate;
    }
    
    @Override
    public void addApplicationInstance(ApplicationInstanceId instId, ApplicationId appId, String name) throws RegistrationException {
        try { delegate.addApplicationInstance(instId, appId, name); }
        catch(RemoteException re){throw new RegistrationException("operation failed.", re);}
    }

    @Override
    public void addComponent(ApplicationInstanceId instId, ComponentId cid, String name) throws RegistrationException {
        try { delegate.addComponent(instId, cid, name); }
        catch(RemoteException re){throw new RegistrationException("operation failed.", re);}
    }

    @Override
    public void addComponentInstance(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId) throws RegistrationException {
        try { delegate.addComponentInstance(instId, cid, cinstId); }
        catch(RemoteException re){throw new RegistrationException("operation failed.", re);}
    }

    @Override
    public void addComponentProperty(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId, String property,
            Object value) throws RegistrationException {
        try { delegate.addComponentProperty(instId, cid, cinstId, property, value); }
        catch(RemoteException re){throw new RegistrationException("operation failed.", re);}
    }

    @Override
    public Map<ComponentInstanceId, Map<String, String>> dumpComponent(
            ApplicationInstanceId instId, ComponentId compId) throws RegistrationException {
        try { return delegate.dumpComponent(instId, compId); }
        catch(RemoteException re){throw new RegistrationException("operation failed.", re);}
    }

    @Override
    public String getComponentProperty(ApplicationInstanceId appInstId, ComponentId compId, ComponentInstanceId myId, String name)
            throws RegistrationException {
         try { return delegate.getComponentProperty(appInstId, compId, myId, name); }
         catch(RemoteException re){throw new RegistrationException("operation failed.", re);}
    }
}
