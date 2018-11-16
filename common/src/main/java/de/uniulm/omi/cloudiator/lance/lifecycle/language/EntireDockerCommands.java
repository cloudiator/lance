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
  private final DockerCommand create;
  private final DockerCommand start;
  private final DockerCommand stop;
  private final DockerCommand run;
  private final DockerCommand remove;

  public EntireDockerCommands() {
    create = new DockerCommand.Builder(Type.CREATE).build();
    start = new DockerCommand.Builder(Type.START).build();
    stop = new DockerCommand.Builder(Type.STOP).build();
    run =  new DockerCommand.Builder(Type.RUN).build();
    remove =  new DockerCommand.Builder(Type.REMOVE).build();
  }

  private EntireDockerCommands(Builder builder) {
    create = builder.createCmd;
    start =  builder.startCmd;
    stop =  builder.stopCmd;
    run =  builder.runCmd;
    remove =  builder.removeCmd;
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

  public DockerCommand getKill() {
    return remove;
  }

  public void setOption(DockerCommand.Type cType, Option opt, String arg) throws DockerCommandException {
    if(cType==DockerCommand.Type.CREATE)
      create.setOption(opt,arg);
    else if(cType==DockerCommand.Type.START)
      start.setOption(opt,arg);
    else if(cType==DockerCommand.Type.STOP)
      stop.setOption(opt,arg);
    else if(cType==DockerCommand.Type.RUN)
      run.setOption(opt,arg);
    else if(cType==DockerCommand.Type.REMOVE)
      remove.setOption(opt,arg);
    else
      throw new DockerCommandException(wrongCmdMessage);
  }

  public void setOsCommand(DockerCommand.Type cType, OsCommand osCmd) throws DockerCommandException {
    if(cType==DockerCommand.Type.CREATE)
      create.setOsCommand(osCmd);
    else if(cType==DockerCommand.Type.START)
      start.setOsCommand(osCmd);
    else if(cType==DockerCommand.Type.STOP)
      stop.setOsCommand(osCmd);
    else if(cType==DockerCommand.Type.RUN)
      run.setOsCommand(osCmd);
    else if(cType==DockerCommand.Type.REMOVE)
      remove.setOsCommand(osCmd);
    else
      throw new DockerCommandException(wrongCmdMessage);
  }

  public void setArg(DockerCommand.Type cType, String arg) throws DockerCommandException {
    if(cType==DockerCommand.Type.CREATE)
      create.setArg(arg);
    else if(cType==DockerCommand.Type.START)
      start.setArg(arg);
    else if(cType==DockerCommand.Type.STOP)
      stop.setArg(arg);
    if(cType==DockerCommand.Type.RUN)
      run.setArg(arg);
    if(cType==DockerCommand.Type.REMOVE)
      remove.setArg(arg);
    else
      throw new DockerCommandException(wrongCmdMessage);
  }

  public String getSetOptionsString(DockerCommand.Type cType) throws DockerCommandException {
    if(cType==DockerCommand.Type.CREATE)
      return create.getSetOptionsString();
    else if(cType==DockerCommand.Type.START)
      return start.getSetOptionsString();
    else if(cType==DockerCommand.Type.STOP)
      return stop.getSetOptionsString();
    else if(cType==DockerCommand.Type.RUN)
      return run.getSetOptionsString();
    else if(cType==DockerCommand.Type.REMOVE)
      return remove.getSetOptionsString();
    else
      throw new DockerCommandException(wrongCmdMessage);
  }

  public String getSetOsCommandString(DockerCommand.Type cType)  throws DockerCommandException {
    if(cType==DockerCommand.Type.CREATE)
      return create.getSetOsCommandString();
    if(cType==DockerCommand.Type.START)
      return start.getSetOsCommandString();
    if(cType==DockerCommand.Type.STOP)
      return stop.getSetOsCommandString();
    if(cType==DockerCommand.Type.RUN)
      return run.getSetOsCommandString();
    if(cType==DockerCommand.Type.REMOVE)
      return remove.getSetOsCommandString();
    else
      throw new DockerCommandException(wrongCmdMessage);
  }

  public String getSetArgsString(DockerCommand.Type cType)  throws DockerCommandException {
    if(cType==DockerCommand.Type.CREATE)
      return create.getSetArgsString();
    if(cType==DockerCommand.Type.START)
      return start.getSetArgsString();
    if(cType==DockerCommand.Type.STOP)
      return stop.getSetArgsString();
    if(cType==DockerCommand.Type.RUN)
      return run.getSetArgsString();
    if(cType==DockerCommand.Type.REMOVE)
      return remove.getSetArgsString();
    else
      throw new DockerCommandException(wrongCmdMessage);
  }

  //"helper-method" to get the Name for commands: START, STOP
  public String getContainerName(DockerCommand.Type cType) throws DockerCommandException {
    if(cType==DockerCommand.Type.CREATE)
      return create.getContainerName();
    if(cType==DockerCommand.Type.START)
      return start.getContainerName();
    if(cType==DockerCommand.Type.STOP)
      return stop.getContainerName();
    if(cType==DockerCommand.Type.RUN)
      return run.getContainerName();
    if(cType==DockerCommand.Type.REMOVE)
      return remove.getContainerName();
    else
      throw new DockerCommandException(wrongCmdMessage);
  }

  public static void copyCmdOptions(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if(!(from.cmdType == to.cmdType) && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN)))
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);

    Map<DockerCommand.Option, List<String>> setOptsFrom = new HashMap<>(from.getSetOptions());

    for(Entry<DockerCommand.Option, List<String>> opt : setOptsFrom.entrySet()) {
      for(String arg: opt.getValue()) {
        to.setOption(opt.getKey(), arg);
      }
    }
  }

  //private final List<OsCommand> setCommand;
  //private final List<String> setArgs;
  public static void copyCmdOsCommand(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if(!(from.cmdType == to.cmdType) && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN)))
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);

    List<OsCommand> setOsCommandFrom = new ArrayList<>(from.getSetCommand());

    for(OsCommand osCmd: setOsCommandFrom) {
      to.setOsCommand(osCmd);
    }
  }

  public static void copyCmdArgs(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if(!(from.cmdType == to.cmdType) && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN)))
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);

    List<String> setArgsFrom = new ArrayList<>(from.getSetArgs());

    for(String arg: setArgsFrom) {
      to.setArg(arg);
    }
  }

  public static class Builder {
    private final DockerCommand.Builder createCmdBuilder;
    private final DockerCommand.Builder startCmdBuilder;
    private final DockerCommand.Builder stopCmdBuilder;
    private final DockerCommand.Builder runCmdBuilder;
    private final DockerCommand.Builder removeCmdBuilder;

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

    public Builder setOptions(DockerCommand.Type type, Map<Option,List<String>> opts) throws DockerCommandException  {
      if(type == Type.CREATE) {
        createCmdBuilder.setOptions(opts);
      }
      else if(type == Type.START) {
        startCmdBuilder.setOptions(opts);
      }
      else if(type == Type.STOP) {
        stopCmdBuilder.setOptions(opts);
      }
      else
        throw new DockerCommandException(wrongCmdMessage);

      return this;
    }

    public Builder setCommand(DockerCommand.Type type, List<OsCommand> cmd) throws DockerCommandException {
      if(type == Type.CREATE) {
        createCmdBuilder.setCommand(cmd);
      }
      else if(type == Type.START) {
        startCmdBuilder.setCommand(cmd);
      }
      else if(type == Type.STOP) {
        stopCmdBuilder.setCommand(cmd);
      }
      else
        throw new DockerCommandException(wrongCmdMessage);

      return this;
    }

    public Builder setArgs(DockerCommand.Type type, List<String> args) throws DockerCommandException {
      if(type == Type.CREATE) {
        createCmdBuilder.setArgs(args);
      }
      else if(type == Type.START) {
        startCmdBuilder.setArgs(args);
      }
      else if(type == Type.STOP) {
        stopCmdBuilder.setArgs(args);
      }
      else
        throw new DockerCommandException(wrongCmdMessage);

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
