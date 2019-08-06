package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.ConnectorFactory;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.dummy.DummyInterceptor;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
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
		public String getCloudIp() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getContainerIp() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPrivateIp() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getTenantId() {
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
		public String getVMIdentifier() {
			throw new UnsupportedOperationException();
		}

    @Override
    public String getFailFast() {
      throw new UnsupportedOperationException();
    }

    @Override
		public Map<String,String> getEnvVars() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws InterruptedException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Future<?> run(Runnable runner) {
			return null;
		}

		@Override
		public <T> Future<T> run(Callable<T> callable) {
			return null;
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
    OperatingSystem os = new OperatingSystemImpl(
        OperatingSystemFamily.UBUNTU,
        OperatingSystemArchitecture.AMD64,
        OperatingSystemVersions.of(1604, "16.04"));
		LifecycleDockerContainerLogic.Builder builder = new LifecycleDockerContainerLogic.Builder(os);
		//split for better readability
		AbstractDockerContainerLogic logic = builder.cInstId(core.componentInstanceId).dockerConnector(client).
				deplComp(core.comp).deplContext(core.ctx).
				nwHandler(core.networkHandler).dockerShellFac(shellFactory).dockerConfig(dockerConfig).
				hostContext(new FakeHostContext()).build();
		logic.doCreate();
		logic.doDestroy(true, false);
	}
	
	
	@Test
	public void testSnapshot() throws ContainerException {
		
		DockerConfiguration dockerConfig = DockerConfiguration.INSTANCE;
		DockerConnector client = ConnectorFactory.INSTANCE.createConnector(hostname);
		DockerShellFactory shellFactory = new DockerShellFactory();
    OperatingSystem os = new OperatingSystemImpl(
        OperatingSystemFamily.UBUNTU,
        OperatingSystemArchitecture.AMD64,
        OperatingSystemVersions.of(1604, "16.04"));
		LifecycleDockerContainerLogic.Builder builder = new LifecycleDockerContainerLogic.Builder(os);
		//split for better readability
		AbstractDockerContainerLogic logic = builder.cInstId(core.componentInstanceId).dockerConnector(client).
				deplComp(core.comp).deplContext(core.ctx).
				nwHandler(core.networkHandler).dockerShellFac(shellFactory).dockerConfig(dockerConfig).
				hostContext(new FakeHostContext()).build();
		logic.doCreate();
		logic.prepare(LifecycleHandlerType.PRE_INSTALL);
		logic.postprocess(LifecycleHandlerType.PRE_INSTALL);
		logic.doDestroy(true, false);

		LifecycleDockerContainerLogic.Builder builder2 = new LifecycleDockerContainerLogic.Builder(os);
		//split for better readability
		AbstractDockerContainerLogic logic2 = builder2.cInstId(core.componentInstanceId).dockerConnector(client).
				deplComp(core.comp).deplContext(core.ctx).
				nwHandler(core.networkHandler).dockerShellFac(shellFactory).dockerConfig(dockerConfig).
				hostContext(new FakeHostContext()).build();
	}
}
