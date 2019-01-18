package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;

public interface ExecHandler {
  InportAccessor getPortMapper();
  String getLocalAddress() throws ContainerException;
}