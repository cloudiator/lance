package de.uniulm.omi.cloudiator.lance.lifecycles;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.containers.dummy.DummyInterceptor;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.HandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;

public class ComponentInstanceLifecycleTest {

	public volatile DummyInterceptor interceptor;
	public volatile LifecycleController lcc;
	public volatile CoreElements core;
	public volatile LifecycleStoreCreator creator;
	private volatile ExecutionContext ctx;
	
	private volatile Map<ComponentInstanceId, Map<String, String>> dumb;
	
	@BeforeClass
	public static void configureHostContext() {
		CoreElements.initStatic();
	}
	
	private void bootstrap(boolean createRegistry) throws RegistrationException {
		interceptor = new DummyInterceptor();
		core = new CoreElements(createRegistry);
		creator = new LifecycleStoreCreator();
		if(createRegistry)
			core.fillRegistry();
		ctx = new ExecutionContext(null, null);
	}
	
	private void createLifecycleController(boolean useCreator) {
		if(useCreator) {
			lcc = new LifecycleController(creator.build(), interceptor, core.accessor, ctx);
		} else {
			lcc = new LifecycleController(null, interceptor, core.accessor, ctx);
		}
	}
	
	@Test(expected=NullPointerException.class)
	public void initWithNullStore() throws RegistrationException {
		bootstrap(false);
		createLifecycleController(false);
		lcc.blockingInit();
	}
	
	@Test(expected=NullPointerException.class)
	public void createEmptyStore() throws RegistrationException {
		bootstrap(false);
		creator.build();
	}
	
	@Test(expected=NullPointerException.class)
	public void createStoreWithoutStartDetector() throws RegistrationException {
		bootstrap(false);
		creator.addEmptyStartHandler();
		creator.build();
	}
	
	@Test
	public void initWithMinimalStore() throws RegistrationException {
		bootstrap(true);
		creator.addEmptyStartHandler();
		creator.addDefaultStartDetector();
		createLifecycleController(true);
		lcc.blockingInit();
		
		assertEquals(2, interceptor.handlerCalls());
		assertEquals(Arrays.asList(new HandlerType[]{LifecycleHandlerType.NEW}), 
						interceptor.invokedHandlers());
		checkBasicRegistryValue(1, LifecycleHandlerType.NEW);
	}
	
	@Test
	public void installWithMinimalStore() throws RegistrationException {
		bootstrap(true);
		creator.addEmptyStartHandler();
		creator.addDefaultStartDetector();
		createLifecycleController(true);
		lcc.blockingInit();
		lcc.blockingInstall();
		
		assertEquals(6, interceptor.handlerCalls());
		assertEquals(Arrays.asList(new HandlerType[]{
				LifecycleHandlerType.NEW, LifecycleHandlerType.INIT, LifecycleHandlerType.PRE_INSTALL, }), 
						interceptor.invokedHandlers());
		checkBasicRegistryValue(1, LifecycleHandlerType.PRE_INSTALL);
	}
	
	@Test
	public void configureWithMinimalStore() throws RegistrationException {
		bootstrap(true);
		creator.addEmptyStartHandler();
		creator.addDefaultStartDetector();
		createLifecycleController(true);
		lcc.blockingInit();
		lcc.blockingInstall();
		lcc.blockingConfigure();
		
		assertEquals(10, interceptor.handlerCalls());
		assertEquals(Arrays.asList(new HandlerType[]{
				LifecycleHandlerType.NEW, LifecycleHandlerType.INIT, LifecycleHandlerType.PRE_INSTALL, LifecycleHandlerType.INSTALL, LifecycleHandlerType.POST_INSTALL}), 
						interceptor.invokedHandlers());
		checkBasicRegistryValue(1, LifecycleHandlerType.POST_INSTALL);
	}
	
	@Test
	public void startWithMinimalStore() throws RegistrationException, LifecycleException {
		bootstrap(true);
		creator.addEmptyStartHandler();
		creator.addDefaultStartDetector();
		createLifecycleController(true);
		lcc.blockingInit();
		lcc.blockingInstall();
		lcc.blockingConfigure();
		lcc.blockingStart();
		
		assertEquals(16, interceptor.handlerCalls());
		System.out.println(interceptor.invokedHandlers());
		assertEquals(Arrays.asList(new HandlerType[]{
				LifecycleHandlerType.NEW, LifecycleHandlerType.INIT, LifecycleHandlerType.PRE_INSTALL, 
				LifecycleHandlerType.INSTALL, LifecycleHandlerType.POST_INSTALL, LifecycleHandlerType.PRE_START,
				DetectorType.START, LifecycleHandlerType.START}), 
						interceptor.invokedHandlers());
		checkBasicRegistryValue(1, LifecycleHandlerType.START);
		
		assertTrue("handler was not called", creator.checkHandlerHasBeenInvoked(LifecycleHandlerType.START));
		assertTrue("handler was not called", creator.checkHandlerHasBeenInvoked(DetectorType.START));
	}

	private void checkBasicRegistryValue(int compInstances, LifecycleHandlerType expectedType) throws RegistrationException {
		dumb = core.checkBasicRegistryValues();
		assertTrue(dumb.size() == compInstances);
		Map<String,String> cids = dumb.get(CoreElements.componentInstanceId);
		assertEquals(expectedType.toString(), cids.get("Component_Instance_Status"));
	}
}
