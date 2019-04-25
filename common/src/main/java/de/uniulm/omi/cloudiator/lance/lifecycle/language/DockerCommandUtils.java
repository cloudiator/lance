package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import static de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand.EMPTY;

import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Builder;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerCommandUtils {

  //No instances of that class
  private DockerCommandUtils(){}

  public static DockerCommand appendOption(DockerCommand cmd, Option opt, String arg) throws DockerCommandException {
    DockerCommandParams params = new DockerCommandParams(cmd);

    if(!params.type.getPossibleOptions().contains(opt))
      throw new DockerCommandException("Option " + opt.name() + " does not exist for '" +
          DockerCommand.Type.mapCommandToString(params.type) + "' command");

    List<String> lst;

    if(params.usedOptions.get(opt) == null) {
      lst = new ArrayList<>();
    } else {
      lst = new ArrayList<>(params.usedOptions.get(opt));
    }

    lst.add(arg);
    Map<DockerCommand.Option, List<String>> modOpts = new HashMap<>(params.usedOptions);
    modOpts.put(opt,lst);

    DockerCommand.Builder builder = new Builder(params.type);
    DockerCommand modifiedCmd = builder.usedOptions(modOpts).osCommand(params.osCommand)
        .usedArgs(params.usedArgs).build();

    return modifiedCmd;
  }

  public static DockerCommand appendArg(DockerCommand cmd, String arg) throws DockerCommandException {
    DockerCommandParams params = new DockerCommandParams(cmd);

    if(!DockerCommand.argsAllowed(cmd.cmdType))
      throw new DockerCommandException("Args not allowed for '" + DockerCommand.Type.mapCommandToString(cmd.cmdType) + "' command");

    List<String> lst;

    if(params.usedArgs == null) {
      lst = new ArrayList<>();
    } else {
      lst = new ArrayList<>(params.usedArgs);
    }

    lst.add(arg);

    DockerCommand.Builder builder = new Builder(params.type);
    DockerCommand modifiedCmd = builder.usedOptions(params.usedOptions).osCommand(params.osCommand)
        .usedArgs(lst).build();

    return modifiedCmd;
  }

  public static DockerCommand replaceEnvVar(DockerCommand cmd, String envVar) throws DockerCommandException {
    DockerCommandParams params = new DockerCommandParams(cmd);

    if(!params.type.getPossibleOptions().contains(Option.ENVIRONMENT))
      throw new DockerCommandException(params.type.name() + "cannot have environmental variables set");

    List<String> lst;

    if (params.usedOptions.get(Option.ENVIRONMENT) == null) {
      throw new DockerCommandException( "Cannot override an environmental variable with " + envVar
          + " when there are no variables set yet");
    }

    lst = new ArrayList<>(params.usedOptions.get(Option.ENVIRONMENT));

    //todo: escape regex special-chars in String
    int index = getEnvVarIndex(lst, envVar);

    if(index == -1) {
      throw new DockerCommandException("Cannot override an environmental variable with " + envVar + ", because it does not exist.");
    }

    lst.set(index, envVar);
    Map<DockerCommand.Option, List<String>> modOpts = new HashMap<>(params.usedOptions);
    modOpts.put(Option.ENVIRONMENT,lst);

    DockerCommand.Builder builder = new Builder(params.type);
    DockerCommand modifiedCmd = builder.usedOptions(modOpts).osCommand(params.osCommand)
        .usedArgs(params.usedArgs).build();

    return modifiedCmd;
  }

  public static boolean optIsSet(DockerCommand cmd, DockerCommand.Option opt) {
    DockerCommandParams params = new DockerCommandParams(cmd);

    if(params.usedOptions.get(opt) == null) {
      return false;
    }
    return true;
  }

  public static boolean optAndValIsSet(DockerCommand cmd, DockerCommand.Option opt, String val) {
    DockerCommandParams params = new DockerCommandParams(cmd);

    if(params.usedOptions.get(opt) == null) {
      return false;
    }

    List<String> lst = new ArrayList<>(params.usedOptions.get(opt));

    for(String str: lst) {
      if(str.trim().equals(val.trim()))
         return true;
    }
    return false;
  }

  private static int getEnvVarIndex(List<String> envVars, String replaceVar) {
    String[] parts = replaceVar.split("=");
    String replaceVarName = parts[0];
    //todo: make pattern more general, e.g. "$..." ,"${...}"
    //todo: escape regex special-chars in replaceVarName
    final String regexStr = "^[\\s]*" + replaceVarName +"=.*$";
    Pattern pattern = Pattern.compile(regexStr);

    for(int i=0; i<envVars.size(); ++i) {
      Matcher matcher = pattern.matcher(envVars.get(i));
      if (matcher.find()) {
        return i;
      }
    }

    return -1;
  }

  public static String getSetOptionsString(DockerCommand cmd) {
    DockerCommandParams params = new DockerCommandParams(cmd);

    StringBuilder builder = new StringBuilder();

    for (Map.Entry<Option, List<String>> kv : params.usedOptions.entrySet()) {
      DockerCommand.Option opt = kv.getKey();
      for (String str : kv.getValue()) {
        builder.append(opt.getOptionString() + " " + str + " ");
      }
    }

    //strip last blank
    if(builder.length() > 0)
      builder.setLength(builder.length() - 1);

    return builder.toString();
  }

  public static String getSetOsCommandString(DockerCommand cmd) {
    DockerCommandParams params = new DockerCommandParams(cmd);

    if(params.osCommand.size()==0)
      return OsCommand.EMPTY.getOsCommandString();
    //only one OsCommand
    return params.osCommand.get(0).getOsCommandString();
  }

  public static String getSetArgsString(DockerCommand cmd) {
    DockerCommandParams params = new DockerCommandParams(cmd);

    StringBuilder builder = new StringBuilder();

    for (String arg: params.usedArgs) {
      builder.append(arg + " ");
    }

    //strip last blank
    if(builder.length() > 0)
      builder.setLength(builder.length() - 1);

    return builder.toString();
  }

  public static String getFullDockerCommandString(EntireDockerCommands dStack, DockerCommand.Type cType, String fullIdentifier) throws DockerCommandException {
    StringBuilder builder = new StringBuilder();
    builder.append(DockerCommand.Type.mapCommandToString(cType) + " ");
    builder.append(dStack.getSetOptionsString(cType) + " ");
    builder.append(fullIdentifier + " ");
    builder.append(dStack.getSetOsCommandString(cType) + " ");
    builder.append(dStack.getSetArgsString(cType));

    return builder.toString();
  }


  //"helper-method" to get the Name for commands: START, STOP
  public static String getContainerName(DockerCommand cmd) throws DockerCommandException {
    DockerCommandParams params = new DockerCommandParams(cmd);

    if(!params.usedOptions.containsKey(Option.NAME))
      throw new DockerCommandException("NAME option not set for '" + DockerCommand.Type.mapCommandToString(params.type) + "' command");

    return params.usedOptions.get(Option.NAME).get(0);
  }

  private static class DockerCommandParams {
    private final DockerCommand.Type type;
    private final Map<Option,List<String>> usedOptions;
    private final List<OsCommand> osCommand;
    private final List<String> usedArgs;

    private DockerCommandParams(DockerCommand cmd) {
      this.type = cmd.cmdType;
      this.usedOptions = cmd.getUsedOptions();
      this.osCommand = cmd.getOsCommand();
      this.usedArgs = cmd.getUsedArgs();
    }
  }
}
