package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteDockerComponent  extends DockerComponent {
  private final DockerRegistry dReg;

  //todo: Better construct a Builder that fits into the Class Hierarchy
  public RemoteDockerComponent(DockerComponent.Builder builder, DockerRegistry dReg) {
    super(builder);
    this.dReg = dReg;
  }

  public DockerRegistry getDockerReg () {
    return this.dReg;
  }

  private String buildRemoteRegString() throws IllegalArgumentException {
    if(dReg.hostName == null || dReg.hostName.equals("") ||
        dReg.port<0 || dReg.port>65535)
      throw new IllegalArgumentException("Registry information is corrupted.");

    String fullString = dReg.hostName + ":" + new Integer(dReg.port).toString() + "/";
    return fullString;
  }

  public static class DockerRegistry implements Serializable {
    public String hostName;
    public int port;
    public String userName;
    public String password;
  }
}
