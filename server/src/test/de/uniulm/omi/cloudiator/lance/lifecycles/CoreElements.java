package de.uniulm.omi.cloudiator.lance.lifecycles;

import static org.junit.Assert.assertTrue;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortType;
import de.uniulm.omi.cloudiator.lance.lca.EnvContextWrapper;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.dummy.DummyRegistry;
import java.util.Map;

public class CoreElements {

  public static final String GHOST_COMPONENT_NAME = "ghost-blog";
  public static final ComponentId GHOST_COMPONENT_ID =
      ComponentId.fromString("e07eb01f-9b77-4443-bca6-67116126dac3");
  public static final String GHOST_IN_PORT_NAME = "GHOST_INPORT";

  public static final int GHOST_DEFAULT_IN_PORT = 2368;

  public static volatile ApplicationInstanceId appInstanceId;
  public static volatile ApplicationId appId;
  public static volatile DeployableComponentBuilder componentBuilder;
  public static volatile ComponentInstanceId componentInstanceId;
  public static volatile ComponentId componentId;
  public static volatile HostContext context;

  public volatile DeploymentContext ctx;
  public volatile LcaRegistry reg;
  public volatile DeployableComponent comp;
  public volatile GlobalRegistryAccessor accessor;
  public volatile NetworkHandler networkHandler;

  public CoreElements(boolean createRegistry) {
    reg = createRegistry ? new DummyRegistry() : null;
    ctx = new DeploymentContext(appId, appInstanceId, reg);
    ctx.setProperty(GHOST_IN_PORT_NAME, GHOST_DEFAULT_IN_PORT, InPort.class);
    comp = componentBuilder.build();
    accessor = new GlobalRegistryAccessor(ctx, comp, componentInstanceId);
    networkHandler = new NetworkHandler(accessor, comp, context);
  }

  public static void initStatic() {
    appId = new ApplicationId();
    appInstanceId = new ApplicationInstanceId();
    context = EnvContextWrapper.create();
    componentInstanceId = new ComponentInstanceId();
    componentId = new ComponentId();
    componentBuilder =
        DeployableComponentBuilder.createBuilder("jUnitTestComponent", CoreElements.componentId);
    componentBuilder.addInport(
        GHOST_IN_PORT_NAME,
        PortType.PUBLIC_PORT,
        PortProperties.INFINITE_CARDINALITY,
        GHOST_DEFAULT_IN_PORT);
  }

  public void fillRegistry() throws RegistrationException {
    reg.addApplicationInstance(appInstanceId, appId, "myTestApplication");
    reg.addComponent(appInstanceId, componentId, "myTestComponent");
    reg.addComponentInstance(appInstanceId, componentId, componentInstanceId);
  }

  public Map<ComponentInstanceId, Map<String, String>> checkBasicRegistryValues()
      throws RegistrationException {
    assertTrue(reg.applicationInstanceExists(appInstanceId));
    assertTrue(reg.applicationComponentExists(appInstanceId, componentId));
    return reg.dumpComponent(appInstanceId, componentId);
  }
}
