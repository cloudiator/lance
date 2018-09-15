package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.uniulm.omi.cloudiator.lance.application.component.ComponentType.DOCKER;

//TODO: Implement. DeployableComponent should among other things not depend on LifeCycleStore but LifecycleComponent should
public class DockerComponent extends DeployableComponent implements ComponentFactory<DockerComponent> {
    DockerComponent(String nameParam, ComponentId idParam, LifecycleStore lifecycleStoreParam,
                       List<InPort> inPortsParam, List<OutPort> outPortsParam, Map<String, Class<?>> propertiesParam,
                       HashMap<String, ? extends Serializable> propertyValuesParam) {
        super(nameParam, idParam, lifecycleStoreParam, inPortsParam, outPortsParam, propertiesParam, propertyValuesParam);
    }

    //needed temporarily to make ComponentBuilder class work, as long as DockerComponent and LifecycleComponent inherit from DeployableComponent
    public DockerComponent() {
        super();
    }

    @Override
    public ComponentType getType() {
        return DOCKER;
    }

    @Override
    public DockerComponent newObject(String nameParam, ComponentId idParam,
        LifecycleStore lifecycleStoreParam, List<InPort> inPortsParam, List<OutPort> outPortsParam,
        Map<String, Class<?>> propertiesParam,
        HashMap<String, ? extends Serializable> propertyValuesParam) {
        return new DockerComponent(nameParam, idParam, lifecycleStoreParam,  inPortsParam, outPortsParam, propertiesParam, propertyValuesParam);
    }
}
