package de.uniulm.omi.cloudiator.lance.lifecycles;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.junit.BeforeClass;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.containers.dummy.DummyContainer;
import de.uniulm.omi.cloudiator.lance.lca.containers.dummy.DummyInterceptor;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lca.EnvContextWrapper;
import de.uniulm.omi.cloudiator.lance.container.standard.ErrorAwareContainer;

public class ErrorAwareContainerLifecycleTest {

	public volatile DummyInterceptor interceptor;
	public volatile DummyContainer container;
	private volatile CoreElements core;
	public volatile LifecycleStoreCreator creator;
	private volatile ExecutionContext ctx;
	
	private static final int DEFAULT_PROPERTIES = 5;
	private static final String INITIAL_LOCAL_ADDRESS = "<unknown>";
	
	private volatile Map<ComponentInstanceId, Map<String, String>> dumb;
	
	@BeforeClass
	public static void configureHostContext() {
		CoreElements.initStatic();
	}
	
	private void init(boolean creatRegistry) {
		dumb = null;
		core = new CoreElements(creatRegistry);
		container = new DummyContainer(new String[] {null, EnvContextWrapper.getContainerIp()},
				new Object[]{ new int[] {11, 12, 13}});
		ctx = new ExecutionContext(null, null);
		creator = new LifecycleStoreCreator();
		creator.addDefaultStartDetector();
	}
	
	private ErrorAwareContainer<DummyContainer> linkControllers() {
		interceptor = new DummyInterceptor();
		LifecycleController lcc = new LifecycleController(creator.build(), interceptor, core.accessor, ctx, null);
		
		ErrorAwareContainer<DummyContainer> containerWrapper = 
				new ErrorAwareContainer<>(CoreElements.componentInstanceId, container,
						core.networkHandler, lcc, core.accessor, false);
		return containerWrapper;
	}

