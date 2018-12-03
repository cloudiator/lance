package de.uniulm.omi.cloudiator.lance.util.application;

import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;

import java.io.Serializable;
import java.util.Set;

public class ComponentInfo implements Serializable {

  private static final long serialVersionUID = 8703011548496301301L;
  private final String componentName;
  private final ComponentId componentId;
  private final ComponentInstanceId componentInstanceId;
  private final Set<ProvidedPortInfo> providedPortInfos;
  private final Set<RequiredPortInfo> requiredPortInfos;
  private final OperatingSystem os;

  public String getComponentName() {
        return componentName;
    }

  public ComponentId getComponentId() {
        return componentId;
    }

  public ComponentInstanceId getComponentInstanceId() {
        return componentInstanceId;
    }

  public Set<ProvidedPortInfo> getProvidedPortInfos() {
        return providedPortInfos;
    }

  public Set<RequiredPortInfo> getRequiredPortInfos() {
        return requiredPortInfos;
    }

  public OperatingSystem getOs() {
        return os;
    }

  public ComponentInfo(String componentName, ComponentId componentId, ComponentInstanceId componentInstanceId, Set<ProvidedPortInfo> providedPortInfos, Set<RequiredPortInfo> requiredPortInfos, OperatingSystem os) {
    this.componentName = componentName;
    this.componentId = componentId;
    this.componentInstanceId = componentInstanceId;
    this.providedPortInfos = providedPortInfos;
    this.requiredPortInfos = requiredPortInfos;
    this.os = os;
  }
}
