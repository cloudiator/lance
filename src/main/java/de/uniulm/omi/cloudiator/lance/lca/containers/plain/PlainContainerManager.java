package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.*;
import de.uniulm.omi.cloudiator.lance.lca.container.registry.ContainerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Daniel Seybold on 10.08.2015.
 */
public class PlainContainerManager implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManager.class);
    private final ContainerRegistry registry = new ContainerRegistry();

    public PlainContainerManager(HostContext vmId){

    }

    @Override
    public ContainerType getContainerType() {
        return ContainerType.PLAIN;
    }

    @Override
    public ContainerController getContainer(ComponentInstanceId id) {
        return this.registry.getContainer(id);
    }

    @Override
    public ContainerController createNewContainer(DeploymentContext ctx, DeployableComponent component, OperatingSystem os) throws ContainerException {

        //fixme: implement this
        return null;
    }

    @Override
    public void terminate() {
        //fixme: implement this
    }

    @Override
    public List<ComponentInstanceId> getAllContainers() {
        return registry.listComponentInstances();
    }

    @Override
    public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) {
        ContainerController dc = registry.getContainer(cid);
        return dc.getState();
    }
}
