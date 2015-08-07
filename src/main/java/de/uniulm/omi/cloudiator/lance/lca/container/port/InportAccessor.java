package de.uniulm.omi.cloudiator.lance.lca.container.port;

public interface InportAccessor<T extends Exception> {

	void accessPort(String portName, HierarchyLevelState<Integer> clientState) throws T;

}
