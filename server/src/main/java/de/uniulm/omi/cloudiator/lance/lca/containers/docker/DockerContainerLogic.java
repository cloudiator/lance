package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DockerContainerLogic extends AbstractDockerContainerLogic {
  private final DockerComponent myComponent;
  //Needed to check for redeployment
  private Map<String, String> envVarsStaticPrev;
  //Needed to check for redeployment
  private Map<String, String> envVarsDynamicPrev;
  protected final DockerImageHandler imageHandler;

  private enum EnvType {STATIC, DYNAMIC};

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
    
    envVarsStaticPrev = new HashMap<>(envVarsStatic);
    envVarsDynamicPrev = new HashMap<>(envVarsDynamic);
    initRedeployDockerCommand();
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      imageHandler.doPullImages(myId, myComponent.getFullImageName());
      //todo: Create function to check, if these ports match the ports given in docker command
      //Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
      //myComponent.setPort(portsToSet);
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.CREATE));
    } catch(DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  private void initRedeployDockerCommand() {
    DockerCommand origCmd = myComponent.getEntireDockerCommands().getCreate();
    DockerCommand redeplCmd = myComponent.getEntireDockerCommands().getRun();
    try {
      EntireDockerCommands.copyCmdOptions(origCmd, redeplCmd);
      EntireDockerCommands.copyCmdOsCommand(origCmd, redeplCmd);
      EntireDockerCommands.copyCmdArgs(origCmd, redeplCmd);
    } catch (DockerCommandException ex) {
      LOGGER.error(ex.getMessage());
    }
  }

  @Override
  void setStaticEnvironment(DockerShell shell, BashExportBasedVisitor visitor) throws ContainerException {
    Map<String,String> envVarsStaticTmp = new HashMap<>();

    for(Entry<String, String> entry: envVarsStatic.entrySet()) {
      //todo: not needed in post-colosseum version, as the environment-var names should be set correctly then
      if(!translateMap.containsKey(entry.getKey()))
        continue;

      envVarsStaticTmp.put(translateMap.get(entry.getKey()),entry.getValue());
    }

    executeEnvSetting(EnvType.STATIC, envVarsStaticTmp);
  }

  @Override
  void setDynamicEnvironment(BashExportBasedVisitor visitor, PortDiff<DownstreamAddress> diff) throws ContainerException {
    this.myComponent.injectDeploymentContext(this.deploymentContext);
    networkHandler.generateDynamicEnvVars(diff);
    this.myComponent.generateDynamicEnvVars();
    collectDynamicEnvVars();
    executeEnvSetting(EnvType.DYNAMIC, envVarsDynamic);
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
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.STOP));
      if(remove)
        client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.REMOVE));
    } catch (DockerException de) {
      throw new ContainerException(de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
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
  void collectDynamicEnvVars() {
    envVarsDynamic.putAll(myComponent.getEnvVars());
    envVarsDynamic.putAll(networkHandler.getEnvVars());
  }

  private DockerShell executeGenericStart() throws ContainerException {
    final DockerShell dshell;
    try {
      dshell = client.executeProgressingDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.START));
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

  private void executeEnvSetting(EnvType eType, Map<String,String> vars) throws ContainerException {
    if(checkEnvChange(eType)) {
      try {
        doRedeploy();
      } catch (ContainerException e) {
        LOGGER.error("cannot redeploy container " + myId + "for updating the environment");
      }
    }

    if(eType == EnvType.STATIC)
      envVarsStaticPrev = new HashMap<>(envVarsStatic);
    else
      envVarsDynamicPrev = new HashMap<>(envVarsDynamic);
  }

  private void doRedeploy() throws ContainerException {
    DockerCommand runCmd = myComponent.getEntireDockerCommands().getRun();
    try {
      copyEnvIntoCommand(runCmd);
    } catch (DockerCommandException e) {
      throw new ContainerException("cannot redeploy container " + myId + " because of failing to create the run command", e);
    }
    executeGenericRedeploy();
  }

  private void executeGenericRedeploy() throws ContainerException {
    try {
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.STOP));
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.REMOVE));
      client.executeProgressingDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.RUN));
    } catch (DockerException de) {
      throw new ContainerException("cannot redeploy container: " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  private void copyEnvIntoCommand(DockerCommand dCmd) throws DockerCommandException {
    for(Entry<String, String> var: envVarsStatic.entrySet()) {
      dCmd.setOption(Option.ENVIRONMENT,var.getKey() + "=" + var.getValue());
    }
    for(Entry<String, String> var: envVarsDynamic.entrySet()) {
      dCmd.setOption(Option.ENVIRONMENT,var.getKey() + "=" + var.getValue());
    }
  }

  private boolean checkEnvChange(EnvType eType) {
    if(eType == EnvType.STATIC && envVarsStatic.equals(envVarsStaticPrev))
      return false;

    if(eType == EnvType.DYNAMIC && envVarsDynamic.equals(envVarsDynamicPrev))
      return false;

    return true;
  }

  private String buildEnvString(Map<String,String> vars) {
    StringBuilder builder = new StringBuilder();

    for(Entry<String, String> var: vars.entrySet()) {
      builder.append("-e " + var.getKey()+"="+var.getValue() + " ");
    }

    return builder.toString();
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
