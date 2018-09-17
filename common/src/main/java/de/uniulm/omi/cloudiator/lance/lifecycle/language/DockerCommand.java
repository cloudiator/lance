package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import static de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand.EMPTY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//todo: build DockerCommandException
public enum DockerCommand {
  CREATE(new Option[]{Option.NAME, Option.PORT, Option.RESTART, Option.INTERACTIVE, Option.ENVIRONMENT, Option.TTY}, new OsCommand[]{OsCommand.BASH}),
  START(new Option[]{Option.INTERACTIVE, Option.NETWORK}, new OsCommand[]{}),
  STOP(new Option[]{}, new OsCommand[]{});

  private final Set<Option> possibleOptions;
  private final Set<OsCommand> possibleCommands;

  private Map<Option, String> setOptions;
  private OsCommand setCommand;
  private List<String> setArgs;

  private DockerCommand(Option[] opts, OsCommand[] commands) {
    possibleOptions = new HashSet<>(Arrays.asList(opts));
    possibleCommands = new HashSet<>(Arrays.asList(commands));

    setOptions = new HashMap<>();
    setCommand = EMPTY;
    setArgs = new ArrayList<>();
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

   setCommand = cmd;
  }

  public void setArg(String arg) {
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
    return getOsCommandString(setCommand);
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
        return "--port";
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
}
