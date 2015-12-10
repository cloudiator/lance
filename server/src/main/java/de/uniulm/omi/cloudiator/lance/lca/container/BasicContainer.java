package de.uniulm.omi.cloudiator.lance.lca.container;

import java.util.List;

public interface BasicContainer {
	void terminate();
    /**
     * 
     * @param id the id of the component instance we are looking for
     * @return null if a container with this Id does not exist
     */
    ContainerController getContainer(ComponentInstanceId id);
	List<ComponentInstanceId> getAllContainers();
	ContainerStatus getComponentContainerStatus(ComponentInstanceId cid);
}
