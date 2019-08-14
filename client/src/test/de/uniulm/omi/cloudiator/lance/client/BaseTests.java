package de.uniulm.omi.cloudiator.lance.client;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.client.TestUtils.InportInfo;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Test;

public abstract class BaseTests {

  protected void testClientGetter() {
    try {
      TestUtils.client = LifecycleClient.getClient(TestUtils.publicIp);
    } catch (RemoteException ex) {
      System.err.println("Server not reachable");
    } catch (NotBoundException ex) {
      System.err.println("Socket not bound");
    }
  }

  protected void testRegisterBase() {
    try {
      TestUtils.client.registerApplicationInstance(TestUtils.appInstanceId, TestUtils.applicationId);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }
}
