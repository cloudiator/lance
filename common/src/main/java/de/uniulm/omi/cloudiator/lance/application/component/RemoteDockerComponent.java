package de.uniulm.omi.cloudiator.lance.application.component;

import java.io.Serializable;

public class RemoteDockerComponent extends DockerComponent {

  private final DockerRegistry dockerReg;

  // todo: Better construct a Builder that fits into the Class Hierarchy
  public RemoteDockerComponent(DockerComponent.Builder builder, DockerRegistry dockerReg) {
    super(builder);
    this.dockerReg = dockerReg;
  }

  public DockerRegistry getDockerReg() {
    return this.dockerReg;
  }

  public String getUriEndPoint() throws IllegalArgumentException {
    if (dockerReg.hostName == null || dockerReg.hostName.equals("")) {
      throw new IllegalArgumentException("Registry information is corrupted.");
    }

    boolean usePort = !(dockerReg.port < 0) && dockerReg.port < 65536;
    String fullString;
    if (usePort) {
      fullString = dockerReg.hostName + ":" + new Integer(dockerReg.port).toString();
    } else {
      fullString = dockerReg.hostName;
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

    public DockerRegistry(
        String hostNameParam,
        int portParam,
        String userNameParam,
        String passwordParam,
        boolean useCredentialsParam) {
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
