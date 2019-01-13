package de.uniulm.omi.cloudiator.lance.application.component;

import java.io.Serializable;

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

  public String getUriEndPoint() throws IllegalArgumentException {
    if(dReg.hostName == null || dReg.hostName.equals(""))
      throw new IllegalArgumentException("Registry information is corrupted.");

    boolean usePort = !(dReg.port<0) && dReg.port<65536;
    String fullString;
    if(usePort) {
      fullString = dReg.hostName + ":" + new Integer(dReg.port).toString();
    } else {
      fullString = dReg.hostName;
    }
    return fullString;
  }

  @Override
  public String getFullImageName() throws IllegalArgumentException {
    String fullImageName = getUriEndPoint() + "/" + super.getFullImageName();
    return fullImageName;
  }

  public String getFullImageNameRegStripped() throws IllegalArgumentException {
    String strippedImageName = super.getFullImageName();
    return strippedImageName;
  }

  public static class DockerRegistry implements Serializable {
    public final String hostName;
    public final int port;
    public final String userName;
    public final String password;
    public final boolean useCredentialsParam;

    public DockerRegistry(String hostNameParam, int portParam, String userNameParam, String passwordParam, boolean useCredentialsParam) {
      this.hostName = hostNameParam;
      this.port = portParam;
      this.userName = userNameParam;
      this.password = passwordParam;
      this.useCredentialsParam = useCredentialsParam;
    }

    public DockerRegistry(String hostNameParam) {
      this(hostNameParam, -1, "", "", false);
    }

    public DockerRegistry(String hostNameParam, int portParam) {
      this(hostNameParam, portParam, "", "", false);
    }

    public DockerRegistry(String hostNameParam, String userNameParam, String passwordParam) {
      this(hostNameParam, -1, userNameParam, passwordParam, true);
    }
  }
}
