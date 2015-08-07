package de.uniulm.omi.cloudiator.lance.lca.container.port;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;

public interface InportAccessor {

	void accessPort(String portName, HierarchyLevelState<Integer> clientState) throws ContainerException;

}
