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
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.rmi.RemoteException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifecycleAgentImpl implements LifecycleAgent {

  private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleAgentImpl.class);

  private volatile AgentStatus status = AgentStatus.NEW;
  private final ContainerContainment containers = new ContainerContainment();

  private final HostContext hostContext;

  LifecycleAgentImpl(HostContext contex) {

    //manager = ContainerManagerFactory.createContainerManager(contex, ContainerType.fromString(contex.getContainerType()));
    this.hostContext = contex;
    status = AgentStatus.CREATED;
  }

  synchronized void init() {
    status = AgentStatus.READY;
  }

  @Override
  public synchronized void terminate() {
    LOGGER.info("terminating lifecycle agent");
    LifecycleAgentBooter.unregister(this);
    try {
      hostContext.close();
    } catch (InterruptedException ie) {
      LOGGER.warn("shutting down interrupted");
    }
    containers.terminate();
  }


  @Override
  public synchronized void stop() {
    LOGGER.info("stopping lifecycle agent. no further connections possible.");
    LifecycleAgentBooter.unregister(this);
  }

  @Override
  public List<ComponentInstanceId> listContainers() throws RemoteException {
    return containers.getAllContainers();
  }

  @Override
  public ComponentInstanceId deployComponent(DeploymentContext ctx, DeployableComponent component,
      OperatingSystem os, ContainerType containerType)
      throws RemoteException, LcaException, RegistrationException, ContainerException {
    applicationRegistered(ctx);
    componentPartOfApplication(ctx, component);

    ContainerManager manager = containers.getContainerManager(hostContext, os, containerType);

    LOGGER.info(String
        .format("Creating new container using context %s, os %s and containerType %s.",
            hostContext, os, containerType));
    ContainerController cc = manager.createNewContainer(ctx, component, os);

    LOGGER.info(String
        .format("Dispatching handling of container controller %s to execution handler.", cc));
    hostContext.run(() -> {
      try {
        LOGGER.info(String.format("Awaiting creation of %s.", cc));
        cc.awaitCreation();
        LOGGER.info(String.format("Bootstrapping %s.", cc));
        cc.bootstrap();
        LOGGER.info(String.format("Awaiting Bootstrapping %s.", cc));
        cc.awaitBootstrap();
        LOGGER
            .info(String.format("Initiating lifecycle store of component %s.", component));
        cc.init(component.getLifecycleStore());
        LOGGER.info(String.format("Awaiting Initialisation of %s.", cc));
        cc.awaitInitialisation();
      } catch (ContainerException e) {
        LOGGER.error("Error occurred in container controller " + cc, e);
      } catch (Exception e) {
        LOGGER.error("Unexpected exception occured in container controller " + cc, e);
      }
    });

    LOGGER.info(String.format("Returning ID %s of %s", cc.getId(), cc));
    return cc.getId();
  }

  @Override
  public boolean stopComponentInstance(ContainerType containerType,
      ComponentInstanceId instanceId) throws RemoteException, LcaException, ContainerException {
    ContainerController cid = containers.getContainer(instanceId);
    if (cid == null) {
      throw new LcaException("Component instance not known: " + instanceId);
    }
    ContainerStatus state = cid.getState();
    if (state != ContainerStatus.READY) {
      throw new ContainerException("container in wrong state: " + state);
    }
    cid.tearDown();
    cid.awaitDestruction();
    return true;
  }

  private static void applicationRegistered(DeploymentContext ctx)
      throws LcaException, RegistrationException {
    LcaRegistry reg = ctx.getRegistry();
    ApplicationInstanceId appInstId = ctx.getApplicationInstanceId();

    if (reg.applicationInstanceExists(appInstId)) {
      return;
    }

    throw new LcaException("cannot proceed: application instance is not known.");
  }

  //todo: print whole environment
  @Override
  public String getEnv() throws RemoteException {
    StringBuilder builder = new StringBuilder();
    builder.append("PUBLIC_IP_KEY=" + hostContext.getPublicIp() + "\n");
    builder.append("PRIVATE_IP_KEY=" + hostContext.getInternalIp() + "\n");
    builder.append("VM_ID_KEY=" + hostContext.getVMIdentifier());
    return builder.toString();
  }

  private static void componentPartOfApplication(DeploymentContext ctx,
      DeployableComponent component) throws RegistrationException, LcaException {
    LcaRegistry reg = ctx.getRegistry();
    ApplicationInstanceId appInstId = ctx.getApplicationInstanceId();

    if (reg.applicationComponentExists(appInstId, component.getComponentId())) {
      return;
    }

    throw new LcaException(
        String.format("cannot proceed: component %s is not known within application instance %s.",
            component, appInstId));
  }

  @Override
  public AgentStatus getAgentStatus() {
    return status;
  }

  @Override
  public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) {
    LOGGER.info("Resolving status of container " + cid);
    ContainerStatus ret = null;
    try {
      ret = containers.getComponentContainerStatus(cid);
      return ret;
    } finally {
      LOGGER.info("Status of container " + cid + " is " + ret);
    }
  }
}
