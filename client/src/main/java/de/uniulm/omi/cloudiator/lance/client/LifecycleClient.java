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
import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

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

public final class LifecycleClient {

  private final static Logger LOGGER = LoggerFactory.getLogger(LifecycleClient.class);

  public static LifecycleClient getClient(String serverIp)
      throws RemoteException, NotBoundException {
    checkNotNull(serverIp);
    checkArgument(!serverIp.isEmpty());
    return new LifecycleClient(serverIp, 0);
  }

  public static LifecycleClient getClient(String serverIp, int rmiTimeout)
      throws RemoteException, NotBoundException {
    checkNotNull(serverIp);
    checkArgument(!serverIp.isEmpty());
    checkArgument(rmiTimeout >= 0, "rmiTimeout must be larger or equal to 0");
    return new LifecycleClient(serverIp, rmiTimeout);
  }

  public static LifecycleClientRegistryWrapper getRegWrapper() {
    return regWrapper;
  }

  private static final LifecycleClientRegistryWrapper regWrapper;
  private final LifecycleAgent lifecycleAgent;

  private LifecycleClient(String serverIp, int rmiTimeout)
      throws RemoteException, NotBoundException {
    try {
      RMISocketFactory.setSocketFactory(new RMISocketFactory() {

        private final RMISocketFactory delegate =
            RMISocketFactory.getDefaultSocketFactory();

        @Override
        public Socket createSocket(String host, int port) throws IOException {
          final Socket socket = delegate.createSocket(host, port);
          if (rmiTimeout != 0) {
            socket.setSoTimeout(rmiTimeout);
            socket.setSoLinger(false, 0);
            socket.connect(new InetSocketAddress(host, port), rmiTimeout);
          }
          return socket;
        }

        @Override
        public ServerSocket createServerSocket(int i) throws IOException {
          return delegate.createServerSocket(i);
        }
      });
    } catch (IOException e) {
      //ignored
    }
    this.lifecycleAgent = findLifecycleAgent(serverIp);
  }

  static {
   regWrapper = LifecycleClientRegistryWrapper.getInstance();
  }

  public final ComponentInstanceId deploy(final DeploymentContext ctx,
      final DeployableComponent comp, final OperatingSystemImpl os, final ContainerType containerType)
      throws DeploymentException {

    final Retryer<ComponentInstanceId> retryer = buildRetryerComponent();

    Callable<ComponentInstanceId> callable = () -> {
      LOGGER.info("Trying to deploy Lifecycle component " + comp);
      return lifecycleAgent
          .deployDeployableComponent(ctx, comp, os, containerType);
    };

    return doRetryerCall(retryer, callable);
  }

  public final ComponentInstanceId deploy(final DeploymentContext ctx,
      final DockerComponent comp)
      throws DeploymentException {

    final Retryer<ComponentInstanceId> retryer = buildRetryerComponent();

    Callable<ComponentInstanceId> callable = () -> {
      LOGGER.info("Trying to deploy Docker component " + comp);
      return lifecycleAgent
          .deployDockerComponent(ctx, comp);
    };

    return doRetryerCall(retryer, callable);
  }

  public final ComponentInstanceId deploy(final DeploymentContext ctx,
      final RemoteDockerComponent comp)
      throws DeploymentException {

    final Retryer<ComponentInstanceId> retryer = buildRetryerComponent();

    Callable<ComponentInstanceId> callable = () -> {
      LOGGER.info("Trying to deploy Remote Docker component " + comp);
      return lifecycleAgent
          .deployRemoteDockerComponent(ctx, comp);
    };

    return doRetryerCall(retryer, callable);
  }

  private static Retryer<ComponentInstanceId> buildRetryerComponent() {
    Retryer<ComponentInstanceId> retryer = RetryerBuilder.<ComponentInstanceId>newBuilder()
        .retryIfExceptionOfType(RemoteException.class).withWaitStrategy(
            WaitStrategies.exponentialWait()).withStopStrategy(StopStrategies.stopAfterDelay(5,
            TimeUnit.MINUTES)).build();

    return retryer;
  }

  private static ComponentInstanceId doRetryerCall(Retryer<ComponentInstanceId> retryer,
      Callable<ComponentInstanceId> callable) throws DeploymentException {
    try {
      return retryer.call(callable);
    } catch (ExecutionException e) {
      throw new DeploymentException(e.getCause());
    } catch (RetryException e) {
      if (e.getCause() instanceof RemoteException) {
        throw new DeploymentException(handleRemoteException((RemoteException) e.getCause()));
      } else {
        throw new IllegalStateException(e);
      }
    }
  }

  public void injectExternalDeploymentContext(ExternalContextParameters params)
      throws DeploymentException {
    regWrapper.injectExternalDeploymentContext(params);
  }

