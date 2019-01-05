package de.uniulm.omi.cloudiator.lance.util.application;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import java.util.HashSet;
import java.util.Set;

public final class AppArchitectureBuilder {
  private final String applicationName;
  private final ApplicationId applicationId;
  private final ApplicationInstanceId appInstanceId;

  private final Set<ComponentInfo> componentInfos = new HashSet<>();

  public AppArchitectureBuilder(
      String applicationName, ApplicationId applicationId, ApplicationInstanceId appInstanceId) {
    this.applicationName = applicationName;
    this.applicationId = applicationId;
    this.appInstanceId = appInstanceId;
  }

  public AppArchitectureBuilder addComponentInfo(ComponentInfo cInfo) {
    componentInfos.add(cInfo);
    return this;
  }

  public AppArchitecture build() {
    return new AppArchitecture(applicationName, applicationId, appInstanceId, componentInfos);
  }

  public AppArchitectureBuilder addAllComponents(ComponentInfo[] compInfos) {
    for (ComponentInfo c : compInfos) {
      addComponentInfo(c);
    }
    return this;
  }
}
