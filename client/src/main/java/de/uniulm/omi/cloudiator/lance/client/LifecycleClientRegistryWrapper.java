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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.CONTAINER_STATUS;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import de.uniulm.omi.cloudiator.lance.LcaConstants;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.application.component.RemoteDockerComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.LcaException;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.LifecycleAgent;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistryFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      currentRegistry.addComponent(params.getAppId(), params.getcId(), params.getName());
      currentRegistry.addComponentInstance(params.getAppId(), params.getcId(), params.getcInstId());
      currentRegistry.addComponentProperty(params.getAppId(), params.getcId(), params.getcInstId(),
          LcaRegistryConstants.regEntries.get(CONTAINER_STATUS), params.getStatus().toString());
      //do I need to create a DeploymentContext for this and do setProperty instead?

      for (ExternalContextParameters.InPortContext inPortC : params.getInpContext()) {
        currentRegistry.addComponentProperty(
            params.getAppId(),
            params.getcId(),
            params.getcInstId(),
            inPortC.getFullPortName(),
            inPortC.getInernalInPortNmbr().toString());
      }

      currentRegistry.addComponentProperty(
          params.getAppId(),
          params.getcId(),
          params.getcInstId(),
          params.getFullHostName(),
          params.getPublicIp());
    } catch (RegistrationException e) {
      e.printStackTrace();
    }
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
