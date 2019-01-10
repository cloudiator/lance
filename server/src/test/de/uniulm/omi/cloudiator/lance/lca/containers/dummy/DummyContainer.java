package de.uniulm.omi.cloudiator.lance.lca.containers.dummy;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycles.ResponseHistory;

public class DummyContainer implements ContainerLogic {

	private volatile int invocationCounter = 0;
	private final EnumMap<ContainerCalls, ResponseHistory<?>> calls = new EnumMap<>(ContainerCalls.class);
	private final List<ContainerCalls> callStack = new ArrayList<>();
	
	public DummyContainer(String[] local, Object[] ports) {
		calls.put(ContainerCalls.LOCAL_ADDRESSES, ResponseHistory.forStringMethods(local));
		calls.put(ContainerCalls.CREATE, ResponseHistory.forVoidMethods());
		calls.put(ContainerCalls.INIT, ResponseHistory.forVoidMethods());
		calls.put(ContainerCalls.COMPLETE_INIT, ResponseHistory.forVoidMethods());
		calls.put(ContainerCalls.PORT_MAP, ResponseHistory.forObjectMethods(ports));
	}
	
	/* interface methods */
	@Override
	public void doCreate() throws ContainerException {
		invocationCounter++;
		callStack.add(ContainerCalls.CREATE);
		calls.get(ContainerCalls.CREATE).getNext();
	}

	@Override
	public void doInit(LifecycleStore store) throws ContainerException {
		invocationCounter++;
		callStack.add(ContainerCalls.INIT);
		calls.get(ContainerCalls.INIT).getNext();
	}

	@Override
	public void preInit() throws ContainerException {
		invocationCounter++;
		callStack.add(ContainerCalls.COMPLETE_INIT);
		calls.get(ContainerCalls.COMPLETE_INIT).getNext();
	}

	@Override
	public void completeShutDown() throws ContainerException {

	}

  @Override
	public void doDestroy(boolean forceShutdown, boolean remove) throws ContainerException {
		invocationCounter++;
		callStack.add(ContainerCalls.OTHER);
		throw new UnsupportedOperationException();
	}

	@Override
	public void preDestroy() throws ContainerException {

	}

	@Override
	public String getLocalAddress() throws ContainerException {
		invocationCounter++;
		callStack.add(ContainerCalls.LOCAL_ADDRESSES);
		String ret = (String) calls.get(ContainerCalls.LOCAL_ADDRESSES).getNext();
		return ret;
	}

	@Override
	public InportAccessor getPortMapper() {
		invocationCounter++;
		callStack.add(ContainerCalls.PORT_MAP);
		Object o = calls.get(ContainerCalls.PORT_MAP).getNext();
		final int[] mapping = (int[]) o;
        return ( (portName, clientState) -> {	
        	clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_0, mapping[0]);
        	clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_1, mapping[1]);
        	clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_2, mapping[2]);
        });
	}

	public void setStaticEnvironment() throws ContainerException {

	}

	/* validation methods */
	
	public enum ContainerCalls {
		LOCAL_ADDRESSES,
		CREATE,
		INIT,
		PORT_MAP,
		COMPLETE_INIT,
		OTHER,
		;
	}
	
	public int invocationCount() {
		return invocationCounter;
	}
	
	public int getInvocationCount(EnumSet<ContainerCalls> callSet) {
		int counter = 0;
		for(ContainerCalls cc : callSet) {
			ResponseHistory<?> rh = calls.get(cc);
			counter = counter + rh.getCount();
		}
		return counter;
	}
	
	public boolean invocationHistoryMatches(ContainerCalls ... expected) {
		if(expected == null || expected.length == 0) {
			throw new IllegalStateException();
		}
		List<ContainerCalls> copy = new LinkedList<>(callStack);
		
		for(ContainerCalls cc : expected) {
			if(copy.isEmpty()) 
				return false;
			ContainerCalls _c = copy.remove(0);
			if(cc != _c) return false;
		}
		if(!copy.isEmpty()) 
			return false;
		return true;
	}
	
	public void printInvocations() {
		System.out.println(this);
	}
	
	@Override
	public String toString() {
		return calls + "-->" + callStack.toString();
	}
}
