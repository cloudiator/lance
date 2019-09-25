package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.COMPONENT_INSTANCE_STATUS;
import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.DYN_GROUP_KEY;
import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.DYN_HANDLER_KEY;

import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerContainerLogic extends AbstractDockerContainerLogic {
  private final static String invalidIp = "0.0.0.0";
  private final DockerComponent myComponent;
  protected final DockerImageHandler imageHandler;
  //Needed to check for redeployment
  private Map<String, String> envVarsStaticPrev;
  //Needed to check for redeployment
  private Map<String, String> envVarsDynamicPrev;
  private DockerDynHandler dynHandler;
  private Thread dynThread;

  private enum EnvType {STATIC, DYNAMIC};

  public DockerContainerLogic(Builder builder) {
    super(builder);
    this.myComponent = builder.myComponent;
    this.imageHandler = new DockerImageHandler(builder.client, builder.myComponent,
        builder.dockerConfig);
    try {
      myComponent.setContainerName(this.myId);
    } catch (DockerCommandException ce) {
      LOGGER.error("Cannot set name for Docker container for component:" + myId, ce);
    }

    envVarsStaticPrev = new HashMap<>(envVarsStatic);
    envVarsDynamicPrev = new HashMap<>(envVarsDynamic);
    //Getting initialized dynamically
    dynHandler = null;
    //todo: check if must be initialised in doStart method
    dynThread = null;
    //doesn't copy lance internal env-vars, just the docker ones
    //lance internal env-vars will be copied when an environment-variable changes a value and redeployment is triggered
    initRedeployDockerCommands();
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      imageHandler.doPullImages(myComponent.getFullImageName());
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
    setGlobalContainerEnv(EnvType.STATIC, envVarsStaticTmp);
    setLocalContainerEnv(EnvType.STATIC, shell, visitor);
    envVarsStaticPrev = new HashMap<>(envVarsStatic);
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
    setGlobalContainerEnv(EnvType.DYNAMIC, envVarsDynamic);
    setLocalContainerEnv(EnvType.DYNAMIC, null, visitor);
    envVarsDynamicPrev = new HashMap<>(envVarsDynamic);
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
      .format("Starting container %s with docker cli command: %s.", myId, cmdStr));
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.Type.RUN));
    } catch (DockerException de) {
      throw new ContainerException("cannot redeploy container: " + myId, de);
    } catch (DockerCommandException e) {
      throw new ContainerException("cannot redeploy container " + myId + " because of failing to create the run command", e);
    }
  }

  // env change implies redeployment (global env is set then) if no updatehandler is present
  private void setGlobalContainerEnv(EnvType eType, Map<String,String> vars) throws ContainerException {
    if(checkEnvChange(eType) && noPortUpdateHandlerPresent()) {
      try {
        doRedeploy();
      } catch (ContainerException e) {
        LOGGER.error("cannot redeploy container " + myId + "for updating the environment");
      }
    }
  }

  // if updatehandler is present, set environment (even if not changed) in local (docker) shell
  private void setLocalContainerEnv(EnvType eType, DockerShell shell, BashExportBasedVisitor visitor) throws ContainerException {
    if(! noPortUpdateHandlerPresent()) {
      if(eType == EnvType.STATIC) {
        super.setStaticEnvironment(shell,visitor);
      } else {
        networkHandler.accept(visitor);
        this.myComponent.accept(visitor);
      }
    }
  }

  private boolean noPortUpdateHandlerPresent() {
    List<OutPort> outPorts = myComponent.getDownstreamPorts();

    for(OutPort port: outPorts) {
      PortUpdateHandler handler = port.getUpdateHandler();
      if(handler != null && !handler.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private void doRedeploy() throws ContainerException {
    DockerCommand runCmd = myComponent.getEntireDockerCommands().getRun();
    try {
      resolveDockerEnvVars(runCmd);
      copyEnvIntoCommand(runCmd);
      executeGenericRedeploy();
    } catch (DockerCommandException e) {
      throw new ContainerException("cannot redeploy container " + myId + " because of failing to create the run command", e);
    }
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

  private void resolveAddHostOption(DockerCommand cmd) throws DockerCommandException {
    Map<Option, List<String>> setOptions = cmd.getSetOptions();
    List<String> setDockerHostVars = setOptions.get(Option.ADD_HOST);

    Map<String,String> filterNeedResolveAddHostNames = getFilterNeedResolveAddHostNames(setDockerHostVars);

    for(Entry<String, String> vars: filterNeedResolveAddHostNames.entrySet()) {
      String resolvedVarVal = findVarVal(vars.getValue().trim());
      String newHostVar = vars.getKey() + ":" + invalidIp;
      if(!resolvedVarVal.equals("")) {
        newHostVar = vars.getKey() + ":" + resolvedVarVal;
      }
      //todo: escape regex special-chars in String
      cmd.replaceAddHostVar(newHostVar);
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

  private static Map<String,String> getFilterNeedResolveAddHostNames(List<String> setDockerHostVars) {
    Map<String,String> needResolveAddHostNames = new HashMap<>();
    //todo: make pattern more general, e.g. "$..." ,"${...}"
    Pattern pattern1 = Pattern.compile("^[\\s]*([^\\s]+):\\$([^\\s]+)[\\s]*$");
    Pattern pattern2 = Pattern.compile("^[\\s]*([^\\s]+):" + invalidIp + "[\\s]*$");

    if(setDockerHostVars == null) {
      return needResolveAddHostNames ;
    }

    for(String hostVar: setDockerHostVars) {
      Matcher matcher1 = pattern1.matcher(hostVar);
      Matcher matcher2 = pattern2.matcher(hostVar);
      if (matcher1.find()) {
        needResolveAddHostNames.put(matcher1.group(1), matcher1.group(2));
      } else if (matcher2.find()) {
        needResolveAddHostNames.put(matcher2.group(1),"CLOUD_IP");
      }
    }

    return needResolveAddHostNames;
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
