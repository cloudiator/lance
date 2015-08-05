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

package de.uniulm.omi.cloudiator.lance.lca.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.registry.etcd.EtcdRegistryContainer;
import de.uniulm.omi.cloudiator.lance.lca.registry.rmi.RmiRegistryContainer;

public enum RegistryFactory {

	RMI_REGISTRY{
		@Override RegistryContainer create() throws RegistrationException {
			return RmiRegistryContainer.create();
		}
	},
	ETCD_REGISTRY{
		@Override RegistryContainer create() throws RegistrationException {
			return EtcdRegistryContainer.create();
		}
	};
	
	abstract RegistryContainer create() throws RegistrationException;

	private static final Logger LOGGER = LoggerFactory.getLogger(LcaRegistry.class);
	
	public static final String LCA_REGISTRY_CONFIG_KEY = "lca.client.config.registry";
	public static final String LCA_REGISTRY_CONFIG_RMI_VALUE = "rmiregistry";
	public static final String LCA_REGISTRY_CONFIG_ETCD_VALUE = "etcdregistry";
	
	private static RegistryContainer container = null;
	
	public synchronized static LcaRegistry createRegistry() throws RegistrationException {
		if(container == null) {
			container = doCreateRegistry();
		}
		return container.getRegistry();
	}
	
	public static RegistryContainer doCreateRegistry() throws RegistrationException {
		LOGGER.info("looking for registry configuration.");
		String value = System.getProperty(LCA_REGISTRY_CONFIG_KEY);
		RegistryContainer retVal = null;
		
		if(LCA_REGISTRY_CONFIG_ETCD_VALUE.equals(value)) {
			LOGGER.debug("checking for etcd registry configuration.");
			retVal = ETCD_REGISTRY.create();
		} else if (LCA_REGISTRY_CONFIG_RMI_VALUE.equals(value)) {
			LOGGER.debug("checking for rmi-based registry configuration.");
			retVal = RMI_REGISTRY.create();
		}
		
		if(retVal != null) return retVal;
		LOGGER.warn("registry creation failed: falling back to RMI.");
		return RMI_REGISTRY.create();
	}
}
