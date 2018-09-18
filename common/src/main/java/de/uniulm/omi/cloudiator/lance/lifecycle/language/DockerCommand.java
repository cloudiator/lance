package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import static de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand.EMPTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//todo: build DockerCommandException
public class DockerCommand implements Serializable{
  public final static DockerCommand CREATE = new DockerCommand(new Option[]{Option.NAME, Option.PORT, Option.RESTART,
      Option.INTERACTIVE, Option.ENVIRONMENT, Option.NETWORK, Option.TTY}, new OsCommand[]{OsCommand.BASH});
  public final static DockerCommand START = new DockerCommand(new Option[]{Option.INTERACTIVE}, new OsCommand[]{});
  public final static DockerCommand STOP = new DockerCommand(new Option[]{}, new OsCommand[]{});
  private static final long serialVersionUID = 1509736151696237288L;

  private final Set<Option> possibleOptions;
  private final Set<OsCommand> possibleCommands;

  private final Map<Option, String> setOptions;
  private final List<OsCommand> setCommand;
  private final List<String> setArgs;

  private DockerCommand(Option[] opts, OsCommand[] commands) {
    possibleOptions = new HashSet<>(Arrays.asList(opts));
    possibleCommands = new HashSet<>(Arrays.asList(commands));

    setOptions = new HashMap<>();
    setCommand = new ArrayList<>();
    setArgs = new ArrayList<>();
  }

  private DockerCommand(Builder builder) {
    possibleOptions = builder.possibleOptions;
    possibleCommands = builder.possibleCommands;

    setOptions = builder.setOptions;
    setCommand = builder.setCommand;
    setArgs = builder.setArgs;

  }

  public static enum Option {
    NAME, PORT, RESTART, INTERACTIVE, NETWORK, ENVIRONMENT, TTY;
  }

  public static enum OsCommand {
    EMPTY, BASH
  }

  //todo: include Enum-name (DockerCommand) and Enum-name(Option) in Exception String
  public void setOption(Option opt, String arg) throws Exception {
    if(!possibleOptions.contains(opt))
      throw new Exception("Option does not exist for this DockerCommand");

    setOptions.put(opt,arg);
  }

  //todo: include Enum-name (DockerCommand) and Enum-name(Command) in Exception String
  public void setOsCommand(OsCommand cmd) throws Exception {
    if(!possibleCommands.contains(cmd))
      throw new Exception("Command does not exist for this DockerCommand");

    //only one OsCommand allowed
    if(setCommand.size()>0)
      setCommand.remove(setCommand.size()-1);

    setCommand.add(cmd);
  }

  //todo: include Enum-name (DockerCommand) in Exception String
  public void setArg(String arg) throws Exception {
    if(!argsAllowed())
      throw new Exception("Cannot set args for this DockeOsCommand");

    setArgs.add(arg);
  }

  public String getSetOptionsString() {
    StringBuilder builder = new StringBuilder();

    for (Map.Entry<Option, String> kv : setOptions.entrySet()) {
      builder.append(getOptionString(kv.getKey()) + " " + kv.getValue() + " ");
    }
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
    if(this==CREATE)
      return true;

    return false;
  }

  //"helper-method" to get the Name for commands: START, STOP
  String getContainerName() throws IllegalStateException {
    if(!this.setOptions.containsKey(Option.NAME))
      throw new IllegalStateException("NAME option not settable for this Command");

    return this.setOptions.get(Option.NAME);
  }

  public static class Builder {
    private final Set<Option> possibleOptions;
    private final Set<OsCommand> possibleCommands;

    private Map<Option, String> setOptions;
    private List<OsCommand> setCommand;
    private List<String> setArgs;

    public Builder(DockerCommand cmd) {
      possibleOptions = new HashSet<>(cmd.possibleOptions);
      possibleCommands = new HashSet<>(cmd.possibleCommands);

      setOptions = new HashMap<>(cmd.setOptions);
      setCommand = new ArrayList<>(cmd.setCommand);
      setArgs = new ArrayList<>(cmd.setArgs);
    }

    public Builder setOptions(Map<Option,String> opts) throws Exception {
      if(!checkMapKeysinSet(opts, possibleOptions))
        throw new Exception("Option does not exist for this DockerCommand");

      setOptions = new HashMap<>(opts);
      return this;
    }

    public Builder setCommand(List<OsCommand> cmd) throws Exception {
      if(!isSubset(possibleCommands,cmd))
        throw new Exception("Command does not exist for this DockerCommand");

      //only zero/one OsCommand allowed
      if(cmd.size()>1)
        throw new Exception("Only one Command allowed for this DockerCommand");

      setCommand = new ArrayList<>(cmd);
      return this;
    }

    public Builder setArgs(List<String> args) throws Exception {
      //todo: check if args allowed within this DockerCommand
      setArgs = new ArrayList<>(args);
      return this;
    }

    public DockerCommand build() {
      return new DockerCommand(this);
    }
  }

  static boolean checkMapKeysinSet(Map<Option,String> map, Set<Option> set) {
    for (Map.Entry<Option, String> kv : map.entrySet()) {
      if(!set.contains(kv.getKey()))
        return false;
    }

    return true;
  }

  static boolean isSubset(Set<OsCommand> origSet, List<OsCommand> possibleSubSet) {
    for (OsCommand osCmd: possibleSubSet) {
      if(!origSet.contains(osCmd))
        return false;
    }

    return true;
  }
}
