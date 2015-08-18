package de.uniulm.omi.cloudiator.lance.util;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Registrator<T extends Remote> {
	
	public static final <S extends Remote> Registrator<S> create(Class<S> clazz) {
		return new Registrator<>(clazz);
	}
	
	private final static Logger LOGGER = LoggerFactory.getLogger(Registrator.class);

	private final Class<T> myClass;
	
	private Registrator (Class<T> classParam) {
		myClass = classParam;
	}
	
	public <S extends T> T export(S agent, int port) {
        try {
            return (T) UnicastRemoteObject.exportObject(agent, port);
        } catch(RemoteException re) {
            LOGGER.error("got exception at export; quitting the platform", re);
        }
        return null;
    }
    
    public boolean addToRegistry(T lca, String key) {
        try {
            Registry reg = getAndCreateRegistry();
            removeExisting(reg, key);
            reg.rebind(key, lca);
            return true;
        } catch(RemoteException e) { // includes AccessException //
            LOGGER.error("got exception at startup: could not add lca to registry. aborting.", e);
        } 
        return false;
    }
    
    private static Registry getAndCreateRegistry() throws RemoteException {
        try { 
            return java.rmi.registry.LocateRegistry.createRegistry(Registry.REGISTRY_PORT); 
        } catch(RemoteException re) {
            LOGGER.info("could not create registry. assuming, it already exists.", re);
            return java.rmi.registry.LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
        }
    }
    
    private void removeExisting(Registry reg, String key) throws RemoteException {
        T object = null;
        Object o = null;
        
        try { 
            o = reg.lookup(key);
        } catch(NotBoundException nbe){
            LOGGER.debug("could not remove element as it was not registered.", nbe);
            return; 
        }
        
        if(o != null && myClass.isAssignableFrom(o.getClass())){
        	//FIXME: should we do something with this?
        	/*
        	 * if(o instanceof LifecycleAgent) {
            agent = (LifecycleAgent) o;
            agent.stop();
        	 */
        }
            
        try { 
            reg.unbind(key); 
        } catch(NotBoundException nbe){
            LOGGER.info("could not remove element as it was not registered.", nbe);
            return; 
        }
    }

    public<S extends T> void unregister(S element) {
        try { 
            UnicastRemoteObject.unexportObject(element, true);
            // TODO: shutdown registry if possible //
        } catch(NoSuchObjectException ex) {
            LOGGER.info("LCA has not been registered at this registry.", ex);
        }
    }
}
