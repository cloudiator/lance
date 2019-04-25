package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerEnvVarHandler {
  private Map<String,String> envVarsStatic;
  private Map<String,String> envVarsDynamic;
  //Needed to check for redeployment
  private Map<String, String> envVarsStaticPrev;
  //Needed to check for redeployment
  private Map<String, String> envVarsDynamicPrev;
  private final Map<String, String> translateMap;

  public enum EnvType {STATIC, DYNAMIC};

  DockerEnvVarHandler(Map<String, String> envVarsStatic, Map<String, String> envVarsDynamic,
      Map<String,String> translateMap) {
    this.envVarsStatic = envVarsStatic;
    this.envVarsDynamic = envVarsDynamic;
    envVarsStaticPrev = new HashMap<>(this.envVarsStatic);
    envVarsDynamicPrev = new HashMap<>(this.envVarsDynamic);
    this.translateMap = translateMap;
  }

  public boolean checkEnvChange(EnvType eType) {
    if(eType == EnvType.STATIC && envVarsStatic.equals(envVarsStaticPrev))
      return false;

    if(eType == EnvType.DYNAMIC && envVarsDynamic.equals(envVarsDynamicPrev))
      return false;

    return true;
  }

  public void setEnvMemory(EnvType eType) {
    if(eType == EnvType.STATIC)
      envVarsStaticPrev = new HashMap<>(envVarsStatic);
    else
      envVarsDynamicPrev = new HashMap<>(envVarsDynamic);
  }

  public DockerCommand copyLanceEnvIntoCommand(DockerCommand dCmd) throws DockerCommandException {
    //concatenate static and dynamic env vars
    Map<String,String> concatMap = buildTranslatedStaticEnvMap();
    concatMap.putAll(envVarsDynamic);

    for(Entry<String, String> var: concatMap.entrySet()) {
      final String envVarString = var.getKey().trim() + "=" + var.getValue().trim();
      if (DockerCommandUtils.optAndValIsSet(dCmd, Option.ENVIRONMENT, envVarString)) {
        dCmd = DockerCommandUtils.replaceEnvVar(dCmd, envVarString);
      } else {
        dCmd = DockerCommandUtils.appendOption(dCmd, Option.ENVIRONMENT, envVarString);
      }
    }
    return dCmd;
  }

  //Needed if a Docker env-var depends on a lance-internal env-var, e.g. PUBLIC_ReqPort=134.60.64.1:3302
  public DockerCommand resolveDockerEnvVars(DockerCommand cmd) throws DockerCommandException {
    Map<Option, List<String>> usedOptions = cmd.getUsedOptions();
    List<String> setDockerEnvVars = usedOptions.get(Option.ENVIRONMENT);

    //Map entry for ENV_DOCK=$ENV_LANCE: entry.key()=="ENV_DOCK", entry.val()=="ENV_LANCE"
    Map<String,String> filterNeedResolveDockerVarNames = getFilterNeedResolveDockerVarNames(setDockerEnvVars);

    for(Entry<String, String> vars: filterNeedResolveDockerVarNames.entrySet()) {
      String resolvedVarVal = findVarVal(vars.getValue().trim());
      if(resolvedVarVal.equals("")) {
        continue;
      }
      String newEnvVar = vars.getKey().trim() + "=" + resolvedVarVal.trim();
      //todo: escape regex special-chars in String
      cmd = DockerCommandUtils.replaceEnvVar(cmd, newEnvVar);
    }

    return cmd;
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

  public void generateDynamicEnvVars(DockerComponent myComponent,
      DeploymentContext deploymentContext, NetworkHandler networkHandler,
      PortDiff<DownstreamAddress> diff) {
    myComponent.injectDeploymentContext(deploymentContext);
    networkHandler.generateDynamicEnvVars(diff);
    myComponent.generateDynamicEnvVars();
    collectDynamicEnvVars(myComponent, networkHandler);
  }

  void collectDynamicEnvVars(DockerComponent myComponent,
      NetworkHandler networkHandler) {
    envVarsDynamic.putAll(myComponent.getEnvVars());
    envVarsDynamic.putAll(networkHandler.getEnvVars());
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
}
