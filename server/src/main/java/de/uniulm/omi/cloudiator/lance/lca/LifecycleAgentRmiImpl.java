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

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.*;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.List;

public class LifecycleAgentRmiImpl implements LifecycleAgent {

  private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleAgentRmiImpl.class);

  private LifecycleAgentCore core;

  LifecycleAgentRmiImpl(LifecycleAgentCore core) {
    this.core = core;
  }

  @Override
  public synchronized void terminate() {
    core.terminate();
  }


  @Override
  public synchronized void stop() {
    LOGGER.info("stopping lifecycle agent. no further connections possible.");
    LifecycleAgentBooter.unregisterRmi(this);
  }

  @Override
  public List<ComponentInstanceId> listContainers() throws RemoteException {
    return core.listContainers();
  }

  @Override
  public ComponentInstanceId deployComponent(DeploymentContext ctx, DeployableComponent component,
      OperatingSystem os, ContainerType containerType)
      throws RemoteException, LcaException, RegistrationException, ContainerException {
    return core.deployComponent(ctx, component, os, containerType);
  }

  @Override
  public boolean stopComponentInstance(ContainerType containerType,
      ComponentInstanceId instanceId) throws RemoteException, LcaException, ContainerException {
    return core.stopComponentInstance(containerType, instanceId);
  }

  private static void applicationRegistered(DeploymentContext ctx)
      throws LcaException, RegistrationException {
    applicationRegistered(ctx);
  }

  @Override
  public AgentStatus getAgentStatus() {
    return core.getAgentStatus();
  }

  @Override
  public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) {
    return core.getComponentContainerStatus(cid);
  }

}
