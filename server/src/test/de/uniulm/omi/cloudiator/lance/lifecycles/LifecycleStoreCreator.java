package de.uniulm.omi.cloudiator.lance.lifecycles;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.lifecycle.HandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStoreBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DefaultDetectorFactories;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StartDetector;

public class LifecycleStoreCreator {

	private LifecycleStoreBuilder builder = new LifecycleStoreBuilder();
	
	public volatile Map<HandlerType, TestLifecycleHandler> mapping = new HashMap<>();
	
	private void addToMapping(HandlerType t, TestLifecycleHandler h) {
		mapping.put(t, h);
	}
	
	private <T> T createProxy(TestLifecycleHandler h, Class<T> type) {
		Object o = Proxy.newProxyInstance(h.getClass().getClassLoader(), new Class<?>[] {
			type}, h );
		if(! type.isAssignableFrom(o.getClass())) {
			throw new RuntimeException(o.getClass().toString() + " vs " + type);
		}
		return (T) o;
	}
	
	public LifecycleStoreCreator addDefaultStartDetector() {
		return addDefaultStartDetector(1);
	}
	
	public LifecycleStoreCreator addDefaultStartDetector(int i) {
		TestLifecycleHandler h = new TestLifecycleHandler(DetectorType.START, i, false);
		StartDetector hh = createProxy(h, StartDetector.class);
		builder.setStartDetector(hh);
		addToMapping(DetectorType.START, h);
		return this;
	}

	public LifecycleStore build() {
		return builder.build();
	}

	public LifecycleStoreCreator addEmptyStartHandler() {
		TestLifecycleHandler h = new TestLifecycleHandler(LifecycleHandlerType.START);
		LifecycleHandler hh = createProxy(h, LifecycleHandlerType.START.getTypeClass());
		builder.setHandler(hh, LifecycleHandlerType.START);
		addToMapping(LifecycleHandlerType.START, h);
		return this;
	}
	
	public LifecycleStoreCreator addLifecycleHandler(LifecycleHandlerType type, boolean throwsException) {
		TestLifecycleHandler h = new TestLifecycleHandler(type, throwsException);
		LifecycleHandler hh = createProxy(h, type.getTypeClass());
		builder.setHandler(hh, type);
		addToMapping(type, h);
		return this;
	}

	public boolean checkHandlerHasBeenInvoked(HandlerType type, int i) {
		TestLifecycleHandler h = mapping.get(type);
		return h.wasCalled(i);
	}

}
