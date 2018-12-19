package de.uniulm.omi.cloudiator.lance.lca;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitecture;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RewiringTestAgent extends Remote {

  ApplicationInstanceId testNewTopology(AppArchitecture arch, String publicIp, LcaRegistry reg)
      throws ContainerException, RemoteException;

  void testSMTraversingBeforeLccSM() throws ContainerException, RemoteException;

  void testSMsTraversingBeforeStop() throws ContainerException, RemoteException;

  void testSMsStopTransition() throws ContainerException, RemoteException;

  void testPortUpdater() throws ContainerException, RemoteException;

  void stop() throws RemoteException;

  void terminate() throws RemoteException;
}
