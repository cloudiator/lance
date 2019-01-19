package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.container.standard.ExecHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandStack;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DockerExecHandler implements ExecHandler {
  protected static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerManager.class);

  private final ComponentInstanceId myId;
  private final DeploymentContext deploymentContext;
  private final DockerConnector client;
  private final DockerImageHandler imageHandler;

  DockerExecHandler(ComponentInstanceId myId, DeploymentContext deplContext,
      DockerConnector client, DockerImageHandler imageHandler) {
    this.myId = myId;
    this.deploymentContext = deplContext;
    this.client = client;
    this.imageHandler = imageHandler;
  }

  @Override
  public InportAccessor getPortMapper() {
    return ( (portName, clientState) -> {
      try {
        Integer portNumber = (Integer) deploymentContext.getProperty(portName, InPort.class);
        int mapped = client.getPortMapping(myId, portNumber);
        Integer i = Integer.valueOf(mapped);
        clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_0, i);
        clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_1, i);
        clientState.registerValueAtLevel(PortRegistryTranslator.PORT_HIERARCHY_2, portNumber);
      } catch(DockerException de) {
        throw new ContainerException("coulnd not register all port mappings", de);
      }
    });
  }

  @Override
  public String getLocalAddress() throws ContainerException {
    try {
      return client.getContainerIp(myId);
    } catch(DockerException de) {
      // this means that that the container is not
      // up and running; hence, no IP address is
      // available. it is up to the caller to figure
      // out the semantics of this state
    }
    return null;
  }

  public void creatContainer(DockerCommandStack dStack, DockerEnvVarHandler envVarHandler,
      String fullImageName, String fullIdentifier) throws DockerException {
    DockerCommand createCmd = dStack.getCreate();
    imageHandler.doPullImages(myId, fullImageName);
    //do Not copy lance environment in create command
    try {
      createCmd = envVarHandler.resolveDockerEnvVars(createCmd);
      dStack.setCreate(createCmd);
      //todo: Create function to check, if these ports match the ports given in docker command
      //Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
      //myComponent.setPort(portsToSet);
      final String createCmdString = DockerCommandUtils.getFullDockerCommandString(dStack,
          Type.CREATE, fullIdentifier);

      //todo: better log this in DockerConnectorClass
      LOGGER.debug(String
          .format("Creating container %s with docker cli command: %s.", myId, createCmdString));
      client.executeSingleDockerCommand(createCmdString);
    } catch (DockerCommandException dCmdEx) {
      throw new DockerException(String
          .format("Error in building docker create command for container %s.", myId));
    }
  }

  public DockerShell executeStart(DockerCommandStack dStack, String fullIdentifier)
      throws DockerException {
    DockerShell dShell;
    final String startCmdStr;
    try {
      startCmdStr = DockerCommandUtils.getFullDockerCommandString(dStack,
          Type.START, fullIdentifier);
    } catch (DockerCommandException e) {
      throw new DockerException(String
          .format("Error in building docker start command for container %s.", myId));
    }

    dShell = client.executeProgressingDockerCommand(startCmdStr);
    return dShell;
  }

  public void stopContainer(ComponentInstanceId myId) throws DockerException {
    client.stopContainer(myId);
  }

  DockerShell startLifecycleContainer() throws DockerException {
    DockerShell dShell = client.startLifecycleContainer(myId);
    return dShell;
  }

  void createLifecycleContainer(String target, Map<Integer,Integer> portsToSet) throws DockerException {
    client.createLifecycleContainer(target, myId, portsToSet);
  }

  DockerShell getSideShell() throws DockerException {
    DockerShell shell = client.getSideShell(myId);
    return shell;
  }

  void doPullImages(String fullImageName) throws DockerException {
    imageHandler.doPullImages(myId, fullImageName);
  }

  void executeSingleDockerCommand(String cmdStr) throws DockerException {
    client.executeSingleDockerCommand(cmdStr);
  }

  void executeRedeploy(DockerCommandStack dStack, Map<DockerCommand.Type,String> cmdFullIdentifierMap)
      throws DockerException, DockerCommandException {
    final String stopIdentifier = cmdFullIdentifierMap.get(Type.STOP);
    final String rmIdentifier = cmdFullIdentifierMap.get(Type.REMOVE);
    final String runIdentifier = cmdFullIdentifierMap.get(Type.RUN);

    if(stopIdentifier == null || rmIdentifier == null || runIdentifier == null) {
      throw new DockerCommandException("Identifier for Redeployment command not set");
    }

    final String stopCmdStr = DockerCommandUtils.getFullDockerCommandString(dStack, DockerCommand.Type.STOP, stopIdentifier);
    final String rmCmdStr = DockerCommandUtils.getFullDockerCommandString(dStack, DockerCommand.Type.REMOVE, rmIdentifier);
    final String runCmdStr =  DockerCommandUtils.getFullDockerCommandString(dStack, DockerCommand.Type.RUN, runIdentifier);
    LOGGER.debug(String
        .format("Redeploying container %s with docker cli command: %s.", myId, runCmdStr));
    client.executeSingleDockerCommand(stopCmdStr);
    client.executeSingleDockerCommand(rmCmdStr);
    client.executeSingleDockerCommand(runCmdStr);
  }

  void destroyContainer(DockerCommandStack dStack, Map<DockerCommand.Type,String> cmdFullIdentifierMap, boolean remove)
      throws DockerException, DockerCommandException {
    final String stopIdentifier = cmdFullIdentifierMap.get(Type.STOP);
    final String rmIdentifier = cmdFullIdentifierMap.get(Type.REMOVE);

    if(stopIdentifier == null || rmIdentifier == null) {
      throw new DockerCommandException("Identifier for Destroy command not set");
    }

    final String stopCmdStr = DockerCommandUtils.getFullDockerCommandString(dStack,
        Type.STOP, stopIdentifier);
    client.executeSingleDockerCommand(stopCmdStr);
    if (remove) {
      final String rmCmdStr = DockerCommandUtils.getFullDockerCommandString(dStack,
          Type.REMOVE, rmIdentifier);
      client.executeSingleDockerCommand(rmCmdStr);
    }
  }

  void runPostInstallAction() throws DockerException {
    imageHandler.runPostInstallAction(myId);
  }

  DockerShell executeProgressingDockerCommand(String fullDockerCommandString) throws DockerException {
    DockerShell dshell;
    dshell = client.executeProgressingDockerCommand(fullDockerCommandString);

    return dshell;
  }
}
