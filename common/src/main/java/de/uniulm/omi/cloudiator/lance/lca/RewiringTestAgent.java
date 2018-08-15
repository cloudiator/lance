package de.uniulm.omi.cloudiator.lance.lca;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitecture;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RewiringTestAgent extends Remote {

    public ApplicationInstanceId testNewTopology(AppArchitecture arch, String publicIp) throws ContainerException, RemoteException;
    public void testSMTraversingBeforeLccSM() throws ContainerException, RemoteException;
    public void testSMsTraversingBeforeStop() throws ContainerException, RemoteException;
    public void testSMsStopTransition() throws ContainerException, RemoteException;
    public void testPortUpdater() throws ContainerException, RemoteException;
    void stop() throws RemoteException;
    void terminate() throws RemoteException;
}
