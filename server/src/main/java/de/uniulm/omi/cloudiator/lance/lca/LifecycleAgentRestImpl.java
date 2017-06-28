package de.uniulm.omi.cloudiator.lance.lca;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by frankgriesinger on 28.06.2017.
 */
public class LifecycleAgentRestImpl implements LifecycleAgent {

  final private LifecycleAgentCore core;
  final private RestServer restServer;

  public LifecycleAgentRestImpl(LifecycleAgentCore core) {
    this.core = core;


    ExecutorService executorService = Executors.newCachedThreadPool();
    restServer = new RestServer(9088, "http://0.0.0.0", executorService, this);
  }

  @Override
  public AgentStatus getAgentStatus() throws RemoteException {

    return core.getAgentStatus();
  }

  @Override
  public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid)
      throws RemoteException {
    return core.getComponentContainerStatus(cid);
  }

  @Override
  public void stop() throws RemoteException {
    // stop REST server
    //TODO
  }

  @Override
  public void terminate() throws RemoteException {
    // terminate REST server
    //TODO
    core.terminate();
  }

  @Override
  public List<ComponentInstanceId> listContainers() throws RemoteException {
    return core.listContainers();
  }

  @Override
  public ComponentInstanceId deployComponent(DeploymentContext ctx, DeployableComponent component,
      OperatingSystem os, ContainerType containerType)
      throws RemoteException, LcaException, RegistrationException, ContainerException {
    return core.deployComponent(ctx, component, os, containerType);
  }

  @Override
  public boolean stopComponentInstance(ContainerType containerType, ComponentInstanceId instanceId)
      throws RemoteException, LcaException, ContainerException {
    return core.stopComponentInstance(containerType, instanceId);
  }
}
