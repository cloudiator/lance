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

import de.uniulm.omi.cloudiator.lance.LcaConstants;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.LcaException;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.LifecycleAgent;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistryFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Optional;

public final class LifecycleClient {

    public static LifecycleClient getClient() {
        return InnerClient.client;
    }

    private static class InnerClient {
        private static final LifecycleClient client;

        static {
            LifecycleClient _client = null;
            try {
                _client = new LifecycleClient();
            } catch (Throwable t) {
                System.err.println("error while setting up client singleton.");
                t.printStackTrace();
                System.exit(-1);
            }
            client = _client;
        }
    }


    private final LcaRegistry currentRegistry;

    private LifecycleClient() throws RegistrationException {
        currentRegistry = RegistryFactory.createRegistry();
    }


    public final Optional<ComponentInstanceId> deploy(String serverIp, final DeploymentContext ctx,
        final DeployableComponent comp, final OperatingSystem os, final ContainerType containerType)
        throws LcaException, RegistrationException, ContainerException {
        try {
            LifecycleAgent agent = findLifecycleAgent(serverIp);
            return Optional.of(deploy(agent, ctx, comp, os, containerType));
        } catch (RemoteException re) {
            handleRemoteException(re);
        } catch (NotBoundException e) {
            throw new RegistrationException("bad registry handling.", e);
        }
        return Optional.empty();
    }

    private static void handleRemoteException(RemoteException re)
        throws LcaException, RegistrationException {
        Throwable t = re.getCause();
        if (t == null)
            throw new LcaException("network exception occurred");
        if (t instanceof LcaException)
            throw (LcaException) t;
        if (t instanceof RegistrationException)
            throw (RegistrationException) t;
        throw new LcaException("downstream exception occurred.", t);

    }

    private static LifecycleAgent findLifecycleAgent(String serverIp)
        throws RemoteException, NotBoundException {

        Registry reg = LocateRegistry.getRegistry(serverIp);
        Object o = reg.lookup(LcaConstants.AGENT_REGISTRY_KEY);
        LifecycleAgent agent = (LifecycleAgent) o;
        return agent;
    }

    private static ComponentInstanceId deploy(final LifecycleAgent agent,
        final DeploymentContext ctx, final DeployableComponent comp, final OperatingSystem os,
        final ContainerType containerType)
        throws RemoteException, LcaException, RegistrationException, ContainerException {
    /*executor.submit(new Callable<ComponentInstanceId>() {

			@Override
			public ComponentInstanceId call() throws Exception {
				return agent.deployComponent(ctx, comp, os);		
			}
		});*/
        return agent.deployComponent(ctx, comp, os, containerType);
        // catch(Throwable t) {t.printStackTrace();}
    }

    /**
     * @param myInstanceId
     * @param lsyAppId
     * @return true if this application instance has been added successfully. false if it was already contained
     * in the registry.
     * @throws RegistrationException
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
