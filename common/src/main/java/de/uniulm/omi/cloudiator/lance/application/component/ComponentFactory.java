package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface ComponentFactory<T extends DeployableComponent> {
  T newObject(String nameParam, ComponentId idParam, LifecycleStore lifecycleStoreParam,
      List<InPort> inPortsParam, List<OutPort> outPortsParam, Map<String, Class<?>> propertiesParam,
      HashMap<String, ? extends Serializable> propertyValuesParam);
}
