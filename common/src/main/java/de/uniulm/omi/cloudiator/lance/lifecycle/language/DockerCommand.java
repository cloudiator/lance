package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import static de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand.EMPTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    NAME, PORT, RESTART, INTERACTIVE, NETWORK, ENVIRONMENT, TTY, DETACH, VOLUME;

    private static Map<DockerCommand.Option,String> optionStringMap;

    static {
      optionStringMap = new HashMap<>();
      optionStringMap.put(DockerCommand.Option.NAME, "--name");
      optionStringMap.put(DockerCommand.Option.PORT, "--publish");
      optionStringMap.put(DockerCommand.Option.RESTART, "--restart");
      optionStringMap.put(DockerCommand.Option.INTERACTIVE, "--interactive");
      optionStringMap.put(DockerCommand.Option.NETWORK, "--network");
      optionStringMap.put(DockerCommand.Option.ENVIRONMENT, "--env");
      optionStringMap.put(DockerCommand.Option.TTY, "--tty");
      optionStringMap.put(DockerCommand.Option.DETACH, "--detach");
      optionStringMap.put(DockerCommand.Option.VOLUME, "--volume");
      optionStringMap = Collections.unmodifiableMap(optionStringMap);
    }

    public String getOptionString() {
      String res = optionStringMap.get(this);

      if (res == null) {
        return "";
      }

      return res;
    }
  }

  public enum OsCommand {
    EMPTY, BASH;

    private static Map<DockerCommand.OsCommand,String> osCommandStringMap;

    static {
      osCommandStringMap = new HashMap<>();
      osCommandStringMap.put(DockerCommand.OsCommand.EMPTY, "");
      osCommandStringMap.put(DockerCommand.OsCommand.BASH, "bash");
      osCommandStringMap = Collections.unmodifiableMap(osCommandStringMap);
    }

    public String getOsCommandString() {
      String res = osCommandStringMap.get(this);

      if (res == null) {
        return "";
      }

      return res;
    }
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

    private static Map<DockerCommand.Type,String> cmdStringMap;

    static {
      cmdStringMap = new HashMap<>();
      cmdStringMap.put(DockerCommand.Type.CREATE, createCommandName);
      cmdStringMap.put(DockerCommand.Type.START, startCommandName);
      cmdStringMap.put(DockerCommand.Type.STOP, stopCommandName);
      cmdStringMap.put(DockerCommand.Type.RUN, runCommandName);
      cmdStringMap.put(DockerCommand.Type.REMOVE, removeCommandName);
      cmdStringMap = Collections.unmodifiableMap(cmdStringMap);
    }

    Type(Option[] opts, OsCommand[] commands) {
      possibleOptions = new HashSet<>(Arrays.asList(opts));
      possibleCommands = new HashSet<>(Arrays.asList(commands));
    }

    public static String mapCommandToString(DockerCommand.Type cType) throws IllegalArgumentException {
      String res = cmdStringMap.get(cType);

      if (res == null) {
        // todo insert String representation of DockerCommand in exception String
         throw new IllegalArgumentException("No mapping for this Docker Command available");
      }

      return res;
    }

    public Set<Option> getPossibleOptions() {
      return possibleOptions;
    }

    public Set<OsCommand> getPossibleOsCommands() {
      return possibleCommands;
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
  private Map<Option, List<String>> usedOptions;
  private List<OsCommand> osCommand;
  private List<String> usedArgs;

  private DockerCommand(Builder builder) {
    cmdType = builder.cmdType;
    usedOptions = builder.usedOptions;
    osCommand = builder.osCommand;
    usedArgs = builder.usedArgs;
  }

  public Map<Option, List<String>> getUsedOptions() {
    return new HashMap<>(usedOptions);
  }

  public List<OsCommand> getOsCommand() {
    return new ArrayList<>(osCommand);
  }

  public List<String> getUsedArgs() {
    return new ArrayList<>(usedArgs);
  }

  public void setUsedOptions(Map<DockerCommand.Option, List<String>> opts) throws DockerCommandException {
    checkMapKeysInSet(opts, cmdType.possibleOptions, cmdType);
    this.usedOptions = opts;
  }

  public void setOsCommand(OsCommand cmd) throws DockerCommandException {
    if (!cmdType.possibleCommands.contains(cmd)) {
      throw new DockerCommandException(
      "Command "
        + cmd.name()
        + " does not exist for '"
        + DockerCommand.Type.mapCommandToString(cmdType)
        + "' command");
    }

    // only one OsCommand allowed
    if (osCommand.size() > 0) {
      osCommand.remove(osCommand.size() - 1);
    }

    osCommand.add(cmd);
  }

  public void setUsedArgs(List<String> usedArgs) throws DockerCommandException {
    if (!argsAllowed(cmdType)) {
      throw new DockerCommandException(
          "Args not allowed for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");
    }

    if (osCommand.size() != 1) {
      throw new DockerCommandException("Args cannot be set when there are " + osCommand.size() +  " Os Commands set");
    }

    this.usedArgs = usedArgs;
  }


  public static boolean argsAllowed(DockerCommand.Type cType) {
    if (cType == Type.CREATE || cType == Type.RUN) {
      return true;
    }

    return false;
  }

  private static void checkMapKeysInSet(Map<Option,List<String>> map, Set<Option> set, DockerCommand.Type cType)
      throws DockerCommandException {
    for (Map.Entry<Option, List<String>> kv : map.entrySet()) {
        if (!set.contains(kv.getKey())) {
          throw new DockerCommandException(
              "Option "
                  + kv.getKey().name()
                  + " does not exist for '"
                  + DockerCommand.Type.mapCommandToString(cType)
                  + "' command");
        }
    }
  }

  public static class Builder {
    private final Type cmdType;
    private final Map<Option, List<String>> usedOptions;
    private final List<OsCommand> osCommand;
    private final List<String> usedArgs;

    public Builder(DockerCommand.Type cmdType) {
      this.cmdType = cmdType;
      this.usedOptions = new HashMap<>();
      this.osCommand = new ArrayList<>();
      this.usedArgs = new ArrayList<>();
    }

    public Builder usedOptions(Map<Option,List<String>> opts) throws DockerCommandException  {
      checkMapKeysInSet(opts, cmdType.possibleOptions, cmdType);

      usedOptions.putAll(opts);
      return this;
    }

    public Builder osCommand(List<OsCommand> cmd) throws DockerCommandException {
      isSubset(cmdType.possibleCommands,cmd);
      //only zero/one OsCommand allowed
      if(cmd.size()>1)
        throw new DockerCommandException("Only one Command allowed for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");

      osCommand.addAll(cmd);
      return this;
    }

    public Builder usedArgs(List<String> args) throws DockerCommandException {
      //todo: check if args allowed within this DockerCommand
      usedArgs.addAll(args);
      return this;
    }

    public DockerCommand build() {
      return new DockerCommand(this);
    }

    void isSubset(Set<OsCommand> origSet, List<OsCommand> possibleSubSet) throws DockerCommandException {
      for (OsCommand osCmd: possibleSubSet) {
        if(!origSet.contains(osCmd))
          throw new DockerCommandException("Command " + osCmd.name() + " does not exist for '" + DockerCommand.Type.mapCommandToString(cmdType) + "' command");
      }
    }
  }
}
