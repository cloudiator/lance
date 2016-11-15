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

import com.google.common.collect.Maps;
import de.uniulm.omi.cloudiator.lance.LcaConstants;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.LcaException;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.LifecycleAgent;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistryFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class LifecycleClient {

    private static Map<String, CacheEntry> lifecycleAgentCache = Maps.newConcurrentMap();

    private static class CacheEntry {
        private final Registry registry;
        private final LifecycleAgent lifecycleAgent;

        private CacheEntry(Registry registry, LifecycleAgent lifecycleAgent) {

            checkNotNull(registry, "registry is null.");
            checkNotNull(lifecycleAgent, "lifecycleAgent is null");

            this.registry = registry;
            this.lifecycleAgent = lifecycleAgent;
        }

        public Registry registry() {
            return registry;
        }

        public LifecycleAgent lifecycleAgent() {
            return lifecycleAgent;
        }
    }

    public static LifecycleClient getClient(String serverIp) throws RemoteException, NotBoundException {
        checkNotNull(serverIp);
        checkArgument(!serverIp.isEmpty());
        return new LifecycleClient(serverIp);
    }


    private static final LcaRegistry currentRegistry;
    private final LifecycleAgent lifecycleAgent;

    private LifecycleClient(String serverIp) throws RemoteException, NotBoundException {
        this.lifecycleAgent = findLifecycleAgent(serverIp);
    }

    static {
        try {
            RMISocketFactory.setSocketFactory(new RMISocketFactory() {

                private final RMISocketFactory delegate = RMISocketFactory.getDefaultSocketFactory();

                @Override public Socket createSocket(String host, int port) throws IOException {
                    final Socket socket = delegate.createSocket(host, port);
                    socket.setSoTimeout(30000);
                    socket.setKeepAlive(true);
                    return socket;
                }

                @Override public ServerSocket createServerSocket(int i) throws IOException {
                    return delegate.createServerSocket(i);
                }
            });
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        try {
            currentRegistry = RegistryFactory.createRegistry();
        } catch (RegistrationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public final ComponentInstanceId deploy(final DeploymentContext ctx,
                                            final DeployableComponent comp, final OperatingSystem os, final ContainerType containerType)
            throws DeploymentException {

        try {
            return lifecycleAgent.deployComponent(ctx, comp, os, containerType);
        } catch (RemoteException re) {
            throw new DeploymentException(handleRemoteException(re));
        } catch (LcaException | ContainerException | RegistrationException e) {
            throw new DeploymentException(e);
        }
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

    public final boolean undeploy(ComponentInstanceId componentInstanceId,
                                  ContainerType containerType) throws DeploymentException {
        try {
            return lifecycleAgent.stopComponentInstance(containerType, componentInstanceId);
        } catch (RemoteException e) {
            throw new DeploymentException(handleRemoteException(e));
        } catch (LcaException | ContainerException e) {
            throw new DeploymentException(e);
        }
    }

    private static Exception handleRemoteException(RemoteException re) {
        Throwable t = re.getCause();
        if (t == null)
            return new LcaException("network exception occurred");
        if (t instanceof LcaException)
            return (LcaException) t;
        if (t instanceof RegistrationException)
            return (RegistrationException) t;
        return new LcaException("downstream exception occurred.", re);

    }

    private static synchronized LifecycleAgent findLifecycleAgent(String serverIp)
            throws RemoteException, NotBoundException {

        if (!lifecycleAgentCache.containsKey(serverIp)) {
            Registry reg = LocateRegistry.getRegistry(serverIp);
            Object o = reg.lookup(LcaConstants.AGENT_REGISTRY_KEY);
            lifecycleAgentCache.put(serverIp, new CacheEntry(reg, (LifecycleAgent) o));
        }
        checkState(lifecycleAgentCache.containsKey(serverIp));
        return lifecycleAgentCache.get(serverIp).lifecycleAgent();
    }

    /**
     * @param myInstanceId the instance id
     * @param lsyAppId     the aplication id
     * @return true if this application instance has been added successfully. false if it was already contained
     * in the registry.
     * @throws RegistrationException when an registration error occurs
     */
    public boolean registerApplicationInstance(ApplicationInstanceId myInstanceId,
                                               ApplicationId lsyAppId) throws RegistrationException {
        return currentRegistry.addApplicationInstance(myInstanceId, lsyAppId, "<unknown name>");
    }

    public void registerApplicationInstance(ApplicationInstanceId myInstanceId,
                                            ApplicationId lsyAppId, String name) throws RegistrationException {
        currentRegistry.addApplicationInstance(myInstanceId, lsyAppId, name);
    }

    public void registerComponentForApplicationInstance(ApplicationInstanceId myInstanceId,
                                                        ComponentId zookeeperComponentId) throws RegistrationException {
        currentRegistry.addComponent(myInstanceId, zookeeperComponentId, "<unknown name>");
    }

    public void registerComponentForApplicationInstance(ApplicationInstanceId myInstanceId,
                                                        ComponentId zookeeperComponentId, String componentName) throws RegistrationException {
        currentRegistry.addComponent(myInstanceId, zookeeperComponentId, componentName);
    }

    public DeploymentContext initDeploymentContext(ApplicationId appId,
                                                   ApplicationInstanceId appInstanceId) {
        return new DeploymentContext(appId, appInstanceId, currentRegistry);
    }
}
