package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;

public class AbstractComponentDependencies {

  public final ComponentInstanceId componentInstanceId;
  public final GlobalRegistryAccessor accessor;
  public final NetworkHandler networkHandler ;

  public AbstractComponentDependencies(DeploymentContext ctx, AbstractComponent component, HostContext hostContext) {
    componentInstanceId = new ComponentInstanceId();
    accessor = new GlobalRegistryAccessor(ctx, component, componentInstanceId);
    networkHandler = new NetworkHandler(accessor, component, hostContext);
  }
}
