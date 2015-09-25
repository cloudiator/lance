package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.container.SpecificContainerManagerFactory;

/**
 * Created by Daniel Seybold on 10.08.2015.
 */
public enum PlainContainerManagerFactory implements SpecificContainerManagerFactory {

    INSTANCE {
            @Override
            public ContainerManager createContainerManager (HostContext vmId){
                return new PlainContainerManager(vmId);
            }
    }
}
