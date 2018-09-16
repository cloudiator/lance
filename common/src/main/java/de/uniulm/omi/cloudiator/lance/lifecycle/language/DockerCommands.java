package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//todo: build DockerCommandException
public enum DockerCommands {
  CREATE(new Option[]{Option.NAME, Option.PORT, Option.RESTART, Option.INTERACTIVE, Option.NETWORK, Option.TTY}, new Command[]{Command.BASH}),
  START(new Option[]{Option.INTERACTIVE, Option.NETWORK}, new Command[]{}),
  STOP(new Option[]{}, new Command[]{});

  private final Set<Option> possibleOptions;
  private final Set<Command> possibleCommands;

  private Set<Option> setOption;
  private Command setCommand;
  private List<String> setArgs;

  private DockerCommands(Option[] opts, Command[] commands) {
    possibleOptions = new HashSet<>(Arrays.asList(opts));
    possibleCommands = new HashSet<>(Arrays.asList(commands));
  }

  public static enum Option {
    NAME, PORT, RESTART, INTERACTIVE, NETWORK, TTY;
  }

  public static enum Command {
    BASH
  }

  //todo: include Enum-name (DockerCommands) and Enum-name(Option) in Exception String
  public void setOption(Option opt) throws Exception {
    if(!possibleOptions.contains(opt))
      throw new Exception("Option does not exist for this DockerCommand");

    setOption.add(opt);
  }

  //todo: include Enum-name (DockerCommands) and Enum-name(Option) in Exception String
  public void setCommand(Command cmd) throws Exception {
    if(!possibleCommands.contains(cmd))
      throw new Exception("Command does not exist for this DockerCommand");

   setCommand = cmd;
  }

  public void setArg(String arg) {
    setArgs.add(arg);
  }

  //todo: implement
  public String getSetOptionsString() {
    return null;
  }

  //todo: implement
  public String getSetCommandString() {
    return null;
  }

  //todo: implement
  public String getSetArgsString() {
    return null;
  }
}
