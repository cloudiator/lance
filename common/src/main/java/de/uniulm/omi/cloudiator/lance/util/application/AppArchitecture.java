package de.uniulm.omi.cloudiator.lance.util.application;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;

import java.io.Serializable;
import java.util.Set;

public class AppArchitecture implements Serializable {

    private static final long serialVersionUID = 626605105815925644L;
    private final String applicationName;
    private final ApplicationId applicationId;
    private final ApplicationInstanceId appInstanceId;
    private final Set<ComponentInfo> components;

    AppArchitecture(String applicationName, ApplicationId applicationId, ApplicationInstanceId appInstanceId, Set<ComponentInfo> components) {
        this.applicationName = applicationName;
        this.applicationId = applicationId;
        this.appInstanceId = appInstanceId;
        this.components = components;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public ApplicationId getApplicationId() {
        return applicationId;
    }

    public ApplicationInstanceId getAppInstanceId() {
        return appInstanceId;
    }

    public Set<ComponentInfo> getComponents() {
        return components;
    }

}
