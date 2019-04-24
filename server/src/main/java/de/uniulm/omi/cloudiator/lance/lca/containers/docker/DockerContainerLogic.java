package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.COMPONENT_INSTANCE_STATUS;
import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.DYN_GROUP_KEY;
import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.DYN_HANDLER_KEY;

import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerEnvVarHandler.EnvType;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//todo: there might be a problem, that the env-vars are appended, but not copied each time a value changed

public class DockerContainerLogic extends AbstractDockerContainerLogic {
  private final DockerComponent myComponent;
  protected final DockerImageHandler imageHandler;
  private DockerDynHandler dynHandler;
  private Thread dynThread;

  private enum EnvType {STATIC, DYNAMIC};
  protected final DockerEnvVarHandler envVarHandler;

  public DockerContainerLogic(Builder builder) {
    super(builder);
    this.myComponent = builder.myComponent;
    this.imageHandler = new DockerImageHandler(new DockerOperatingSystemTranslator(),
      builder.client, builder.myComponent, builder.dockerConfig);

    try {
      myComponent.setContainerName(this.myId);
    } catch (DockerCommandException ce) {
      LOGGER.error("Cannot set name for Docker container for component:" + myId, ce);
    }

    //Getting initialized dynamically
    dynHandler = null;
    //todo: check if must be initialised in doStart method
    dynThread = null;
    envVarHandler = new DockerEnvVarHandler(envVarsStatic, envVarsDynamic, translateMap);
    //doesn't copy lance internal env-vars, just the docker ones
    //lance internal env-vars will be copied when an environment-variable changes a value and redeployment is triggered
    initRedeployDockerCommands();
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      imageHandler.doPullImages(myId, myComponent.getFullImageName());
      DockerCommand createOrig = myComponent.getEntireDockerCommands().getCreate();
      DockerCommand createEnvMod = envVarHandler.resolveDockerEnvVars(createOrig);
      myComponent.getEntireDockerCommands().setCreate(createEnvMod);
      //todo: Create function to check, if these ports match the ports given in docker command
      //Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
      //myComponent.setPort(portsToSet);
      final String createCommand = myComponent.getFullDockerCommand(DockerCommand.Type.CREATE);
      //todo: better log this in DockerConnectorClass
      LOGGER.debug(String
          .format("Creating container %s with docker cli command: %s.", myId, createCommand));
      client.executeSingleDockerCommand(createCommand);
      checkContainerStatus("created");
    } catch(DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  private void initRedeployDockerCommands() {
    //stop and remove command (part of redeployment) do not need to be initialised
    DockerCommand origCmd = myComponent.getEntireDockerCommands().getCreate();
    DockerCommand redeplCmd = myComponent.getEntireDockerCommands().getRun();
    try {
      redeplCmd = DockerCommandUtils.appendOption(redeplCmd, Option.DETACH, "");
      EntireDockerCommands.copyCmdOptions(origCmd, redeplCmd);
      EntireDockerCommands.copyCmdOsCommand(origCmd, redeplCmd);
      EntireDockerCommands.copyCmdArgs(origCmd, redeplCmd);
      myComponent.getEntireDockerCommands().setRun(redeplCmd);
    } catch (DockerCommandException ex) {
      LOGGER.error(ex.getMessage());
    }
  }

  @Override
  void setStaticEnvironment(@SuppressWarnings("unused") DockerShell shell,
      @SuppressWarnings("unused") BashExportBasedVisitor visitor) throws ContainerException {
    if(envVarHandler.checkEnvChange(DockerEnvVarHandler.EnvType.STATIC)) {
      try {
        doRedeploy();
      } catch (ContainerException e) {
        LOGGER.error("cannot redeploy container " + myId + "for updating the environment");
      }
    }

    envVarHandler.setEnvMemory(DockerEnvVarHandler.EnvType.STATIC);
  }

  @Override
  void setDynamicEnvironment(@SuppressWarnings("unused") BashExportBasedVisitor visitor,
      PortDiff<DownstreamAddress> diff) throws ContainerException {
    envVarHandler.generateDynamicEnvVars(this.myComponent, this.deploymentContext, this.networkHandler, diff);

    if(envVarHandler.checkEnvChange(DockerEnvVarHandler.EnvType.DYNAMIC)) {
      try {
        doRedeploy();
      } catch (ContainerException e) {
        LOGGER.error("cannot redeploy container " + myId + "for updating the environment");
      }
    }

    envVarHandler.setEnvMemory(DockerEnvVarHandler.EnvType.DYNAMIC);
  }

  @Override
  public void doInit(LifecycleStore store) throws ContainerException {
      try {
        //Environment still set (in logic.doInit call in BootstrapTransitionAction)
        //could return a shell
        executeGenericStart();
        checkContainerStatus("running");
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
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.STOP));
      checkContainerStatus("exited");
      if (remove) {
        client.executeSingleDockerCommand(
            myComponent.getFullDockerCommand(DockerCommand.Type.REMOVE));
      }
    } catch (DockerException de) {
      throw new ContainerException(de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  @Override
  public AbstractComponent getComponent() {
    return myComponent;
  }

  //todo: Put this check into DockerDynHandler
  @Override
  public boolean isValidDynamicProperty(String key) {
    if(key.equals(LcaRegistryConstants.regEntries.get(DYN_GROUP_KEY))) {
      return true;
    } else if(key.equals(LcaRegistryConstants.regEntries.get(DYN_HANDLER_KEY))) {
      return true;
    } else if(key.equals(LcaRegistryConstants.regEntries.get(COMPONENT_INSTANCE_STATUS))) {
      return true;
    }

    return false;
  }

  @Override
  public void doStartDynHandling(GlobalRegistryAccessor accessor) throws ContainerException {
    try {
      dynHandler = new DockerDynHandler(myComponent.getContainerName(), myComponent.getDynamicHandler(), myComponent
          .getUpdateScriptFilePath(), client);
      dynHandler.setAccessor(accessor);
      //todo: check if must be initialised in doStart method
      dynThread = new Thread(dynHandler);
      dynThread.start();
    } catch (DockerCommandException e) {
      throw new ContainerException("Cannot initialize Dyn Handler. Problems setting the correct container name...", e);
    }
  }

  @Override
  public void doStopDynHandling() throws ContainerException {
    dynHandler.setRunning(false);
    try {
      dynThread.join();
    } catch (InterruptedException e) {
      throw new ContainerException("Dyn Handling was interrupted while waiting for the handler thread to finish.");
    }
  }

  @Override
  void postPreInstall() {
    try {
      imageHandler.runPostInstallAction(myId);
    } catch (DockerException de) {
      LOGGER.warn("could not update finalise image handling.", de);
    }
  }

  @Override
  protected void checkContainerStatus(String status) throws DockerException {
      final String containerId = getContainerId();
      final String cmdString = String.format("ps --all --quiet --filter=status=%s"
          + " --filter=id=%s", status, containerId);
      final String stdOutStr = client.executeSingleDockerCommand(cmdString);
      if(containerId.equals(stdOutStr)) {
        return;
      }

      final String cmdStringDebug =
          String.format("ps --all --filter=id=%s --format \"{{.Status}}\"", containerId);
      final String stdOutStrDebug = client.executeSingleDockerCommand(cmdStringDebug);
      throw new DockerException(String.format("Container with id %s has wrong state \'%s\'."
          + "Should be in %s.", myId, stdOutStrDebug, status));
  }

  @Override
  protected String getContainerId() throws DockerException {
    String containerName;
    String id;
    try {
      containerName = myComponent.getContainerName();
      final String cmdStr = "ps --all --filter=name=" + containerName + " --format \"{{.ID}}:{{.Names}}\"";
      String stdOutString = client.executeSingleDockerCommand(cmdStr);
      stdOutString = stdOutString.replaceAll("[\"']","");
      final String[] splitArr = stdOutString.split(":");
      if (splitArr.length != 2) {
        throw new DockerException("Cannot query container id.");
      } else {
        id = splitArr[0];
        return id;
      }
    } catch (DockerCommandException e) {
      throw new DockerException("Cannot resolve the correct container name", e);
    }
  }

  private void executeGenericStart() throws ContainerException {
    DockerCommand runCmd = myComponent.getEntireDockerCommands().getRun();
    try {
      runCmd = envVarHandler.resolveDockerEnvVars(runCmd);
      runCmd = envVarHandler.copyLanceEnvIntoCommand(runCmd);
      myComponent.getEntireDockerCommands().setRun(runCmd);
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.REMOVE));
      final String cmdStr = myComponent.getFullDockerCommand(DockerCommand.Type.RUN);
      LOGGER.debug(String
      .format("Redeploying container %s with docker cli command: %s.", myId, cmdStr));
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.RUN));
      BashExportBasedVisitor visitor = new BashExportBasedVisitor(null);
      setStaticEnvironment(null, visitor);
      //Todo: Check why dynamic env setting sets additional values of other container(s)
      //setDynamicEnvironment(visitor, null);
    } catch (DockerException de) {
      throw new ContainerException("cannot redeploy container: " + myId, de);
    } catch (DockerCommandException e) {
      throw new ContainerException("cannot redeploy container " + myId + " because of failing to create the run command", e);
    }
  }

  private void doRedeploy() throws ContainerException {
    DockerCommand runCmd = myComponent.getEntireDockerCommands().getRun();
    try {
      checkContainerStatus("running");
      runCmd = envVarHandler.resolveDockerEnvVars(runCmd);
      runCmd = envVarHandler.copyLanceEnvIntoCommand(runCmd);
      myComponent.getEntireDockerCommands().setRun(runCmd);
      //problem:possible problem that vars get appended but not copied
    } catch (DockerException de) {
      throw new ContainerException(de);
    } catch (DockerCommandException e) {
      throw new ContainerException("cannot redeploy container " + myId + " because of failing to create the run command", e);
    }
    executeGenericRedeploy();
  }

  private void executeGenericRedeploy() throws ContainerException {
    try {
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.STOP));
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.REMOVE));
      final String cmdStr = myComponent.getFullDockerCommand(DockerCommand.Type.RUN);
      LOGGER.debug(String
          .format("Redeploying container %s with docker cli command: %s.", myId, cmdStr));
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.RUN));
    } catch (DockerException de) {
      throw new ContainerException("cannot redeploy container: " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
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
