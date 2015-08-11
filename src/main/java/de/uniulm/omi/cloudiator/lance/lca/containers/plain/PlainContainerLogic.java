package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleActionInterceptor;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

/**
 * Created by Daniel Seybold on 10.08.2015.
 */
public class PlainContainerLogic implements ContainerLogic, LifecycleActionInterceptor {
    @Override
    public void doCreate() throws ContainerException {
        //prepare container env, e.g. create folders
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

    @Override
    public InportAccessor getPortMapper() {
        return null;
    }

    @Override
    public void prepare(LifecycleHandlerType type) {

    }

    @Override
    public void postprocess(LifecycleHandlerType type) {

    }

    @Override
    public ComponentInstanceId getComponentId() {
        return null;
    }
}
