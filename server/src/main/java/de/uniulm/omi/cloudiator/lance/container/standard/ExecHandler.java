package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;

public interface ExecHandler {
  InportAccessor getPortMapper();
  String getLocalAddress() throws ContainerException;
}