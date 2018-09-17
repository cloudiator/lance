package de.uniulm.omi.cloudiator.lance.lca.containers;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycles.CoreElementsRemote;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitecture;
import java.rmi.RemoteException;

abstract public class TestImpl {
  public volatile CoreElementsRemote core;

  protected ApplicationInstanceId setupApp(AppArchitecture appArch, String publicIp, LcaRegistry reg)  throws ContainerException, RemoteException {
    //assertNotNull(CoreElements.context);
    CoreElementsRemote.arch = appArch;
    CoreElementsRemote.initHostContext(publicIp);
    core = new CoreElementsRemote(reg);

    try {
      core.setUpRegistry();
    } catch (RegistrationException e) {
      e.printStackTrace();
    }

    init(appArch);

    return appArch.getAppInstanceId();
  }

  protected abstract void init(AppArchitecture appArch) throws ContainerException;
}
