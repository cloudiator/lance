package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class EntireDockerCommands implements Serializable {

  private static final long serialVersionUID = -5675613485138618094L;
  private static final String wrongCmdMessage = "DockerCommand must be either CREATE, START, STOP";
  private DockerCommand create, start, stop, run, remove;
  private final Map<DockerCommand.Type, DockerCommand> typeInstanceMap;

  public EntireDockerCommands() {
    create = new DockerCommand.Builder(Type.CREATE).build();
    start = new DockerCommand.Builder(Type.START).build();
    stop = new DockerCommand.Builder(Type.STOP).build();
    run =  new DockerCommand.Builder(Type.RUN).build();
    remove =  new DockerCommand.Builder(Type.REMOVE).build();

    typeInstanceMap = new HashMap<>();
    initTypeInstanceMap();
  }

  private EntireDockerCommands(Builder builder) {
    create = builder.createCmd;
    start =  builder.startCmd;
    stop =  builder.stopCmd;
    run =  builder.runCmd;
    remove =  builder.removeCmd;

    typeInstanceMap = new HashMap<>();
    initTypeInstanceMap();
  }

  private final void initTypeInstanceMap() {
    typeInstanceMap.put(Type.CREATE, create);
    typeInstanceMap.put(Type.START, start);
    typeInstanceMap.put(Type.STOP, stop);
    typeInstanceMap.put(Type.RUN, run);
    typeInstanceMap.put(Type.REMOVE, remove);
  }

  public DockerCommand getCreate() {
    return create;
  }

  public DockerCommand getStart() {
    return start;
  }

  public DockerCommand getStop() {
    return stop;
  }

  public DockerCommand getRun() {
    return run;
  }

  public DockerCommand getRemove() {
    return remove;
  }

  public void setCreate(DockerCommand create) {
    this.create = create;
    typeInstanceMap.put(Type.CREATE, create);
  }

  public void setStart(DockerCommand start) {
    this.start = start;
    typeInstanceMap.put(Type.START, start);
  }

  public void setStop(DockerCommand stop) {
    this.stop = stop;
    typeInstanceMap.put(Type.STOP, stop);
  }

  public void setRun(DockerCommand run) {
    this.run = run;
    typeInstanceMap.put(Type.RUN, run);
  }

  public void setRemove(DockerCommand remove) {
    this.remove = remove;
    typeInstanceMap.put(Type.REMOVE, remove);
  }

  public void appendOption(DockerCommand.Type cType, Option opt, String arg) throws DockerCommandException {
    DockerCommand cmd = getCommandFromMap(cType);
    cmd = DockerCommandUtils.appendOption(cmd, opt, arg);
    setDockerCommand(cType, cmd);
}

  public void setOsCommand(DockerCommand.Type cType, OsCommand osCmd) throws DockerCommandException {
    DockerCommand cmd = getCommandFromMap(cType);
    cmd.setOsCommand(osCmd);
    setDockerCommand(cType, cmd);
  }

  public void appendArg(DockerCommand.Type cType, String arg) throws DockerCommandException {
    DockerCommand cmd = getCommandFromMap(cType);
    cmd = DockerCommandUtils.appendArg(cmd, arg);
    setDockerCommand(cType, cmd);
  }

  public String getSetOptionsString(DockerCommand.Type cType) throws DockerCommandException {
    DockerCommand cmd = getCommandFromMap(cType);
    DockerCommandUtils.getSetOptionsString(cmd);
  }

  public String getSetOsCommandString(DockerCommand.Type cType)  throws DockerCommandException {
    DockerCommand cmd = getCommandFromMap(cType);
    return DockerCommandUtils.getSetOsCommandString(cmd);
  }

  public String getSetArgsString(DockerCommand.Type cType)  throws DockerCommandException {
    DockerCommand cmd = getCommandFromMap(cType);
    return DockerCommandUtils.getSetArgsString(cmd);
  }

  //"helper-method" to get the Name for commands: START, RUN, STOP
  public String getContainerName(DockerCommand.Type cType) throws DockerCommandException {
    DockerCommand cmd = getCommandFromMap(cType);
    return DockerCommandUtils.getContainerName(cmd);
  }

  //only copying options that are allowed for both commands
  //Set options will get overwritten
  public static void copyCmdOptions(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if (!(from.cmdType == to.cmdType)
        && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN))) {
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);
    }

    Map<DockerCommand.Option, List<String>> usedOptsFrom = new HashMap<>(from.getUsedOptions());
    Map<DockerCommand.Option, List<String>> allowedOptsFrom = new HashMap<>();

    for (Entry<DockerCommand.Option, List<String>> optEntry : usedOptsFrom.entrySet()) {
      DockerCommand.Option opt = optEntry.getKey();
      List<String> lst = optEntry.getValue();


      if (!to.cmdType.isAllowedOpt(opt)) {
        continue;
      }

      allowedOptsFrom.put(opt, lst);
    }

    Map<DockerCommand.Option,List<String>> mergedMap = new HashMap<>(to.getUsedOptions());
    //overwrite set values in to-Options
    mergedMap.putAll(allowedOptsFrom);
    to.setUsedOptions(mergedMap);
  }

  //only copying OsCommand that is allowed for both commands
  //only the last command in the list will be used to overwrite, if List is empty, nothing changes
  public static void copyCmdOsCommand(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if (!(from.cmdType == to.cmdType)
        && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN))) {
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);
    }

    List<OsCommand> setOsCommandFrom = new ArrayList<>(from.getOsCommand());

    for(OsCommand osCmd: setOsCommandFrom) {
      if (!to.cmdType.isAllowedOsCommand(osCmd)) {
        continue;
      }

      to.setOsCommand(osCmd);
    }
  }

  //todo: merge this method with copyCmdOsCommand method, because args can only be set in conjunction with OsCommand
  //only copying if OsCommand is set, appending args
  public static void copyCmdArgs(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if (!(from.cmdType == to.cmdType)
        && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN))) {
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);
    }

    if(to.getOsCommand().size() != 1) {
        return;
      }

    List<String> usedArgsFrom = new ArrayList<>(from.getUsedArgs());
    List<String> joinedArgs = new ArrayList<>(to.getUsedArgs());
    joinedArgs.addAll(usedArgsFrom);
    to.setUsedArgs(joinedArgs);
  }

  //todo: check for allowed mappings
  private void setDockerCommand(DockerCommand.Type cType, DockerCommand cmd) throws DockerCommandException{
    if (cType == DockerCommand.Type.CREATE) {
        create = cmd;
        typeInstanceMap.put(Type.CREATE, create);
      } else if (cType == DockerCommand.Type.START) {
        start = cmd;
        typeInstanceMap.put(Type.START, start);
      } else if (cType == DockerCommand.Type.STOP) {
        stop = cmd;
        typeInstanceMap.put(Type.STOP, stop);
      } else if (cType == DockerCommand.Type.RUN) {
        run = cmd;
        typeInstanceMap.put(Type.RUN, run);
      } else if (cType == DockerCommand.Type.REMOVE) {
        remove = cmd;
        typeInstanceMap.put(Type.REMOVE, remove);
      } else {
        throw new DockerCommandException(wrongCmdMessage);
      }
    }

    private DockerCommand getCommandFromMap(DockerCommand.Type cType) throws DockerCommandException {
      DockerCommand cmd = typeInstanceMap.get(cType);

      if(cmd == null) {
        throw new DockerCommandException(wrongCmdMessage);
      }

      return cmd;
    }

    public static class Builder {
      private final DockerCommand.Builder createCmdBuilder, startCmdBuilder, stopCmdBuilder,
          runCmdBuilder, removeCmdBuilder;

      private DockerCommand createCmd;
      private DockerCommand startCmd;
      private DockerCommand stopCmd;
      private DockerCommand runCmd;
      private DockerCommand removeCmd;

      public Builder() {

        this.createCmdBuilder = new DockerCommand.Builder(Type.CREATE);
        this.startCmdBuilder = new DockerCommand.Builder(Type.START);
        this.stopCmdBuilder = new DockerCommand.Builder(Type.STOP);
        this.runCmdBuilder = new DockerCommand.Builder(Type.RUN);
        this.removeCmdBuilder = new DockerCommand.Builder(Type.REMOVE);
      }

      public Builder usedOptions(DockerCommand.Type type, Map<Option,List<String>> opts) throws DockerCommandException  {
        if(type == Type.CREATE) {
        createCmdBuilder.usedOptions(opts);
      } else if(type == Type.START) {
        startCmdBuilder.usedOptions(opts);
      } else if(type == Type.STOP) {
        stopCmdBuilder.usedOptions(opts);
      } else {
        throw new DockerCommandException(wrongCmdMessage);
      }

      return this;
    }

    public Builder osCommand(DockerCommand.Type type, List<OsCommand> cmd) throws DockerCommandException {
      if(type == Type.CREATE) {
        createCmdBuilder.osCommand(cmd);
      } else if(type == Type.START) {
        startCmdBuilder.osCommand(cmd);
      } else if(type == Type.STOP) {
        stopCmdBuilder.osCommand(cmd);
      } else {
        throw new DockerCommandException(wrongCmdMessage);
      }

      return this;
    }

    public Builder usedArgs(DockerCommand.Type type, List<String> args) throws DockerCommandException {
      if(type == Type.CREATE) {
        createCmdBuilder.usedArgs(args);
      } else if(type == Type.START) {
        startCmdBuilder.usedArgs(args);
      } else if(type == Type.STOP) {
        stopCmdBuilder.usedArgs(args);
      } else {
        throw new DockerCommandException(wrongCmdMessage);
      }

      return this;
    }

    public EntireDockerCommands build() {
      createCmd = createCmdBuilder.build();
      startCmd = startCmdBuilder.build();
      stopCmd = stopCmdBuilder.build();
      runCmd = runCmdBuilder.build();
      removeCmd = removeCmdBuilder.build();
      return new EntireDockerCommands(this);
    }
  }
}
