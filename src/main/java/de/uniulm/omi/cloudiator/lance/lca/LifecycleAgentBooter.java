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

import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.uniulm.omi.cloudiator.lance.lca.LifecycleAgent.AGENT_REGISTRY_KEY;
import static de.uniulm.omi.cloudiator.lance.lca.LifecycleAgent.AGENT_RMI_PORT;

public final class LifecycleAgentBooter {

	private final static Logger log = Logger.getLogger(LifecycleAgentBooter.class.getName());
	
	public static void main(String[] args) {
		System.err.println("LifecycleAgentBooter: starting.");
		LifecycleAgentImpl lca = createAgentImplementation();
		LifecycleAgent stub = exportAgent(lca);
		// TODO: it might be worth exploiting ways to get rid of this
		// dependency to a registry. note that there does not seem to
		// be an easy way to do it (i.e. relaying on standard interfaces)
		if(stub != null && addToRegistry(stub)) {
			// from here on RMI takes over //
			System.err.println("LifecycleAgentBooter: agent exported. waiting for requests.");
			return;
		}
		System.err.println("cannot start lifecycle agent");
		Runtime.getRuntime().exit(-128);
	}
	
	private static LifecycleAgentImpl createAgentImplementation() {
		HostContext ctx = EnvContext.fromEnvironment();
		System.err.println("LifecycleAgentBooter: created host context: " + ctx);
		LifecycleAgentImpl impl = new LifecycleAgentImpl(ctx);
		impl.init();
		return impl;
	}
	
	private static LifecycleAgent exportAgent(LifecycleAgentImpl agent) {
		try {
			return (LifecycleAgent) UnicastRemoteObject.exportObject(agent, AGENT_RMI_PORT);
		} catch(RemoteException re) {
			log.log(Level.INFO, "got exception at export", re);
		}
		return null;
	}
	RemoteRef ref;
	private static boolean addToRegistry(LifecycleAgent lca) {
		try {
			Registry reg = getAndCreateRegistry();
			removeExisting(reg);
			reg.rebind(AGENT_REGISTRY_KEY, lca);
			return true;
		} catch(AccessException ae) {
			log.log(Level.INFO, "got exception at startup", ae);
		} catch(RemoteException re) {
			log.log(Level.INFO, "got exception at startup", re);
		} 
		return false;
	}
	
	private static Registry getAndCreateRegistry() throws RemoteException {
		try { return java.rmi.registry.LocateRegistry.createRegistry(Registry.REGISTRY_PORT); }
		catch(RemoteException re) {
			return java.rmi.registry.LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
		}
	}
	
	private static void removeExisting(Registry reg) throws AccessException, RemoteException {
		LifecycleAgent agent = null;
		Object o = null;
		
		try { o = reg.lookup(AGENT_REGISTRY_KEY); }
		catch(NotBoundException nbe){ return; }
			
		if(o instanceof LifecycleAgent) {
			agent = (LifecycleAgent) o;
			agent.stop();
		}
			
		try { reg.unbind(AGENT_REGISTRY_KEY); }
		catch(NotBoundException nbe){ return; }
	}

	public static void unregister(LifecycleAgentImpl lifecycleAgentImpl) {
		try { UnicastRemoteObject.unexportObject(lifecycleAgentImpl, true); }
		catch(NoSuchObjectException ex) {
			ex.printStackTrace();
		}
		//FIXME
	}

}
