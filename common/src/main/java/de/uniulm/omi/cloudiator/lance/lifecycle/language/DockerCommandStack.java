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

public class DockerCommandStack implements Serializable {

  private static final long serialVersionUID = -5675613485138618094L;
  private static final String wrongCmdMessage = "DockerCommand must be either CREATE, START, STOP";
  private DockerCommand create, start, stop, run, remove;

  public DockerCommandStack() {
    create = new DockerCommand.Builder(Type.CREATE).build();
    start = new DockerCommand.Builder(Type.START).build();
    stop = new DockerCommand.Builder(Type.STOP).build();
    run =  new DockerCommand.Builder(Type.RUN).build();
    remove =  new DockerCommand.Builder(Type.REMOVE).build();
  }

  private DockerCommandStack(Builder builder) {
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

  public DockerCommand getRemove() {
    return remove;
  }

  public void appendOption(DockerCommand.Type cType, Option opt, String arg) throws DockerCommandException {
    if (cType == DockerCommand.Type.CREATE) {
      create = DockerCommandUtils.appendOption(create, opt, arg);
    } else if (cType == DockerCommand.Type.START) {
      start = DockerCommandUtils.appendOption(start, opt, arg);
    } else if (cType == DockerCommand.Type.STOP) {
      stop = DockerCommandUtils.appendOption(stop, opt, arg);
    } else if (cType == DockerCommand.Type.RUN) {
      run = DockerCommandUtils.appendOption(run, opt, arg);
    } else if (cType == DockerCommand.Type.REMOVE) {
      remove = DockerCommandUtils.appendOption(remove, opt, arg);
    } else {
      throw new DockerCommandException(wrongCmdMessage);
    }
  }

  public void setOsCommand(DockerCommand.Type cType, OsCommand osCmd) throws DockerCommandException {
    if (cType == DockerCommand.Type.CREATE) {
      create.setOsCommand(osCmd);
    } else if (cType == DockerCommand.Type.START) {
      start.setOsCommand(osCmd);
    } else if (cType == DockerCommand.Type.STOP) {
      stop.setOsCommand(osCmd);
    } else if (cType == DockerCommand.Type.RUN) {
      run.setOsCommand(osCmd);
    } else if (cType == DockerCommand.Type.REMOVE) {
      remove.setOsCommand(osCmd);
    } else {
      throw new DockerCommandException(wrongCmdMessage);
    }
  }

  public void appendArg(DockerCommand.Type cType, String arg) throws DockerCommandException {
    if (cType == DockerCommand.Type.CREATE) {
      create = DockerCommandUtils.appendArg(create, arg);
    } else if (cType == DockerCommand.Type.START) {
      start = DockerCommandUtils.appendArg(start, arg);
    } else if (cType == DockerCommand.Type.STOP) {
      stop = DockerCommandUtils.appendArg(stop, arg);
    } else if (cType == DockerCommand.Type.RUN) {
      run = DockerCommandUtils.appendArg(run, arg);
    } else if (cType == DockerCommand.Type.REMOVE) {
      remove = DockerCommandUtils.appendArg(remove, arg);
    } else {
      throw new DockerCommandException(wrongCmdMessage);
    }
  }

  public String getSetOptionsString(DockerCommand.Type cType) throws DockerCommandException {
    if (cType == DockerCommand.Type.CREATE) {
      return DockerCommandUtils.getSetOptionsString(create);
    } else if (cType == DockerCommand.Type.START) {
      return DockerCommandUtils.getSetOptionsString(start);
    } else if (cType == DockerCommand.Type.STOP) {
      return DockerCommandUtils.getSetOptionsString(stop);
    } else if (cType == DockerCommand.Type.RUN) {
      return DockerCommandUtils.getSetOptionsString(run);
    } else if (cType == DockerCommand.Type.REMOVE) {
      return DockerCommandUtils.getSetOptionsString(remove);
    } else {
      throw new DockerCommandException(wrongCmdMessage);
    }
  }

  public String getSetOsCommandString(DockerCommand.Type cType)  throws DockerCommandException {
    if (cType == DockerCommand.Type.CREATE) {
      return DockerCommandUtils.getSetOsCommandString(create);
    } else if (cType == DockerCommand.Type.START) {
      return DockerCommandUtils.getSetOsCommandString(start);
    } else if (cType == DockerCommand.Type.STOP) {
      return DockerCommandUtils.getSetOsCommandString(stop);
    } else if (cType == DockerCommand.Type.RUN) {
      return DockerCommandUtils.getSetOsCommandString(run);
    } else if (cType == DockerCommand.Type.REMOVE) {
      return DockerCommandUtils.getSetOsCommandString(remove);
    } else {
      throw new DockerCommandException(wrongCmdMessage);
    }
  }

  public String getSetArgsString(DockerCommand.Type cType)  throws DockerCommandException {
    if (cType == DockerCommand.Type.CREATE) {
      return DockerCommandUtils.getSetArgsString(create);
    } else if (cType == DockerCommand.Type.START) {
      return DockerCommandUtils.getSetArgsString(start);
    } else if (cType == DockerCommand.Type.STOP) {
      return DockerCommandUtils.getSetArgsString(stop);
    } else if (cType == DockerCommand.Type.RUN) {
      return DockerCommandUtils.getSetArgsString(run);
    } else if (cType == DockerCommand.Type.REMOVE) {
      return DockerCommandUtils.getSetArgsString(remove);
    } else {
      throw new DockerCommandException(wrongCmdMessage);
    }
  }

  //"helper-method" to get the Name for commands: START, STOP
  public String getContainerName(DockerCommand.Type cType) throws DockerCommandException {
    if (cType == DockerCommand.Type.CREATE) {
      return DockerCommandUtils.getContainerName(create);
    } else if (cType == DockerCommand.Type.START) {
      return DockerCommandUtils.getContainerName(start);
    } else if (cType == DockerCommand.Type.STOP) {
      return DockerCommandUtils.getContainerName(stop);
    } else if (cType == DockerCommand.Type.RUN) {
      return DockerCommandUtils.getContainerName(run);
    } else if (cType == DockerCommand.Type.REMOVE) {
      return DockerCommandUtils.getContainerName(remove);
    } else {
      throw new DockerCommandException(wrongCmdMessage);
    }
  }

  public static void copyCmdOptions(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if(!(from.cmdType == to.cmdType) && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN)))
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);

    Map<DockerCommand.Option, List<String>> usedOptsFrom = new HashMap<>(from.getUsedOptions());
    to.setUsedOptions(usedOptsFrom);
  }

  public static void copyCmdOsCommand(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if(!(from.cmdType == to.cmdType) && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN)))
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);

    List<OsCommand> setOsCommandFrom = new ArrayList<>(from.getOsCommand());

    for(OsCommand osCmd: setOsCommandFrom) {
      to.setOsCommand(osCmd);
    }
  }

  public static void copyCmdArgs(DockerCommand from, DockerCommand to) throws DockerCommandException {
    if(!(from.cmdType == to.cmdType) && !((from.cmdType == Type.CREATE) && (to.cmdType == Type.RUN)))
      throw new DockerCommandException("Cannot convert from " + from.cmdType + " to " + to.cmdType);

    List<String> usedArgsFrom = new ArrayList<>(from.getUsedArgs());
    to.setUsedArgs(usedArgsFrom);
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
        createCmdBuilder.usedOptions(opts);
      }
      else if(type == Type.START) {
        startCmdBuilder.usedOptions(opts);
      }
      else if(type == Type.STOP) {
        stopCmdBuilder.usedOptions(opts);
      }
      else
        throw new DockerCommandException(wrongCmdMessage);

      return this;
    }

    public Builder setCommand(DockerCommand.Type type, List<OsCommand> cmd) throws DockerCommandException {
      if(type == Type.CREATE) {
        createCmdBuilder.osCommand(cmd);
      }
      else if(type == Type.START) {
        startCmdBuilder.osCommand(cmd);
      }
      else if(type == Type.STOP) {
        stopCmdBuilder.osCommand(cmd);
      }
      else
        throw new DockerCommandException(wrongCmdMessage);

      return this;
    }

    public Builder setArgs(DockerCommand.Type type, List<String> args) throws DockerCommandException {
      if(type == Type.CREATE) {
        createCmdBuilder.usedArgs(args);
      }
      else if(type == Type.START) {
        startCmdBuilder.usedArgs(args);
      }
      else if(type == Type.STOP) {
        stopCmdBuilder.usedArgs(args);
      }
      else
        throw new DockerCommandException(wrongCmdMessage);

      return this;
    }

    public DockerCommandStack build() {
      createCmd = createCmdBuilder.build();
      startCmd = startCmdBuilder.build();
      stopCmd = stopCmdBuilder.build();
      runCmd = runCmdBuilder.build();
      removeCmd = removeCmdBuilder.build();
      return new DockerCommandStack(this);
    }
  }
}
