package de.uniulm.omi.cloudiator.lance.lifecycle.language;

import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;

public class EntireDockerCommands {
  private final DockerCommand create = DockerCommand.CREATE;
  private final DockerCommand start = DockerCommand.START;
  private final DockerCommand stop = DockerCommand.STOP;


  public void setOption(DockerCommand cmd, Option opt, String arg) throws Exception {
    switch(cmd) {
      case CREATE:
        create.setOption(opt,arg);
        break;
      case START:
        start.setOption(opt,arg);
        break;
      case STOP:
        stop.setOption(opt,arg);
        break;
    }
  }

  public void setOsCommand(DockerCommand cmd, OsCommand osCmd) throws Exception {
    switch (cmd) {
      case CREATE:
        create.setOsCommand(osCmd);
        break;
      case START:
        start.setOsCommand(osCmd);
        break;
      case STOP:
        stop.setOsCommand(osCmd);
        break;
    }
  }

  public void setArg(DockerCommand cmd, String arg) {
    switch (cmd) {
      case CREATE:
        create.setArg(arg);
        break;
      case START:
        start.setArg(arg);
        break;
      case STOP:
        stop.setArg(arg);
        break;
    }
  }

  public String getSetOptionsString(DockerCommand cmd) {
    switch (cmd) {
      case CREATE:
        return create.getSetOptionsString();
      case START:
        return start.getSetOptionsString();
      case STOP:
        return stop.getSetOptionsString();
      default:
        return "";
    }
  }

  public String getSetOsCommandString(DockerCommand cmd) {
    switch (cmd) {
      case CREATE:
        return create.getSetOsCommandString();
      case START:
        return start.getSetOsCommandString();
      case STOP:
        return stop.getSetOsCommandString();
      default:
        return "";
    }
  }

  public String getSetArgsString(DockerCommand cmd) {
    switch (cmd) {
      case CREATE:
        return create.getSetArgsString();
      case START:
        return start.getSetArgsString();
      case STOP:
        return stop.getSetArgsString();
      default:
        return "";
    }
  }
}
