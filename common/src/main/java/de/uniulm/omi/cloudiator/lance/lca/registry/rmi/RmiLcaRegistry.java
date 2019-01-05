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

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface RmiLcaRegistry extends Remote {
  boolean addApplicationInstance(ApplicationInstanceId instId, ApplicationId appId, String name)
      throws RemoteException;

  void addComponent(ApplicationInstanceId instId, ComponentId cid, String name)
      throws RemoteException;

  void addComponentInstance(
      ApplicationInstanceId instId, ComponentId cid, ComponentInstanceId cinstId)
      throws RemoteException;

  void addComponentProperty(
      ApplicationInstanceId instId,
      ComponentId cid,
      ComponentInstanceId cinstId,
      String property,
      Object value)
      throws RemoteException;

  Map<ComponentInstanceId, Map<String, String>> dumpComponent(
      ApplicationInstanceId instId, ComponentId compId) throws RemoteException;

  String getComponentProperty(
      ApplicationInstanceId appInstId, ComponentId compId, ComponentInstanceId myId, String name)
      throws RemoteException;

  boolean applicationInstanceExists(ApplicationInstanceId appInstId) throws RemoteException;

  boolean applicationComponentExists(ApplicationInstanceId appInstId, ComponentId compId)
      throws RemoteException;
}
