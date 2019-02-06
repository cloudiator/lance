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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DockerContainerLogic extends AbstractDockerContainerLogic {
  private final DockerComponent myComponent;
  protected final DockerImageHandler imageHandler;
  //Needed to check for redeployment
  private Map<String, String> envVarsStaticPrev;
  //Needed to check for redeployment
  private Map<String, String> envVarsDynamicPrev;
  private DockerDynHandler dynHandler;

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
    //Getting initialized dynamically
    dynHandler = null;
    //doesn't copy lance internal env-vars, just the docker ones
    //lance internal env-vars will be copied when an environment-variable changes a value and redeployment is triggered
    initRedeployDockerCommands();
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      imageHandler.doPullImages(myId, myComponent.getFullImageName());
      resolveDockerEnvVars(myComponent.getEntireDockerCommands().getCreate());
      //todo: Create function to check, if these ports match the ports given in docker command
      //Map<Integer, Integer> portsToSet = networkHandler.findPortsToSet(deploymentContext);
      //myComponent.setPort(portsToSet);
      final String createCommand = myComponent.getFullDockerCommand(DockerCommand.Type.CREATE);
      //todo: better log this in DockerConnectorClass
      LOGGER.debug(String
          .format("Creating container %s with docker cli command: %s.", myId, createCommand));
      client.executeSingleDockerCommand(createCommand);
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
      redeplCmd.setOption(Option.DETACH,"");
      redeplCmd = EntireDockerCommands.copyCmdOptions(origCmd, redeplCmd);
      redeplCmd = EntireDockerCommands.copyCmdOsCommand(origCmd, redeplCmd);
      redeplCmd = EntireDockerCommands.copyCmdArgs(origCmd, redeplCmd);
    } catch (DockerCommandException ex) {
      LOGGER.error(ex.getMessage());
    }
  }

  @Override
  void setStaticEnvironment(DockerShell shell, BashExportBasedVisitor visitor) throws ContainerException {
    Map<String,String> envVarsStaticTmp = buildTranslatedStaticEnvMap();

    executeEnvSetting(EnvType.STATIC, envVarsStaticTmp);
  }

  private Map<String,String> buildTranslatedStaticEnvMap() {
    Map<String,String> envVarsStaticTmp = new HashMap<>();

    for(Entry<String, String> entry: envVarsStatic.entrySet()) {
      //todo: not needed in post-colosseum version, as the environment-var names should be set correctly then
      if(!translateMap.containsKey(entry.getKey()))
        continue;

      envVarsStaticTmp.put(translateMap.get(entry.getKey()),entry.getValue());
    }

    return envVarsStaticTmp;
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
  public AbstractComponent getComponent() {
    return myComponent;
  }

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
      dynHandler.run();
    } catch (DockerCommandException e) {
      throw new ContainerException("Cannot initialize Dyn Handler. Problems setting the correct container name...", e);
    }
  }

  @Override
  public void doStopDynHandling() throws ContainerException {
    dynHandler.setRunning(false);
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

  private void executeGenericStart() throws ContainerException {
    DockerCommand runCmd = myComponent.getEntireDockerCommands().getRun();
    try {
      resolveDockerEnvVars(runCmd);
      copyEnvIntoCommand(runCmd);
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.REMOVE));
      final String cmdStr = myComponent.getFullDockerCommand(DockerCommand.Type.RUN);
      LOGGER.debug(String
      .format("Redeploying container %s with docker cli command: %s.", myId, cmdStr));
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.RUN));
      BashExportBasedVisitor visitor = new BashExportBasedVisitor(null);
      setDynamicEnvironment(visitor, null);
    } catch (DockerException de) {
      throw new ContainerException("cannot redeploy container: " + myId, de);
    } catch (DockerCommandException e) {
      throw new ContainerException("cannot redeploy container " + myId + " because of failing to create the run command", e);
    }
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
      resolveDockerEnvVars(runCmd);
      copyEnvIntoCommand(runCmd);
    } catch (DockerCommandException e) {
      throw new ContainerException("cannot redeploy container " + myId + " because of failing to create the run command", e);
    }
    executeGenericRedeploy();
  }

  //Needed if a Docker env-var depends on a lance-internal env-var, e.g. PUBLIC_ReqPort=134.60.64.1:3302
  private void resolveDockerEnvVars(DockerCommand cmd) throws DockerCommandException {
    Map<Option, List<String>> setOptions = cmd.getSetOptions();
    List<String> setDockerEnvVars = setOptions.get(Option.ENVIRONMENT);

    //Map entry for ENV_DOCK=$ENV_LANCE: entry.key()=="ENV_DOCK", entry.val()=="ENV_LANCE"
    Map<String,String> filterNeedResolveDockerVarNames = getFilterNeedResolveDockerVarNames(setDockerEnvVars);

    for(Entry<String, String> vars: filterNeedResolveDockerVarNames.entrySet()) {
      String resolvedVarVal = findVarVal(vars.getValue().trim());
      if(resolvedVarVal.equals("")) {
        continue;
      }
      String newEnvVar = vars.getKey() + "=" + resolvedVarVal;
      //todo: escape regex special-chars in String
      cmd.replaceEnvVar(newEnvVar);
    }
  }

  //checks only lance-inernal (static/dynamic) env-vars
  private String findVarVal(String needResolveVarName) {
    //concatenate static and dynamic env vars
    Map<String,String> concatMap = buildTranslatedStaticEnvMap();
    concatMap.putAll(envVarsDynamic);

    for(Entry<String, String> var: concatMap.entrySet()) {
      if (var.getKey().equals(needResolveVarName)) {
        return var.getValue();
      }
    }

    return "";
  }

  private static Map<String,String> getFilterNeedResolveDockerVarNames(List<String> setDockerEnvVars) {
    Map<String,String> needResolveDockerVarNames = new HashMap<>();
    //todo: make pattern more general, e.g. "$..." ,"${...}"
    Pattern pattern = Pattern.compile("^[\\s]*([^\\s]+)=\\$([^\\s]+)[\\s]*$");

    if(setDockerEnvVars == null) {
      return needResolveDockerVarNames;
    }

    for(String envVar: setDockerEnvVars) {
      Matcher matcher = pattern.matcher(envVar);
      if (matcher.find()) {
        needResolveDockerVarNames.put(matcher.group(1),matcher.group(2));
      }
    }

    return needResolveDockerVarNames;
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

  private void copyEnvIntoCommand(DockerCommand dCmd) throws DockerCommandException {
    Map<String,String> envVarsStaticTmp = buildTranslatedStaticEnvMap();

    for(Entry<String, String> var: envVarsStaticTmp.entrySet()) {
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
