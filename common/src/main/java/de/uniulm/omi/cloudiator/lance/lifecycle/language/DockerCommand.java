package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import static de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand.EMPTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerCommand implements Serializable {

  public enum Option {
    NAME, PORT, RESTART, INTERACTIVE, NETWORK, ENVIRONMENT, TTY, DETACH, VOLUME};
  public enum OsCommand {
    EMPTY, BASH
  }

  public enum Type {
    CREATE(new Option[]{Option.NAME, Option.PORT, Option.RESTART,Option.INTERACTIVE, Option.ENVIRONMENT,
        Option.NETWORK, Option.TTY, Option.VOLUME}, new OsCommand[]{OsCommand.BASH}),
    START(new Option[]{Option.INTERACTIVE}, new OsCommand[]{}),
    STOP(new Option[]{}, new OsCommand[]{}),
    RUN(new Option[]{Option.NAME, Option.PORT, Option.INTERACTIVE, Option.ENVIRONMENT,
        Option.NETWORK, Option.DETACH, Option.VOLUME}, new OsCommand[]{}),
    REMOVE(new Option[]{}, new OsCommand[]{});

    private static final String createCommandName = "create";
    private static final String startCommandName = "start";
    private static final String stopCommandName = "stop";
    private static final String runCommandName = "run";
    private static final String removeCommandName = "rm";

    private final Set<Option> possibleOptions;
    private final Set<OsCommand> possibleCommands;

    Type(Option[] opts, OsCommand[] commands) {
      possibleOptions = new HashSet<>(Arrays.asList(opts));
      possibleCommands = new HashSet<>(Arrays.asList(commands));
    }

    public static String mapCommandToString(DockerCommand.Type cType) throws IllegalArgumentException {
      if(cType==DockerCommand.Type.CREATE)
        return createCommandName;
      if(cType==DockerCommand.Type.START)
        return startCommandName;
      if(cType==DockerCommand.Type.STOP)
        return stopCommandName;
      if(cType==DockerCommand.Type.RUN)
        return runCommandName;
      if(cType==DockerCommand.Type.REMOVE)
        return removeCommandName;
      else
        //todo insert String representation of DockerCommand in exception String
        throw new IllegalArgumentException("No mapping for this Docker Command available");
    }

    public boolean isAllowedOpt(DockerCommand.Option opt) {
      if(possibleOptions.contains(opt)) {
        return true;
      }

      return false;
    }

    public boolean isAllowedOsCommand(DockerCommand.OsCommand osCmd) {
      if(possibleCommands.contains(osCmd)) {
        return true;
      }

      return false;
    }
  };

  private static final long serialVersionUID = -8890385235481216602L;

  public final Type cmdType;
  private Map<Option, List<String>> setOptions;
  private List<OsCommand> setCommand;
  private List<String> setArgs;

  public Map<Option, List<String>> getSetOptions() {
    return setOptions;
  }

  public List<OsCommand> getSetCommand() {
    return setCommand;
  }

  public List<String> getSetArgs() {
    return setArgs;
  }

  private DockerCommand(Type cmdType, Option[] opts, OsCommand[] commands, String cName) {
    this.cmdType = cmdType;
    setOptions = new HashMap<>();
    setCommand = new ArrayList<>();
    setArgs = new ArrayList<>();
  }

  private DockerCommand(Builder builder) {
    cmdType = builder.cmdType;
    setOptions = builder.setOptions;
    setCommand = builder.setCommand;
    setArgs = builder.setArgs;
  }

  public void setSetOptions(
      Map<Option, List<String>> setOptions) throws DockerCommandException {
    for (Entry<Option, List<String>> optEntry : setOptions.entrySet()) {
      DockerCommand.Option opt = optEntry.getKey();
      if (!cmdType.possibleOptions.contains(opt))
        throw new DockerCommandException(
            "Option "
                + opt.name()
                + " does not exist for '"
                + DockerCommand.Type.mapCommandToString(cmdType)
                + "' command");
    }

    this.setOptions = setOptions;
  }

  public void setSetCommand(
      List<OsCommand> setCommand) throws DockerCommandException {
    for (OsCommand cmd : setCommand) {
      if (!cmdType.possibleCommands.contains(cmd))
        throw new DockerCommandException("Command " + cmd.name() + " does not exist for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");
    }
    this.setCommand = setCommand;
  }

  public void setSetArgs(List<String> setArgs) throws DockerCommandException {
    if(!argsAllowed())
      throw new DockerCommandException("Args not allowed for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");

    if(setCommand.size() == 0)
      throw new DockerCommandException("Args cannot be set when there is no Os Command set");

    this.setArgs = setArgs;
  }

  public void setOption(Option opt, String arg) throws DockerCommandException {
    if(!cmdType.possibleOptions.contains(opt))
      throw new DockerCommandException("Option " + opt.name() + " does not exist for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");

    List<String> lst;

    if(setOptions.get(opt) == null) {
      lst = new ArrayList<>();
    } else {
      lst = new ArrayList<>(setOptions.get(opt));
    }

    lst.add(arg);
    setOptions.put(opt,lst);
  }

  public void replaceEnvVar(String envVar) throws DockerCommandException {
    if(!cmdType.possibleOptions.contains(Option.ENVIRONMENT))
      throw new DockerCommandException(cmdType.name() + "cannot have environmental variables set");

    List<String> lst;

    if (setOptions.get(Option.ENVIRONMENT) == null) {
      throw new DockerCommandException( "Cannot override an environmental variable with " + envVar
              + " when there are no variables set yet");
    }

    lst = new ArrayList<>(setOptions.get(Option.ENVIRONMENT));

    //todo: escape regex special-chars in String
    int index = getEnvVarIndex(setOptions.get(Option.ENVIRONMENT), envVar);

    if(index == -1) {
      throw new DockerCommandException("Cannot override an environmental variable with " + envVar + ", because it does not exist.");
    }

    lst.set(index, envVar);
    setOptions.put(Option.ENVIRONMENT,lst);
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

  public void setOsCommand(OsCommand cmd) throws DockerCommandException {
    if(!cmdType.possibleCommands.contains(cmd))
      throw new DockerCommandException("Command " + cmd.name() + " does not exist for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");

    //only one OsCommand allowed
    if(setCommand.size()>0)
      setCommand.remove(setCommand.size()-1);

    setCommand.add(cmd);
  }

  public void setArg(String arg) throws DockerCommandException {
    if(!argsAllowed())
      throw new DockerCommandException("Arg " + arg + " not allowed for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");

    if(setCommand.size() == 0)
      throw new DockerCommandException("Cannot set an arg, when there is no Os Command set for" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");

    setArgs.add(arg);
  }

  public String getSetOptionsString() {
    StringBuilder builder = new StringBuilder();

    for (Map.Entry<Option, List<String>> kv : setOptions.entrySet()) {
      for (String str : kv.getValue()) {
        builder.append(getOptionString(kv.getKey()) + " " + str + " ");
      }
    }

    //strip last blank
    if(builder.length() > 0)
      builder.setLength(builder.length() - 1);

    return builder.toString();
  }

  public String getSetOsCommandString() {
    if(setCommand.size()==0)
     return getOsCommandString(EMPTY);
    //only one OsCommand
    return getOsCommandString(setCommand.get(0));
  }

  public String getSetArgsString() {
    StringBuilder builder = new StringBuilder();

    for (String arg: setArgs) {
      builder.append(arg + " ");
    }

    //strip last blank
    if(builder.length() > 0)
      builder.setLength(builder.length() - 1);

    return builder.toString();
  }

  private String getOptionString(Option opt) {
    switch (opt) {
      case NAME:
        return "--name";
      case PORT:
        return "--publish";
      case RESTART:
        return "--restart";
      case INTERACTIVE:
        return "--interactive";
      case NETWORK:
        return "--network";
      case ENVIRONMENT:
        return "--env";
      case DETACH:
        return "--detach";
      case VOLUME:
        return "--volume";
      default:
        return "";
    }
  }

  private String getOsCommandString(OsCommand cmd) {
    switch (cmd) {
      case BASH:
        return "bash";
      default:
        return "";
    }
  }

  private boolean argsAllowed() {
    if(cmdType==Type.CREATE || cmdType==Type.RUN)
      return true;

    return false;
  }

  //"helper-method" to get the Name for commands: START, STOP
  String getContainerName() throws DockerCommandException {
    if(!this.setOptions.containsKey(Option.NAME))
      throw new DockerCommandException("NAME option not set for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");

    return this.setOptions.get(Option.NAME).get(0);
  }

  public static class Builder {
    private final Type cmdType;

    private final Map<Option, List<String>> setOptions;
    private final List<OsCommand> setCommand;
    private final List<String> setArgs;

    public Builder(DockerCommand.Type cmdType) {
      this.cmdType = cmdType;
      setOptions = new HashMap<>();
      setCommand = new ArrayList<>();
      setArgs = new ArrayList<>();
    }

    public Builder setOptions(Map<Option,List<String>> opts) throws DockerCommandException  {
      checkMapKeysInSet(opts, cmdType.possibleOptions);

      setOptions.putAll(opts);
      return this;
    }

    public Builder setCommand(List<OsCommand> cmd) throws DockerCommandException {
      isSubset(cmdType.possibleCommands,cmd);
      //only zero/one OsCommand allowed
      if(cmd.size()>1)
        throw new DockerCommandException("Only one Command allowed for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");

      setCommand.addAll(cmd);
      return this;
    }

    public Builder setArgs(List<String> args) throws DockerCommandException {
      //todo: check if args allowed within this DockerCommand
      setArgs.addAll(args);
      return this;
    }

    public DockerCommand build() {
      return new DockerCommand(this);
    }

    void checkMapKeysInSet(Map<Option,List<String>> map, Set<Option> set) throws DockerCommandException {
      for (Map.Entry<Option, List<String>> kv : map.entrySet()) {
        if(!set.contains(kv.getKey()))
          throw new DockerCommandException("Option " + kv.getKey().name() + " does not exist for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");
      }
    }

    void isSubset(Set<OsCommand> origSet, List<OsCommand> possibleSubSet) throws DockerCommandException {
      for (OsCommand osCmd: possibleSubSet) {
        if(!origSet.contains(osCmd))
          throw new DockerCommandException("Command " + osCmd.name() + " does not exist for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");
      }
    }
  }
}
