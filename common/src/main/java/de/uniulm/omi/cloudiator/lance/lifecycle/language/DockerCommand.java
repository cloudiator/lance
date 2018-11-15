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

public class DockerCommand implements Serializable{

  public enum Option {
    NAME, PORT, RESTART, INTERACTIVE, NETWORK, ENVIRONMENT, TTY;
  }

  public enum OsCommand {
    EMPTY, BASH
  }

  public enum Type {
    CREATE(new Option[]{Option.NAME, Option.PORT, Option.RESTART,Option.INTERACTIVE, Option.ENVIRONMENT,
        Option.NETWORK, Option.TTY}, new OsCommand[]{OsCommand.BASH}, createCommandName),
    START(new Option[]{Option.INTERACTIVE}, new OsCommand[]{}, startCommandName),
    STOP(new Option[]{}, new OsCommand[]{}, stopCommandName),
    RUN(new Option[]{Option.NAME, Option.PORT, Option.RESTART,Option.INTERACTIVE, Option.ENVIRONMENT,
        Option.NETWORK, Option.TTY}, new OsCommand[]{OsCommand.BASH}, createCommandName);

    private final Set<Option> possibleOptions;
    private final Set<OsCommand> possibleCommands;
    private final String dockerCommandName;

    Type(Option[] opts, OsCommand[] commands, String cName) {
      possibleOptions = new HashSet<>(Arrays.asList(opts));
      possibleCommands = new HashSet<>(Arrays.asList(commands));
      dockerCommandName = cName;
    }
  };

  private static final String createCommandName = "docker create";
  private static final String startCommandName = "docker start";
  private static final String stopCommandName = "docker stop";
  private static final String runCommandName = "docker run";
  private static final long serialVersionUID = -8890385235481216602L;


  public final Type cmdType;
  private final Map<Option, List<String>> setOptions;
  private final List<OsCommand> setCommand;
  private final List<String> setArgs;

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

  public void setOption(Option opt, String arg) throws DockerCommandException {
    if(!cmdType.possibleOptions.contains(opt))
      throw new DockerCommandException("Option " + opt.name() + " does not exist for '" + cmdType.dockerCommandName + "' command");

    List<String> lst = setOptions.get(opt);

    if(lst == null) {
      lst = new ArrayList<>();
    }

    lst.add(arg);
    setOptions.put(opt,lst);
  }

  public void setOsCommand(OsCommand cmd) throws DockerCommandException {
    if(!cmdType.possibleCommands.contains(cmd))
      throw new DockerCommandException("Command " + cmd.name() + " does not exist for '" + cmdType.dockerCommandName + "' command");

    //only one OsCommand allowed
    if(setCommand.size()>0)
      setCommand.remove(setCommand.size()-1);

    setCommand.add(cmd);
  }

  public void setArg(String arg) throws DockerCommandException {
    if(!argsAllowed())
      throw new DockerCommandException("Arg " + arg + " not allowed for '" + cmdType.dockerCommandName + "' command");

    setArgs.add(arg);
  }

  public String getSetOptionsString() {
    StringBuilder builder = new StringBuilder();

    for (Map.Entry<Option, List<String>> kv : setOptions.entrySet()) {
      for (String str : kv.getValue()) {
        builder.append(getOptionString(kv.getKey()) + " " + str + " ");
      }
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
    if(cmdType==Type.CREATE || cmdType==Type.RUN)
      return true;

    return false;
  }

  //"helper-method" to get the Name for commands: START, STOP
  String getContainerName() throws DockerCommandException {
    if(!this.setOptions.containsKey(Option.NAME))
      throw new DockerCommandException("NAME option not set for '" + cmdType.dockerCommandName + "' command");

    return this.setOptions.get(Option.NAME).get(0);
  }

  public static class Builder {
    private final Type cmdType;

    private Map<Option, List<String>> setOptions;
    private List<OsCommand> setCommand;
    private List<String> setArgs;

    public Builder(DockerCommand.Type cmdType) {

      /*setOptions = new HashMap<>(cmd.setOptions);
      setCommand = new ArrayList<>(cmd.setCommand);
      setArgs = new ArrayList<>(cmd.setArgs);*/
      this.cmdType = cmdType;
    }

    public Builder setOptions(Map<Option,List<String>> opts) throws DockerCommandException  {
      checkMapKeysInSet(opts, cmdType.possibleOptions);
      setOptions = new HashMap<>(opts);
      return this;
    }

    public Builder setCommand(List<OsCommand> cmd) throws DockerCommandException {
      isSubset(cmdType.possibleCommands,cmd);
      //only zero/one OsCommand allowed
      if(cmd.size()>1)
        throw new DockerCommandException("Only one Command allowed for '" + cmdType.dockerCommandName + "' command");

      setCommand = new ArrayList<>(cmd);
      return this;
    }

    public Builder setArgs(List<String> args) throws DockerCommandException {
      //todo: check if args allowed within this DockerCommand
      setArgs = new ArrayList<>(args);
      return this;
    }

    public DockerCommand build() {
      return new DockerCommand(this);
    }

    void checkMapKeysInSet(Map<Option,List<String>> map, Set<Option> set) throws DockerCommandException {
      for (Map.Entry<Option, List<String>> kv : map.entrySet()) {
        if(!set.contains(kv.getKey()))
          throw new DockerCommandException("Option " + kv.getKey().name() + " does not exist for '" + cmdType.dockerCommandName + "' command");
      }
    }

    void isSubset(Set<OsCommand> origSet, List<OsCommand> possibleSubSet) throws DockerCommandException {
      for (OsCommand osCmd: possibleSubSet) {
        if(!origSet.contains(osCmd))
          throw new DockerCommandException("Command " + osCmd.name() + " does not exist for '" + cmdType.dockerCommandName + "' command");
      }
    }
  }
}
