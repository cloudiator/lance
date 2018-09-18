package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import java.io.Serializable;

public class EntireDockerCommands implements Serializable {

  private static final long serialVersionUID = -5675613485138618094L;
  private final DockerCommand create;
  private final DockerCommand start;
  private final DockerCommand stop;

  public EntireDockerCommands() {
    create = null;
    start = null;
    stop = null;
  }

  public EntireDockerCommands(DockerCommand create, DockerCommand start, DockerCommand stop) {
    this.create = create;
    this.start = start;
    this.stop = stop;
  }

  public void setOption(DockerCommand cmd, Option opt, String arg) throws Exception {
    if(cmd==DockerCommand.CREATE)
      create.setOption(opt,arg);
    else if(cmd==DockerCommand.START)
      start.setOption(opt,arg);
    else if(cmd==DockerCommand.STOP)
      stop.setOption(opt,arg);
    else
      throw new Exception("DockerCommand must be either CREATE, START or STOP");
  }

  public void setOsCommand(DockerCommand cmd, OsCommand osCmd) throws Exception {
    if(cmd==DockerCommand.CREATE)
      create.setOsCommand(osCmd);
    else if(cmd==DockerCommand.START)
      start.setOsCommand(osCmd);
    else if(cmd==DockerCommand.STOP)
      stop.setOsCommand(osCmd);
    else
      throw new Exception("DockerCommand must be either CREATE, START or STOP");
  }

  public void setArg(DockerCommand cmd, String arg) throws Exception {
    if(cmd==DockerCommand.CREATE)
      create.setArg(arg);
    else if(cmd==DockerCommand.START)
      start.setArg(arg);
    else if(cmd==DockerCommand.STOP)
      stop.setArg(arg);
    else
      throw new Exception("DockerCommand must be either CREATE, START or STOP");
  }

  public String getSetOptionsString(DockerCommand cmd)  throws Exception {
    if(cmd==DockerCommand.CREATE)
      return create.getSetOptionsString();
    else if(cmd==DockerCommand.START)
      return start.getSetOptionsString();
    else if(cmd==DockerCommand.STOP)
      return stop.getSetOptionsString();
    else
      throw new Exception("DockerCommand must be either CREATE, START or STOP");
  }

  public String getSetOsCommandString(DockerCommand cmd)  throws Exception {
    if(cmd==DockerCommand.CREATE)
      return create.getSetOsCommandString();
    if(cmd==DockerCommand.START)
      return start.getSetOsCommandString();
    if(cmd==DockerCommand.STOP)
      return stop.getSetOsCommandString();
    else
      throw new Exception("DockerCommand must be either CREATE, START or STOP");
  }

  public String getSetArgsString(DockerCommand cmd)  throws Exception {
    if(cmd==DockerCommand.CREATE)
      return create.getSetArgsString();
    if(cmd==DockerCommand.START)
      return start.getSetArgsString();
    if(cmd==DockerCommand.STOP)
      return stop.getSetArgsString();
    else
      throw new Exception("DockerCommand must be either CREATE, START or STOP");
  }

  //"helper-method" to get the Name for commands: START, STOP
  public String getContainerName(DockerCommand cmd) throws Exception {
    try {
      if(cmd==DockerCommand.CREATE)
        return create.getContainerName();
      if(cmd==DockerCommand.START)
        return start.getContainerName();
      if(cmd==DockerCommand.STOP)
        return stop.getContainerName();
      else
        throw new Exception("DockerCommand must be either CREATE, START or STOP");
    }
    catch (IllegalStateException ise) {
      throw new Exception(ise.getMessage());
    }
  }
}
