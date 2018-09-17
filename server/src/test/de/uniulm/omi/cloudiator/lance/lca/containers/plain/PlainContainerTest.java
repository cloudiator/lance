package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import java.util.Map;

import org.junit.Test;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ErrorAwareContainer;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.containers.dummy.DummyInterceptor;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycles.CoreElements;
import de.uniulm.omi.cloudiator.lance.lifecycles.LifecycleStoreCreator;

public class PlainContainerTest {

	public volatile DummyInterceptor interceptor;
	public volatile PlainContainerLogic containerLogic;
	private volatile CoreElements core;
	public volatile LifecycleStoreCreator creator;
	private volatile ExecutionContext ctx;
	
	private volatile ErrorAwareContainer<PlainContainerLogic> controller;
	
	private static final int DEFAULT_PROPERTIES = 5;
	private static final String INITIAL_LOCAL_ADDRESS = "<unknown>";
	
	private volatile Map<ComponentInstanceId, Map<String, String>> dumb;
	
	
	private void init(boolean creatRegistry) {
		dumb = null;
		core = new CoreElements(creatRegistry);
		ctx = new ExecutionContext(OperatingSystem.WINDOWS_7, null);
		creator = new LifecycleStoreCreator();
		creator.addDefaultStartDetector();
	}
	
    /* copied from PlainContainerManager createNewContainer*/
    private void createNewContainer() throws ContainerException {

            PlainShellFactory plainShellFactory = new TestPlainShellFactory();

            GlobalRegistryAccessor accessor =
                new GlobalRegistryAccessor(core.ctx, core.comp, CoreElements.componentInstanceId);

            NetworkHandler networkHandler = core.networkHandler;
            PlainContainerLogic.Builder builder = new PlainContainerLogic.Builder();
            //split for better readability
            builder = builder.cInstId(CoreElements.componentInstanceId).deplComp(core.comp).deplContext(core.ctx).operatingSys(ctx.getOperatingSystem());
            containerLogic = builder.nwHandler(networkHandler).plShellFac(plainShellFactory).hostContext(CoreElements.context).build();

            ExecutionContext executionContext = new ExecutionContext(ctx.getOperatingSystem(), plainShellFactory);
            LifecycleController lifecycleController =
                new LifecycleController(core.comp.getLifecycleStore(), containerLogic, accessor,
                    executionContext, CoreElements.context);

            try {
                accessor.init(CoreElements.componentInstanceId);
            } catch (RegistrationException re) {
                throw new ContainerException("cannot start container, because registry not available",
                    re);
            }

            controller =
                new ErrorAwareContainer<PlainContainerLogic>(CoreElements.componentInstanceId, containerLogic, networkHandler,
                    lifecycleController, accessor);

            controller.create();
        }
    
    @Test 
    public void testInit() throws ContainerException{
    	init(true);
    	createNewContainer();
    }
}
