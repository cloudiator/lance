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
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.PowershellExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShellImpl;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleActionInterceptor;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Daniel Seybold on 10.08.2015.
 */
public class PlainContainerLogic implements ContainerLogic, LifecycleActionInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainContainerLogic.class);

    private final ComponentInstanceId myId;
    private final DeployableComponent deployableComponent;
    private final DeploymentContext deploymentContext;
    private final OperatingSystem os;
    private final NetworkHandler networkHandler;
    private final PlainShellFactory plainShellFactory;
    private final HostContext hostContext;

    PlainContainerLogic(ComponentInstanceId id, DeployableComponent deployableComponent,
        DeploymentContext deploymentContext, OperatingSystem os, NetworkHandler networkHandler,
        PlainShellFactory plainShellFactory, HostContext hostContext) {

        this.myId = id;
        this.deployableComponent = deployableComponent;
        this.deploymentContext = deploymentContext;
        this.os = os;
        this.networkHandler = networkHandler;
        this.plainShellFactory = plainShellFactory;
        this.hostContext = hostContext;
    }

    @Override public void doCreate() throws ContainerException {
        LOGGER.info("Creating shell for operating system: " + this.os.toString());
        PlainShell plainShell = new PlainShellImpl(this.os);

        LOGGER.info("creating new plain container with foldername " + this.myId.toString());
        plainShell.executeCommand("mkdir " + this.myId.toString());

        String switchFolderCommand =
            System.getProperty("user.dir") + System.getProperty("file.separator") + this.myId
                .toString();
        LOGGER.info("Switching to plain container: " + switchFolderCommand);
        plainShell.setDirectory(switchFolderCommand);

        //installing shell
        this.plainShellFactory.installPlainShell(plainShell);

    }

    @Override public void doInit(LifecycleStore store) throws ContainerException {
        //probably not needed for plain container

    }


    @Override public void completeInit() throws ContainerException {

        this.plainShellFactory.closeShell();
    }

    @Override public void doDestroy() throws ContainerException {
        throw new UnsupportedOperationException();
    }

    @Override public String getLocalAddress() throws ContainerException {

        return hostContext.getInternalIp();

    }

    /**
     * loops the port numbers through <a,b> -> <(a,a) , (b,b)>
     *
     * @return
     */
    @Override public InportAccessor getPortMapper() {
        return ((portName, clientState) -> {

            Integer portNumber = (Integer) deploymentContext.getProperty(portName, InPort.class);
            clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_0, portNumber);
            clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_1, portNumber);
            clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_2, portNumber);

        });
    }

    @Override public void prepare(LifecycleHandlerType type) {
        if (type == LifecycleHandlerType.INSTALL) {
            preInstallAction();
        }

    }

    private void preInstallAction() {
        PlainShellWrapper plainShellWrapper = this.plainShellFactory.createShell();

        //todo: move os switch to a central point (currently here and in PlainShellImpl)
        if (this.os.equals(OperatingSystem.WINDOWS_7)) {

            PowershellExportBasedVisitor visitor =
                new PowershellExportBasedVisitor(plainShellWrapper.plainShell);
            networkHandler.accept(visitor);
            this.deployableComponent.accept(this.deploymentContext, visitor);

        } else if (this.os.equals(OperatingSystem.UBUNTU_14_04)) {
            BashExportBasedVisitor visitor =
                new BashExportBasedVisitor(plainShellWrapper.plainShell);

            networkHandler.accept(visitor);
            this.deployableComponent.accept(this.deploymentContext, visitor);

        } else {
            throw new RuntimeException("Unsupported Operating System: " + this.os.toString());
        }

    }

    @Override public void postprocess(LifecycleHandlerType type) {
        if (type == LifecycleHandlerType.PRE_INSTALL) {
            postPreInstall();
        } else if (type == LifecycleHandlerType.POST_INSTALL) {
            // TODO: how should we snapshot the folder? //
        }
    }

    private void postPreInstall() {
        //FIXME: not necessary for plain container
    }

    @Override public ComponentInstanceId getComponentId() {
        return this.myId;
    }
}
