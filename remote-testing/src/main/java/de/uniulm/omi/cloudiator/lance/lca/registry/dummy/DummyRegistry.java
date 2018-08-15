package de.uniulm.omi.cloudiator.lance.lca.registry.dummy;

import java.rmi.RemoteException;
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.rmi.RemoteRegistryImpl;

public class DummyRegistry implements LcaRegistry {

    private final RemoteRegistryImpl reg = new RemoteRegistryImpl();

    @Override
    public boolean addApplicationInstance(ApplicationInstanceId instId, ApplicationId appId, String name)
            throws RegistrationException {
        try {
            return reg.addApplicationInstance(instId, appId, name);
        } catch(RemoteException re) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void addComponent(ApplicationInstanceId instId, ComponentId cid, String name) throws RegistrationException {
        try {
            reg.addComponent(instId, cid, name);
        } catch(RemoteException re) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void addComponentInstance(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId)
            throws RegistrationException {
        try {
            reg.addComponentInstance(instId, cid, cinstId);
        } catch(RemoteException re) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void addComponentProperty(ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId,
                                     String property, Object value) throws RegistrationException {
        try {
            reg.addComponentProperty(instId, cid, cinstId, property, value);
        } catch(RemoteException re) {
            throw new IllegalStateException();
        }
    }

    @Override
    public Map<ComponentInstanceId, Map<String, String>> dumpComponent(ApplicationInstanceId instId, ComponentId compId) throws RegistrationException {
        try {
            return reg.dumpComponent(instId, compId);
        } catch(RemoteException re) {
            throw new IllegalStateException();
        }
    }

    @Override
    public String getComponentProperty(ApplicationInstanceId appInstId, ComponentId compId, ComponentInstanceId myId,
                                       String name) throws RegistrationException {
        try {
            return reg.getComponentProperty(appInstId, compId, myId, name);
        } catch(RemoteException re) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean applicationInstanceExists(ApplicationInstanceId appInstId) throws RegistrationException {
        try {
            return reg.applicationInstanceExists(appInstId);
        } catch(RemoteException re) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean applicationComponentExists(ApplicationInstanceId appInstId, ComponentId compId)
            throws RegistrationException {
        try {
            return reg.applicationComponentExists(appInstId, compId);
        } catch(RemoteException re) {
            throw new IllegalStateException();
        }
    }

}
