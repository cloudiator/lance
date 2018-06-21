package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.uniulm.omi.cloudiator.lance.application.component.ComponentType.DOCKER;

//TODO: Implement. DeployableComponent should among other things not depend on LifeCycleStore but LifeCycleComponent should
public class DockerComponent extends DeployableComponent {
    DockerComponent(String nameParam, ComponentId idParam, LifecycleStore lifecycleStoreParam,
                       List<InPort> inPortsParam, List<OutPort> outPortsParam, Map<String, Class<?>> propertiesParam,
                       HashMap<String, ? extends Serializable> propertyValuesParam) {
        super(nameParam, idParam, lifecycleStoreParam, inPortsParam, outPortsParam, propertiesParam, propertyValuesParam);
    }

    @Override
    public ComponentType getType() {
        return DOCKER;
    }
}
