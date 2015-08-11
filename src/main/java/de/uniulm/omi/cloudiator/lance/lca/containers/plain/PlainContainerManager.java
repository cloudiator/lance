package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.*;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandSequence;

import java.util.List;

/**
 * Created by Daniel Seybold on 10.08.2015.
 */
public class PlainContainerManager implements ContainerManager {

    public PlainContainerManager(HostContext vmId){

    }

    @Override
    public ContainerType getContainerType() {
        return null;
    }

    @Override
    public ContainerController getContainer(ComponentInstanceId id) {
        return null;
    }

    /*
    @Override
    public void runApplication(ComponentInstanceId id, LifecycleStore store) {

    }

    @Override
    public CommandSequence addDeploymentSequence() {
        return null;
    }*/

    @Override
    public ContainerController createNewContainer(DeploymentContext ctx, DeployableComponent component, OperatingSystem os) throws ContainerException {
        return null;
    }

    @Override
    public void terminate() {

    }

    @Override
    public List<ComponentInstanceId> getAllContainers() {
        return null;
    }

    @Override
    public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) {
        return null;
    }
}
