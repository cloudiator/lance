package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleActionInterceptor;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

import java.io.IOException;

/**
 * Created by Daniel Seybold on 10.08.2015.
 */
public class PlainContainerLogic implements ContainerLogic, LifecycleActionInterceptor {

    private final ComponentInstanceId myId;
    private final DeployableComponent deployableComponent;
    private final DeploymentContext deploymentContext;
    private final OperatingSystem os;
    private final NetworkHandler networkHandler;
    private final PlainShellFactory plainShellFactory;

    PlainContainerLogic(ComponentInstanceId id, DeployableComponent deployableComponent, DeploymentContext deploymentContext,
                        OperatingSystem os, NetworkHandler networkHandler, PlainShellFactory plainShellFactory){

        this.myId = id;
        this.deployableComponent = deployableComponent;
        this.deploymentContext = deploymentContext;
        this.os = os;
        this.networkHandler = networkHandler;
        this.plainShellFactory = plainShellFactory;
    }

    @Override
    public void doCreate() throws ContainerException {
        //prepare the component folder
        ProcessBuilder builder = new ProcessBuilder( "mkdir", this.myId.toString());

        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doInit(LifecycleStore store) throws ContainerException {
        //possibly not needed for plain container
    }

    @Override
    public void completeInit() throws ContainerException {

    }

    @Override
    public void doDestroy() throws ContainerException {

    }

    @Override
    public String getLocalAddress() throws ContainerException {
        return null;
    }

    /**
     * loops the port numbers through <a,b> -> <(a,a) , (b,b)>
     * @return
     */
    @Override
    public InportAccessor getPortMapper() {
        return ( (portName, clientState) -> {

                Integer portNumber = (Integer) deploymentContext.getProperty(portName, InPort.class);
                clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_0, portNumber);
                clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_1, portNumber);
                clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_2, portNumber);

        });
    }

    @Override
    public void prepare(LifecycleHandlerType type) {

    }

    @Override
    public void postprocess(LifecycleHandlerType type) {

    }

    @Override
    public ComponentInstanceId getComponentId() {
        return this.myId;
    }
}
