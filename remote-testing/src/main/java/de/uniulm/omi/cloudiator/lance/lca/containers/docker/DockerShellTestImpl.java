package de.uniulm.omi.cloudiator.lance.lca.containers.docker;
//package de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponentBuilder;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.DockerShellTestAgent;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.containers.TestImpl;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.ProcessBasedConnector;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStoreBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.bash.BashBasedHandlerBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.DefaultHandlers;
import de.uniulm.omi.cloudiator.lance.lifecycles.CoreElementsRemote;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitecture;
import de.uniulm.omi.cloudiator.lance.util.application.ComponentInfo;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.UUID;

public class DockerShellTestImpl extends TestImpl implements DockerShellTestAgent {

  private static final int DEFAULT_PROPERTIES = 5;
  private static final String INITIAL_LOCAL_ADDRESS = "<unknown>";
  private volatile ComponentInfo info = null;
  private volatile DockerComponent comp;
  private final ProcessBasedConnector connector;
  private final DockerShellFactory shellFactory;

  public DockerShellTestImpl() {
    connector = new ProcessBasedConnector("ubuntu-host");
    shellFactory = new DockerShellFactory();
  }

  @Override
  public ApplicationInstanceId setupContainer(
      AppArchitecture arch, String publicIp, LcaRegistry reg) throws ContainerException, RemoteException {
    info = (ComponentInfo) arch.getComponents().toArray()[0];
    ApplicationInstanceId id = setupApp(arch, publicIp, reg);

    try {
      core.fillRegistry(info.getComponentId());
    } catch (RegistrationException e) {
      e.printStackTrace();
    }

    return id;
  }

  @Override
  public void openShellAndCreateContainer(String imageName, Map<Integer, Integer> portMap)
      throws ContainerException, RemoteException {
    if (info == null) throw new ContainerException("ComponentInfo not set");

    try {
      connector.createContainer(imageName, info.getComponentInstanceId(), portMap);
    } catch (DockerException de) {
      throw new ContainerException("Cannot stop container");
    }
  }

  @Override
  public void startContainer() throws ContainerException, RemoteException {
    if (info == null) throw new ContainerException("ComponentInfo not set");

    try {
      connector.startContainer(info.getComponentInstanceId());
    } catch (DockerException de) {
      throw new ContainerException("Cannot stop container");
    }
  }

  @Override
  public void setEnvironment() throws ContainerException, RemoteException {
    if (info == null)
      throw new ContainerException("ComponentInfo not set");

    DockerShell shell;

    DockerShellWrapper w = shellFactory.createShell();
    shell = w.shell;

    BashExportBasedVisitor visitor = new BashExportBasedVisitor(shell);
    visitor.visit("TERM", UUID.randomUUID().toString());
    ExecutionResult res = shell.executeBlockingCommand("echo -n \"$TERM\"");

    System.out.println(res);
  }

  @Override
  public void stopContainer() throws ContainerException, RemoteException {
    if (info == null) throw new ContainerException("ComponentInfo not set");

    try {
      connector.stopContainer(info.getComponentInstanceId());
    } catch (DockerException de) {
      throw new ContainerException("Cannot stop container");
    }
  }

  @Override
  public void closeShell() throws ContainerException, RemoteException {
    shellFactory.closeShell();
  }

  @Override
  public void openAndInstallShell()
      throws ContainerException, RemoteException {
    DockerShell shell;

    try {
      shell = connector.getSideShell(info.getComponentInstanceId());
      shellFactory.installDockerShell(shell);
    } catch (DockerException de) {
      throw new ContainerException("Cannot get side shell");
    }
  }

  @Override
  protected void init(AppArchitecture arch) throws ContainerException {
    if (info == null) throw new ContainerException("ComponentInfo not set");

    DockerComponentBuilder builder =
        DockerComponentBuilder.createBuilder(arch.getApplicationName(), info.getComponentId());

    switch (info.getComponentName()) {
      case "zookeeper":
        builder.addLifecycleStore(createZookeeperLifecleStore());
        break;
      default:
        throw new ContainerException("wrong container name: " + info.getComponentName());
    }

    builder.deploySequentially(true);
    comp = builder.build();
  }

  private LifecycleStore createZookeeperLifecleStore() {

    LifecycleStoreBuilder store = new LifecycleStoreBuilder();

    store.setStartDetector(DefaultHandlers.DEFAULT_START_DETECTOR);
    return store.build();
  }
}
