package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.InportAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerEnvVarHandler.EnvType;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandStack;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandUtils;
import java.util.HashMap;
import java.util.Map;

//todo: there might be a problem, that the env-vars are appended, but not copied each time a value changed

public class DockerContainerLogic extends AbstractDockerContainerLogic {
  private final DockerComponent myComponent;
  protected final DockerEnvVarHandler envVarHandler;

  public DockerContainerLogic(Builder builder) {
    super(builder);
    this.myComponent = builder.myComponent;

    try {
      myComponent.setContainerName(this.myId);
    } catch (DockerCommandException ce) {
      LOGGER.error("Cannot set name for Docker container for component:" + myId, ce);
    }

    envVarHandler = new DockerEnvVarHandler(envVarsStatic, envVarsDynamic, translateMap);
    //doesn't copy lance internal env-vars, just the docker ones
    //lance internal env-vars will be copied when an environment-variable changes a value and redeployment is triggered
    initRedeployDockerCommands();
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      DockerCommandStack dockerCommandStack = myComponent.getDockerCommandStack();
      DockerCommand createCmd = dockerCommandStack.getCreate();

      execHandler.doPullImages(myComponent.getFullImageName());
      //do Not copy lance environment in create command
      createCmd = envVarHandler.resolveDockerEnvVars(createCmd);
      dockerCommandStack.setCreate(createCmd);
      //todo: Create function to check, if these ports match the ports given in docker command
      //Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
      //myComponent.setPort(portsToSet);
      final String createCmdString = DockerCommandUtils.getFullDockerCommandString(dockerCommandStack,
          Type.CREATE, myComponent.getFullIdentifier(Type.CREATE));
      //todo: better log this in DockerConnectorClass
      LOGGER.debug(String
          .format("Creating container %s with docker cli command: %s.", myId, createCmdString));
      execHandler.executeSingleDockerCommand(createCmdString);
    } catch(DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  private void initRedeployDockerCommands() {
    //stop and remove command (part of redeployment) do not need to be initialised
    DockerCommand origCmd = myComponent.getDockerCommandStack().getCreate();
    DockerCommand redeplCmd = myComponent.getDockerCommandStack().getRun();
    try {
      redeplCmd = DockerCommandUtils.appendOption(redeplCmd, Option.DETACH, "");
      DockerCommandStack.copyCmdOptions(origCmd, redeplCmd);
      DockerCommandStack.copyCmdOsCommand(origCmd, redeplCmd);
      DockerCommandStack.copyCmdArgs(origCmd, redeplCmd);
      myComponent.getDockerCommandStack().setRun(redeplCmd);
    } catch (DockerCommandException ex) {
      LOGGER.error(ex.getMessage());
    }
  }

  @Override
  void setStaticEnvironment(@SuppressWarnings("unused") DockerShell shell,
      @SuppressWarnings("unused") BashExportBasedVisitor visitor) throws ContainerException {
    if(envVarHandler.checkEnvChange(EnvType.STATIC)) {
      try {
        doRedeploy();
      } catch (ContainerException e) {
        LOGGER.error("cannot redeploy container " + myId + "for updating the environment");
      }
    }

    envVarHandler.setEnvMemory(EnvType.STATIC);
  }

  @Override
  void setDynamicEnvironment(@SuppressWarnings("unused") BashExportBasedVisitor visitor,
      PortDiff<DownstreamAddress> diff) throws ContainerException {
    envVarHandler.generateDynamicEnvVars(this.myComponent, this.deploymentContext, this.networkHandler, diff);

    if(envVarHandler.checkEnvChange(EnvType.DYNAMIC)) {
      try {
        doRedeploy();
      } catch (ContainerException e) {
        LOGGER.error("cannot redeploy container " + myId + "for updating the environment");
      }
    }

    envVarHandler.setEnvMemory(EnvType.DYNAMIC);
  }

  private void doRedeploy() throws ContainerException {
    DockerCommand runCmd = myComponent.getDockerCommandStack().getRun();
    try {
      runCmd = envVarHandler.resolveDockerEnvVars(runCmd);
      runCmd = envVarHandler.copyLanceEnvIntoCommand(runCmd);
      myComponent.getDockerCommandStack().setRun(runCmd);
      //problem:possible problem that vars get appended but not copied
    } catch (DockerCommandException e) {
      throw new ContainerException("cannot redeploy container " + myId + " because of failing to create the run command", e);
    }
    executeGenericRedeploy();
  }

  private void executeGenericRedeploy() throws ContainerException {
    try {
      DockerCommandStack dStack = myComponent.getDockerCommandStack();
      execHandler.executeRedeploy(dStack, getCmdFullIdentifierMap());
    } catch (DockerException de) {
      throw new ContainerException("cannot redeploy container: " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  @Override
  public void doInit(LifecycleStore store) throws ContainerException {
      try {
        //Environment still set (in logic.doInit call in BootstrapTransitionAction)
        //could return a shell
        executeGenericStart();
      } catch (ContainerException ce) {
        throw ce;
      } catch (Exception ex) {
        throw new ContainerException(ex);
      }
  }

  @Override
  public void doDestroy(boolean force, boolean remove) throws ContainerException {
    /* currently docker ignores force flag */
    try {
      DockerCommandStack dockerCommandStack = myComponent.getDockerCommandStack();
      execHandler.destroyContainer(dockerCommandStack, getCmdFullIdentifierMap(), remove);
    } catch (DockerException de) {
      throw new ContainerException(de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  private Map<Type,String> getCmdFullIdentifierMap() {
    Map<Type,String> cmdFullIdentifierMap = new HashMap<>();

    cmdFullIdentifierMap.put(Type.CREATE, myComponent.getFullIdentifier(Type.CREATE));
    cmdFullIdentifierMap.put(Type.START, myComponent.getFullIdentifier(Type.START));
    cmdFullIdentifierMap.put(Type.STOP, myComponent.getFullIdentifier(Type.STOP));
    cmdFullIdentifierMap.put(Type.RUN, myComponent.getFullIdentifier(Type.RUN));
    cmdFullIdentifierMap.put(Type.REMOVE, myComponent.getFullIdentifier(Type.REMOVE));

    return cmdFullIdentifierMap;
  }

  @Override
  void postPreInstall() {
    try {
      execHandler.runPostInstallAction();
    } catch (DockerException de) {
      LOGGER.warn("could not update finalise image handling.", de);
    }
  }

  private DockerShell executeGenericStart() throws ContainerException {
    final DockerShell dshell;
    try {
      DockerCommandStack dockerCommandStack = myComponent.getDockerCommandStack();
      final String startCmdStr = DockerCommandUtils.getFullDockerCommandString(dockerCommandStack,
          Type.START, myComponent.getFullIdentifier(Type.START));
      dshell = execHandler.executeProgressingDockerCommand(startCmdStr);
      BashExportBasedVisitor visitor = new BashExportBasedVisitor(dshell);
      setStaticEnvironment(dshell, visitor);
      //Setting Dynamic-Envvars here fails, because pub-ip would be set to <unknown> which is invalid bash syntax
      //setDynamicEnvironment(visitor, null);
    } catch (DockerException de) {
      throw new ContainerException("cannot start container: " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }

    return dshell;
  }

  public static class Builder extends AbstractDockerContainerLogic.Builder<DockerComponent,Builder> {

    @Override
    public DockerContainerLogic build() {
      return new DockerContainerLogic(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
