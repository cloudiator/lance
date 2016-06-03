package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.ConnectorFactory;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.dummy.DummyInterceptor;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.HandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;
import de.uniulm.omi.cloudiator.lance.lifecycles.CoreElements;
import de.uniulm.omi.cloudiator.lance.lifecycles.LifecycleStoreCreator;

public class DockerSnapshottingTest {

	public volatile DummyInterceptor interceptor;
	public volatile LifecycleController lcc;
	public volatile CoreElements core;
	public volatile LifecycleStoreCreator creator;
	private volatile ExecutionContext ctx;
	
	private volatile Map<ComponentInstanceId, Map<String, String>> dumb;
	private volatile DockerContainerManager manager;
	
	static class FakeHostContext implements HostContext {

		@Override
		public String getPublicIp() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getInternalIp() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getCloudIdentifier() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws InterruptedException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void run(Runnable runner) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> scheduleAction(Runnable runner) {
			throw new UnsupportedOperationException();
		}
		
	}
	
	static String hostname = "localhost";
	
	@BeforeClass
	public static void configureHostContext() {
		CoreElements.initStatic();
	}
	
	@Before
	public void init() {
		System.setProperty("host.docker.registry.port", Integer.toString(5000));
		System.setProperty("host.docker.registry.host", "134.60.64.242");
		System.setProperty("host.docker.registry.use", Boolean.toString(true));
		
		core = new CoreElements(false);
		manager = new DockerContainerManager(new FakeHostContext());
	}
	
	@Test
	public void testCreation() throws ContainerException {
		
		DockerConfiguration dockerConfig = DockerConfiguration.INSTANCE;
		DockerConnector client = ConnectorFactory.INSTANCE.createConnector(hostname);
		DockerShellFactory shellFactory = new DockerShellFactory();
		DockerContainerLogic logic = new DockerContainerLogic(core.componentInstanceId, 
									client, core.comp, core.ctx, OperatingSystem.UBUNTU_14_04, 
									core.networkHandler, shellFactory, dockerConfig);
		logic.doCreate();
	}
}
