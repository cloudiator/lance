/*
 * Copyright (c) 2019 University of Ulm
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
package de.uniulm.omi.cloudiator.lance.client;

import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.COMPONENT_INSTANCE_STATUS;
import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.CONTAINER_STATUS;
import static java.lang.Thread.sleep;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistryFactory;

public final class LifecycleClientRegistryWrapper {

  private static final LifecycleClientRegistryWrapper INSTANCE =
      new LifecycleClientRegistryWrapper();
  private static final LcaRegistry currentRegistry;

  public static LifecycleClientRegistryWrapper getInstance() {
    return INSTANCE;
  }

  public static LcaRegistry getCurrentRegistry() {
    return currentRegistry;
  }

  private LifecycleClientRegistryWrapper() {}

  static {
    try {
      currentRegistry = RegistryFactory.createRegistry();
    } catch (RegistrationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static void injectExternalDeploymentContext(ExternalContextParameters params)
      throws DeploymentException {
    try {
      // do not insert context if registry does not exist yet, the creation is the job of the lance user/orchestrator
      while(!currentRegistry.applicationInstanceExists(params.getAppId())) {
        sleep(1500);
      }

      currentRegistry.addComponent(params.getAppId(), params.getcId(), params.getTaskName());
      currentRegistry.addComponentInstance(params.getAppId(), params.getcId(), params.getcInstId());
      currentRegistry.addComponentProperty(params.getAppId(), params.getcId(), params.getcInstId(),
          LcaRegistryConstants.regEntries.get(CONTAINER_STATUS), params.getStatus().toString());
      currentRegistry.addComponentProperty(params.getAppId(), params.getcId(), params.getcInstId(),
          LcaRegistryConstants.regEntries.get(COMPONENT_INSTANCE_STATUS), params.getCompInstStatus().toString());
      //do I need to create a DeploymentContext for this and do setProperty instead?

      insertIpHierarchyParams(params);
      insertPortHierarchyParams(params);
    } catch (RegistrationException e) {
      throw new DeploymentException(String.format("Cannot inject external deployment context for task: %s due"
          + "to resgistry issues", params.getTaskName()), e);
    } catch (InterruptedException e) {
      throw new DeploymentException(String.format("Cannot inject external deployment context for task: %s due"
          + "to an interruption while waiting for the registry to become accessible", params.getTaskName()), e);
    }
  }

  private static void insertIpHierarchyParams(ExternalContextParameters params) throws RegistrationException {
    currentRegistry.addComponentProperty(
        params.getAppId(),
        params.getcId(),
        params.getcInstId(),
        ExternalContextParameters.IpContext.getFullIpNamePublic(),
        params.getPublicIp());

    currentRegistry.addComponentProperty(
        params.getAppId(),
        params.getcId(),
        params.getcInstId(),
        ExternalContextParameters.IpContext.getFullIpNameCloud(),
        params.getPublicIp());

    currentRegistry.addComponentProperty(
        params.getAppId(),
        params.getcId(),
        params.getcInstId(),
        ExternalContextParameters.IpContext.getFullIpNameContainer(),
        params.getPublicIp());
  }

  private static void insertPortHierarchyParams(ExternalContextParameters params) throws RegistrationException {
    currentRegistry.addComponentProperty(
        params.getAppId(),
        params.getcId(),
        params.getcInstId(),
        params.getProvidedPortContext().getFullPortNamePublic(),
        params.getProvidedPortContext().getPortNmbr().toString());

    currentRegistry.addComponentProperty(
        params.getAppId(),
        params.getcId(),
        params.getcInstId(),
        params.getProvidedPortContext().getFullPortNameCloud(),
        params.getProvidedPortContext().getPortNmbr().toString());

    currentRegistry.addComponentProperty(
        params.getAppId(),
        params.getcId(),
        params.getcInstId(),
        params.getProvidedPortContext().getFullPortNameContainer(),
        params.getProvidedPortContext().getPortNmbr().toString());
  }

  public static final void unRegisterInstance(ApplicationInstanceId appInstId, ComponentId componentId,
      ComponentInstanceId componentInstanceId) throws RegistrationException, DeploymentException {
    currentRegistry.deleteComponentInstance(appInstId, componentId, componentInstanceId);
  }

  /**
   * @param myInstanceId the instance id
   * @param lsyAppId the aplication id
   * @return true if this application instance has been added successfully. false if it was already
   * contained in the registry.
   * @throws RegistrationException when an registration error occurs
   */
  public static boolean registerApplicationInstance(ApplicationInstanceId myInstanceId,
      ApplicationId lsyAppId) throws RegistrationException {
    return currentRegistry.addApplicationInstance(myInstanceId, lsyAppId, "<unknown name>");
  }

  public static void registerApplicationInstance(ApplicationInstanceId myInstanceId,
      ApplicationId lsyAppId, String name) throws RegistrationException {
    currentRegistry.addApplicationInstance(myInstanceId, lsyAppId, name);
  }

  public static void registerComponentForApplicationInstance(ApplicationInstanceId myInstanceId,
      ComponentId zookeeperComponentId) throws RegistrationException {
    currentRegistry.addComponent(myInstanceId, zookeeperComponentId, "<unknown name>");
  }

  public static void registerComponentForApplicationInstance(ApplicationInstanceId myInstanceId,
      ComponentId zookeeperComponentId, String componentName) throws RegistrationException {
    currentRegistry.addComponent(myInstanceId, zookeeperComponentId, componentName);
  }

  public static DeploymentContext initDeploymentContext(ApplicationId appId,
      ApplicationInstanceId appInstanceId) {
    return new DeploymentContext(appId, appInstanceId, currentRegistry);
  }
}
