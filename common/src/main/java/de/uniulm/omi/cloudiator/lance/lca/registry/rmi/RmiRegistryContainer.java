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

import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistryContainer;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public final class RmiRegistryContainer implements RegistryContainer {

  private static final int AGENT_LIFECYCLE_REGISTRY_PORT = 33034;
  private final RemoteRegistryImpl reg = new RemoteRegistryImpl();
  private final RmiLcaRegistry exportedRegistry;
  private final RmiWrapper wrapper;

  private RmiRegistryContainer() throws RemoteException {
    exportedRegistry = initComponentRegistry();
    wrapper = new RmiWrapper(exportedRegistry);
  }

  public static RegistryContainer create() throws RegistrationException {
    try {
      return new RmiRegistryContainer();
    } catch (RemoteException re) {
      throw new RegistrationException("cannot create registry", re);
    }
  }

  private RmiLcaRegistry initComponentRegistry() throws RemoteException {
    RmiLcaRegistry re =
        (RmiLcaRegistry) UnicastRemoteObject.exportObject(reg, AGENT_LIFECYCLE_REGISTRY_PORT);
    return re;
  }

  @Override
  public LcaRegistry getRegistry() {
    return wrapper;
  }
}
