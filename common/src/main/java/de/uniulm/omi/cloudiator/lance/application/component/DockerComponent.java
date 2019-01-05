package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;

public class DockerComponent extends AbstractComponent {
  private final EntireDockerCommands entireDockerCommands;
  private final String imageName;
  private final String imageFolder;
  private final String tag;
  private final String digestSha256;
  private String containerName;

  public DockerComponent(Builder builder) {
    super(builder);
    entireDockerCommands = builder.entireDockerCommandsParam;
    imageName = builder.imageNameParam;

    if (builder.imageFolderParam == null) {
      this.imageFolder = "";
    } else {
      this.imageFolder = builder.imageFolderParam;
    }

    if (builder.tagParam == null) {
      this.tag = "";
    } else {
      this.tag = builder.tagParam;
    }

    if (builder.digestSha256Param == null) {
      this.digestSha256 = "";
    } else {
      this.digestSha256 = builder.digestSha256Param;
    }

    if (builder.containerNameParam == null) {
      this.containerName = "<unknown container>";
    } else {
      this.containerName = builder.containerNameParam;
    }
  }

  private static String buildNameOptionFromId(ComponentInstanceId id) {
    return "dockering__" + id.toString();
  }

  private static String buildPrefixString(String str) {
    String returnStr;

    if (!str.equals("")) {
      returnStr = str + "/";
    } else {
      returnStr = str;
    }

    return returnStr;
  }

  public EntireDockerCommands getEntireDockerCommands() {
    return this.entireDockerCommands;
  }

  public String getImageFolder() {
    return this.imageFolder;
  }

  public String getImageName() {
    return this.imageName;
  }

  public String getTag() {
    return this.tag;
  }

  public String getDigestSha256() {
    return this.digestSha256;
  }

  public String getFullDockerCommand(DockerCommand.Type commType) throws DockerCommandException {
    StringBuilder builder = new StringBuilder();
    builder.append(DockerCommand.Type.mapCommandToString(commType) + " ");
    builder.append(entireDockerCommands.getSetOptionsString(commType) + " ");
    builder.append(getFullIdentifier(commType) + " ");
    builder.append(entireDockerCommands.getSetOsCommandString(commType) + " ");
    builder.append(entireDockerCommands.getSetArgsString(commType));

    return builder.toString();
  }

  public void setPort(Map<Integer, Integer> inPortsParam) throws DockerCommandException {
    if (inPortsParam.size() > 1) {
      LOGGER.info("Trying to set multiple ports for Docker Component " + containerName);
    }

    for (Entry<Integer, Integer> entry : inPortsParam.entrySet()) {
      Integer hostPort = entry.getKey();
      Integer contPort = entry.getValue();
      if (contPort.intValue() < 0 || contPort.intValue() > 65536) {
        entireDockerCommands.setOption(DockerCommand.Type.CREATE, Option.PORT, hostPort.toString());
      } else {
        entireDockerCommands.setOption(
            DockerCommand.Type.CREATE,
            Option.PORT,
            hostPort.toString() + ":" + contPort.toString());
      }
    }
  }

  public String getContainerName() throws DockerCommandException {
    return entireDockerCommands.getContainerName(DockerCommand.Type.CREATE);
  }

  public void setContainerName(ComponentInstanceId id) throws DockerCommandException {
    entireDockerCommands.setOption(
        DockerCommand.Type.CREATE, Option.NAME, buildNameOptionFromId(id));
    containerName = buildNameOptionFromId(id);
  }

  public String getFullIdentifier(DockerCommand.Type commType) throws IllegalArgumentException {
    if (commType == DockerCommand.Type.CREATE || commType == DockerCommand.Type.RUN) {
      return getFullImageName();
    } else {
      return containerName;
    }
  }

  public String getFullImageName() throws IllegalArgumentException {
    StringBuilder builder = new StringBuilder();

    builder.append(buildPrefixString(imageFolder));
    builder.append(imageName);

    if (tag.equals("") && !digestSha256.equals("")) {
      builder.append("@sha256");
    }

    builder.append(":");

    // prioritize tag
    if (!tag.equals("") && !digestSha256.equals("")) {
      builder.append(tag);
    } else if (tag.equals("") && digestSha256.equals("")) {
      builder.append("latest");
    } else if (!tag.equals("") && digestSha256.equals("")) {
      builder.append(tag);
    } else {
      builder.append(digestSha256);
    }
    // == else if (tag.equals("") && !digestSha256.equals(""))

    return builder.toString();
  }

  public static class Builder extends AbstractComponent.Builder<Builder> {
    private final EntireDockerCommands entireDockerCommandsParam;
    private final String imageNameParam;
    private String imageFolderParam;
    private String tagParam;
    private String digestSha256Param;
    private String containerNameParam;

    public Builder(EntireDockerCommands entireDockerCommands, String imageName) {
      this.entireDockerCommandsParam = entireDockerCommands;
      this.imageNameParam = imageName;
    }

    private static void checkSha256(String sha256)
        throws NoSuchAlgorithmException, UnsupportedEncodingException {
      MessageDigest crypt = MessageDigest.getInstance("SHA-256");
      crypt.reset();
      // Java uses UTF-16 for the internal text representation
      crypt.update(sha256.getBytes("UTF-16"));

      if (crypt.getDigestLength() != 32) {
        throw new UnsupportedEncodingException("SHA needs to be 256 bits long");
      }
    }

    public Builder imageFolder(String imageFolder) {
      this.imageFolderParam = imageFolder;
      return this;
    }

    public Builder tag(String tag) {
      this.tagParam = tag;
      return this;
    }

    public Builder digestSha256(String digestSha256)
        throws NoSuchAlgorithmException, UnsupportedEncodingException {
      checkSha256(digestSha256);
      this.digestSha256Param = digestSha256;
      return this;
    }

    public Builder containerName(String containerName) {
      this.containerNameParam = containerName;
      return this;
    }

    @Override
    public DockerComponent build() {
      return new DockerComponent(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
