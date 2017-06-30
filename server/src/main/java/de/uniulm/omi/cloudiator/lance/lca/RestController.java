package de.uniulm.omi.cloudiator.lance.lca;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by frankgriesinger on 28.06.2017.
 */
@Path("/")
public class RestController {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestController.class);

  final private LifecycleAgentRestImpl impl;

  public RestController(LifecycleAgentRestImpl impl) {
    this.impl = impl;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/agentStatus")
  public AgentStatus getAgentStatus() throws RemoteException {
    return impl.getAgentStatus();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/componentContainerStatus/{cid}")
  public String getComponentContainerStatus(@PathParam("cid") String cid)
      throws RemoteException {
    return impl.getComponentContainerStatus(ComponentInstanceId.fromString(cid)).toString();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/stop")
  public String stop() throws RemoteException {
    // stop REST server
    impl.stop();
    return "ok";
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/terminate")
  public String terminate() throws RemoteException {
    // terminate REST server
    impl.terminate();
    return "ok";
  }
  
  private Object[] deserialise(byte[] restBodyDeployComponent) throws ClassNotFoundException, IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(restBodyDeployComponent);
    ObjectInputStream is = new ObjectInputStream(in);
    Object[] o = (Object[])is.readObject();
    return o;
  }
  
  private byte[] serialise(Object ... params) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream os2 = new ObjectOutputStream(out);
    os2.writeObject(params);
    return out.toByteArray();
  }
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/containers")
  public byte[] listContainers() throws RemoteException, LcaException {
    List<ComponentInstanceId> containers = impl.listContainers();

    try {
      return serialise(containers);
    } catch (IOException e) {
      LOGGER.error("failed to output containers", e);
    }
    throw new LcaException("cannot list containers.");
  }

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes("application/octet-stream")
  @Path("/deployComponent")
  public byte[] deployComponent(byte[] restBodyDeployComponent)
      throws IOException, LcaException, RegistrationException, ContainerException, ClassNotFoundException {

    LOGGER.error("start to deploy component");

    try {
      Object[] o = deserialise(restBodyDeployComponent);
      
      final ComponentInstanceId componentInstanceId = impl
          .deployComponent((DeploymentContext) o[0], (DeployableComponent) o[1],
              (OperatingSystem) o[2], (ContainerType) o[3]);

      return serialise(componentInstanceId);
    } catch (IOException e) {
      LOGGER.error("failed to deploy component", e);
    }
    throw new LcaException("failed to deploy component");
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/stopComponentInstance")
  public boolean stopComponentInstance(byte[] restBodyDeployComponent)
      throws RemoteException, LcaException, ContainerException {

    try {
      Object[] o = deserialise(restBodyDeployComponent);
      return impl.stopComponentInstance((ContainerType)o[0], (ComponentInstanceId) o[1]);
    } catch (IOException e) {
      LOGGER.error("failed to read stopComponentInstance (1)", e);
    } catch (ClassNotFoundException e) {
      LOGGER.error("failed to read stopComponentInstance (2)", e);
    }

    throw new LcaException("failed to read stopComponentInstance (3)");
  }
}
