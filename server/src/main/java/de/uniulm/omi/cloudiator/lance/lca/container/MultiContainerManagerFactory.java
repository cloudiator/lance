package de.uniulm.omi.cloudiator.lance.lca.container;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;


/**
 * Created by Daniel Seybold on 23.09.2015.
 */
public class MultiContainerManagerFactory {



    public static ContainerManager createContainerManager(HostContext contex, OperatingSystem operatingSystem, ContainerType containerType){

        switch (operatingSystem.getFamily()){
            case WINDOWS: return createWindowsContainerManager(contex);

            case LINUX: return createLinuxContainerManager(contex, containerType);

            default: throw new IllegalStateException("No matching Operating System found: " + operatingSystem.toString());
        }



    }

    private static ContainerManager createWindowsContainerManager(HostContext hostContext){
        return ContainerManagerFactory.createContainerManager(hostContext, ContainerType.PLAIN);
    }

    private static ContainerManager createLinuxContainerManager(HostContext hostContext, ContainerType containerType){

        switch (containerType){
            case DOCKER: return ContainerManagerFactory.createContainerManager(hostContext, ContainerType.DOCKER);

            case PLAIN:  return ContainerManagerFactory.createContainerManager(hostContext, ContainerType.PLAIN);

            default: throw new IllegalStateException("No matching Container " + containerType.toString() + " for Operating System Linux found!");
        }

    }
}
