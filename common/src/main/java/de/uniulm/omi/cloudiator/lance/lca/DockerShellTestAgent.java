package de.uniulm.omi.cloudiator.lance.lca;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitecture;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface DockerShellTestAgent extends Remote {

  ApplicationInstanceId setupContainer(AppArchitecture arch, String publicIp, LcaRegistry reg)
      throws ContainerException, RemoteException;

  void openShellAndCreateContainer(String imageName, Map<Integer, Integer> portMap)
      throws ContainerException, RemoteException;

  void startContainer() throws ContainerException, RemoteException;

  void setEnvironment() throws ContainerException, RemoteException;

  void stopContainer() throws ContainerException, RemoteException;

  void closeShell() throws ContainerException, RemoteException;

  void openAndInstallShell() throws ContainerException, RemoteException;
}