	@Test
	public void testNewContainer() {
		assertNotNull(CoreElements.context);
		init(false);
		ErrorAwareContainer<DummyContainer> containerWrapper = linkControllers();
		assertRightState(ContainerStatus.NEW, containerWrapper);
		assertTrue(container.invocationCount() == 0);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testNewContainerWithoutRegistryInit() throws ContainerException {
		assertNotNull(CoreElements.context);
		init(true);
		ErrorAwareContainer<DummyContainer> containerWrapper = linkControllers();
		containerWrapper.create();
		containerWrapper.awaitCreation();
	}
	
	@Test
	public void testRegistryInitialisation() throws RegistrationException {
		assertNotNull(CoreElements.context);
		assertNull(dumb);
		init(true);
		core.fillRegistry();
		checkBasicRegistryValue(1);
		assertNotNull(dumb);
		Map<String,String> cids = dumb.get(CoreElements.componentInstanceId);
		assertNotNull(cids);
		assertTrue(cids.size() == 1);
		assertTrue(cids.get("Instance_Number") != null);
	}
	
	@Test
	public void testContainerCreation() throws RegistrationException, ContainerException {
		assertNotNull(CoreElements.context);
		init(true);
		core.fillRegistry();
		
		ErrorAwareContainer<DummyContainer> containerWrapper = linkControllers();
		
		containerWrapper.create();
		containerWrapper.awaitCreation();

		assertRightState(ContainerStatus.CREATED, containerWrapper);

		checkBasicRegistryValue(1);
		assertNotNull(dumb);
		
		checkForDumbElements(
				new String[] {"HOST_CONTAINER_IP", "Instance_Number", "HOST_PUBLIC_IP", "HOST_CLOUD_IP", "Container_Status"},
				DEFAULT_PROPERTIES);
		Map<String,String> cids = dumb.get(CoreElements.componentInstanceId);
		assertEquals(INITIAL_LOCAL_ADDRESS, cids.get("HOST_CONTAINER_IP"));
		assertEquals(EnvContextWrapper.getPublicIp(), cids.get("HOST_PUBLIC_IP"));
		assertEquals(EnvContextWrapper.getCloudIp(), cids.get("HOST_CLOUD_IP"));
		assertEquals("1", cids.get("Instance_Number"));
		assertEquals(containerWrapper.getState().toString(), cids.get("Container_Status"));
		assertTrue(container.invocationHistoryMatches(DummyContainer.ContainerCalls.LOCAL_ADDRESSES, DummyContainer.ContainerCalls.CREATE));
	}
	
	@Test
	public void testContainerBootstrap() throws RegistrationException, ContainerException {
		assertNotNull(CoreElements.context);
		init(true);
		core.fillRegistry();
		
		ErrorAwareContainer<DummyContainer> containerWrapper = linkControllers();
		
		containerWrapper.create();
		containerWrapper.awaitCreation();
		containerWrapper.bootstrap();
		containerWrapper.awaitBootstrap();
		
		assertRightState(ContainerStatus.BOOTSTRAPPED, containerWrapper);

		checkBasicRegistryValue(1);
		assertNotNull(dumb);
		
		checkForDumbElements(
				new String[] {"HOST_CONTAINER_IP", "Instance_Number", "HOST_PUBLIC_IP", "HOST_CLOUD_IP", "Container_Status"},
				DEFAULT_PROPERTIES);
		
		Map<String,String> cids = dumb.get(CoreElements.componentInstanceId);
		assertEquals(INITIAL_LOCAL_ADDRESS, cids.get("HOST_CONTAINER_IP"));
		assertEquals(EnvContextWrapper.getPublicIp(), cids.get("HOST_PUBLIC_IP"));
		assertEquals(EnvContextWrapper.getCloudIp(), cids.get("HOST_CLOUD_IP"));
		assertEquals("1", cids.get("Instance_Number"));
				
		assertTrue(container.toString(), container.invocationHistoryMatches(DummyContainer.ContainerCalls.LOCAL_ADDRESSES, 
																			DummyContainer.ContainerCalls.CREATE,
																			DummyContainer.ContainerCalls.INIT,
																			DummyContainer.ContainerCalls.LOCAL_ADDRESSES,
																			DummyContainer.ContainerCalls.PORT_MAP));
		assertEquals(containerWrapper.getState().toString(), cids.get("Container_Status"));
	}
	
	@Test
	public void testBrokenContainerInit() throws RegistrationException, ContainerException {
		assertNotNull(CoreElements.context);
		init(true);
		core.fillRegistry();
		
		ErrorAwareContainer<DummyContainer> containerWrapper = linkControllers();
		
		containerWrapper.create();
		containerWrapper.awaitCreation();
		containerWrapper.bootstrap();
		containerWrapper.awaitBootstrap();
		containerWrapper.init(creator.build());
		containerWrapper.awaitInitialisation();
		// run handler is missing // 
	}
	
	@Test
	public void testContainerInit() throws RegistrationException, ContainerException {
		assertNotNull(CoreElements.context);
		init(true);
		core.fillRegistry();
		
		ErrorAwareContainer<DummyContainer> containerWrapper = linkControllers();
		
		containerWrapper.create();
		containerWrapper.awaitCreation();
		containerWrapper.bootstrap();
		containerWrapper.awaitBootstrap();
		containerWrapper.init(creator.build());
		containerWrapper.awaitInitialisation();
		
		assertRightState(ContainerStatus.READY, containerWrapper);

		checkBasicRegistryValue(1);
		assertNotNull(dumb);
		
		checkForDumbElements(
				new String[] {"HOST_CONTAINER_IP", "Instance_Number", "HOST_PUBLIC_IP", "HOST_CLOUD_IP", "Container_Status"},
				DEFAULT_PROPERTIES);
		
		Map<String,String> cids = dumb.get(CoreElements.componentInstanceId);
		assertEquals(INITIAL_LOCAL_ADDRESS, cids.get("HOST_CONTAINER_IP"));
		assertEquals(EnvContextWrapper.getPublicIp(), cids.get("HOST_PUBLIC_IP"));
		assertEquals(EnvContextWrapper.getCloudIp(), cids.get("HOST_CLOUD_IP"));
		assertEquals("1", cids.get("Instance_Number"));
				
		assertTrue(container.toString(), container.invocationHistoryMatches(DummyContainer.ContainerCalls.LOCAL_ADDRESSES, 
																			DummyContainer.ContainerCalls.CREATE,
																			DummyContainer.ContainerCalls.INIT,
																			DummyContainer.ContainerCalls.LOCAL_ADDRESSES,
																			DummyContainer.ContainerCalls.PORT_MAP));
		assertEquals(containerWrapper.getState().toString(), cids.get("Container_Status"));
	}
	
	private void checkForDumbElements(String[] values, int expectedSize) {
		Map<String,String> cids = dumb.get(CoreElements.componentInstanceId);
		assertNotNull(cids);
		for(String s : values) {
			assertTrue("unknown key: " + s, cids.containsKey(s));
		}
		assertTrue(cids.size() == expectedSize);
	}
	
	private void checkBasicRegistryValue(int compInstances) throws RegistrationException {
		dumb = core.checkBasicRegistryValues();
		assertTrue(dumb.size() == compInstances);
	}
	
	private static void assertRightState(ContainerStatus stat, ErrorAwareContainer<DummyContainer> container) {
		for(ContainerStatus status : ContainerStatus.values()) {
			if(status == stat)
				assertEquals(status, container.getState());
			else
				assertFalse(status == container.getState());		
		}
	}
}