  public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid, String serverIp)
      throws DeploymentException {
    try {
      final LifecycleAgent lifecycleAgent = findLifecycleAgent(serverIp);
      return lifecycleAgent.getComponentContainerStatus(cid);
    } catch (RemoteException e) {
      throw new DeploymentException(handleRemoteException(e));
    } catch (NotBoundException e) {
      throw new DeploymentException(new RegistrationException("bad registry handling.", e));
    }
  }

  public void waitForDeployment(ComponentInstanceId cid) {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        final ContainerStatus componentContainerStatus =
            lifecycleAgent.getComponentContainerStatus(cid);
        if (ContainerStatus.READY.equals(componentContainerStatus)) {
          return;
        }
        if (ContainerStatus.errorStates().contains(componentContainerStatus)) {
          throw new IllegalStateException(String
              .format("Container reached illegal state %s while waiting for state %s",
                  componentContainerStatus, ContainerStatus.READY));
        }
        Thread.sleep(10000);
      }
    } catch (RemoteException e) {
      throw new RuntimeException(
          String.format("Error while waiting for container %s to be ready.", cid), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Got interrupted while waiting for container to be ready.");
    }
  }

  public final boolean isReady(ComponentInstanceId cid) {
    try {
      boolean isReady = lifecycleAgent.componentInstanceIsReady(cid);
      if (isReady) {
        return true;
      }
      return false;
    } catch (RemoteException | LcaException | ContainerException e) {
      throw new RuntimeException(
          String.format("Error while waiting for container %s to be ready.", cid), e);
    }
  }

  public final boolean undeploy(ComponentInstanceId componentInstanceId,
      boolean forceRegDel) throws DeploymentException {
    try {
      return lifecycleAgent.stopComponentInstance(componentInstanceId, forceRegDel);
    } catch (RemoteException e) {
      throw new DeploymentException(handleRemoteException(e));
    } catch (LcaException | ContainerException e) {
      throw new DeploymentException(e);
    }
  }

  public final void unRegisterInstance(ApplicationInstanceId appInstId, ComponentId componentId,
      ComponentInstanceId componentInstanceId) throws RegistrationException, DeploymentException {
    regWrapper.unRegisterInstance(appInstId, componentId, componentInstanceId);
  }

  public final void updateDownStreamPorts() throws DeploymentException {
    try {
      lifecycleAgent.updateDownStreamPorts();
    } catch (RemoteException e) {
      throw new DeploymentException(handleRemoteException(e));
    } catch (LcaException | ContainerException e) {
      throw new DeploymentException(e);
    }
  }

  private static Exception handleRemoteException(RemoteException re) {
    Throwable t = re.getCause();
    if (t == null) {
      return new LcaException("network exception occurred", re);
    }
    if (t instanceof LcaException) {
      return (LcaException) t;
    }
    if (t instanceof RegistrationException) {
      return (RegistrationException) t;
    }
    return new LcaException("downstream exception occurred.", re);

  }

  private static synchronized LifecycleAgent findLifecycleAgent(String serverIp)
      throws RemoteException, NotBoundException {

    Registry reg = LocateRegistry.getRegistry(serverIp);
    Object o = reg.lookup(LcaConstants.AGENT_REGISTRY_KEY);

    return (LifecycleAgent) o;
  }

  /**
   * @param myInstanceId the instance id
   * @param lsyAppId the aplication id
   * @return true if this application instance has been added successfully. false if it was already
   * contained in the registry.
   * @throws RegistrationException when an registration error occurs
   */
  public boolean registerApplicationInstance(ApplicationInstanceId myInstanceId,
      ApplicationId lsyAppId) throws RegistrationException {
    return regWrapper.registerApplicationInstance(myInstanceId, lsyAppId);
  }

  public void registerApplicationInstance(ApplicationInstanceId myInstanceId,
      ApplicationId lsyAppId, String name) throws RegistrationException {
    regWrapper.registerApplicationInstance(myInstanceId, lsyAppId, name);
  }

  public void registerComponentForApplicationInstance(ApplicationInstanceId myInstanceId,
      ComponentId zookeeperComponentId) throws RegistrationException {
    regWrapper.registerComponentForApplicationInstance(myInstanceId, zookeeperComponentId);
  }

  public void registerComponentForApplicationInstance(ApplicationInstanceId myInstanceId,
      ComponentId zookeeperComponentId, String componentName) throws RegistrationException {
    regWrapper.registerComponentForApplicationInstance(myInstanceId, zookeeperComponentId, componentName);
  }

  public DeploymentContext initDeploymentContext(ApplicationId appId,
      ApplicationInstanceId appInstanceId) {
    return new DeploymentContext(appId, appInstanceId, regWrapper.getCurrentRegistry());
  }
}
