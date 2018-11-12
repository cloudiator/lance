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

package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.application.component.LifecycleComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ErrorAwareContainer;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.*;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.registry.ContainerRegistry;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.PlainContainerLogic.Builder;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Daniel Seybold on 10.08.2015.
 */
public class PlainContainerManager implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManager.class);
    private final ContainerRegistry registry = new ContainerRegistry();
    private final HostContext hostContext;

    public PlainContainerManager(HostContext vmId) {
        this.hostContext = vmId;
    }

    @Override public ContainerType getContainerType() {
        return ContainerType.PLAIN;
    }

    @Override public ContainerController getContainer(ComponentInstanceId id) {
        return this.registry.getContainer(id);
    }

    @Override
    public ContainerController createNewLifecycleContainer(DeploymentContext ctx,
        LifecycleComponent component, OperatingSystem os) throws ContainerException {

      AbstractComponentDependencies cDeps = new AbstractComponentDependencies(ctx, component, hostContext);
        PlainShellFactory plainShellFactory = new PlainShellFactoryImpl();
        PlainContainerLogic.Builder builder = new PlainContainerLogic.Builder();
        PlainContainerLogic plainContainerLogic = builder.cInstId(cDeps.componentInstanceId).deplComp(component)
            .deplContext(ctx).operatingSys(os).nwHandler(cDeps.networkHandler).plShellFac(plainShellFactory)
            .hostContext(this.hostContext).build();

        ExecutionContext executionContext = new ExecutionContext(os, plainShellFactory);
        LifecycleController lifecycleController =
            new LifecycleController(component.getLifecycleStore(), plainContainerLogic, cDeps.accessor,
                executionContext, hostContext);

        try {
            cDeps.accessor.init(cDeps.componentInstanceId);
        } catch (RegistrationException re) {
            throw new ContainerException("cannot start container, because registry not available",
                re);
        }

        //todo: implement this also for LifecycleContainers!?
        ContainerController containerController =
            new ErrorAwareContainer<>(cDeps.componentInstanceId, plainContainerLogic, cDeps.networkHandler,
                lifecycleController, cDeps.accessor);

        this.registry.addContainer(containerController);
        containerController.create();
        return containerController;
    }

    @Override public void terminate() {
        try {
            this.hostContext.close();
        } catch (InterruptedException ie) {
            LOGGER.warn("shutting down interrupted");
        }
        LOGGER.error(
            "terminate has not been fully implemented, closing further parts need to be implemented!");
        // FIXME: add other parts to shut down //
    }

    @Override public List<ComponentInstanceId> getAllContainers() {
        return registry.listComponentInstances();
    }

    @Override public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) {
        ContainerController dc = registry.getContainer(cid);
        return dc.getState();
    }
}
