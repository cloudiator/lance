package de.uniulm.omi.cloudiator.lance.lca.container.environment;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import java.util.Map;

public interface DynamicEnvVars {

  Map<String, String> getEnvVars();

  void generateDynamicEnvVars();

  void injectDynamicEnvVars(DynamicEnvVarsImpl vars) throws ContainerException;

  void removeDynamicEnvVars(DynamicEnvVars vars) throws ContainerException;
}
