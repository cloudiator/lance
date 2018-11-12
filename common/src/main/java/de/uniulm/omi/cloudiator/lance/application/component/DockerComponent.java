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

public class DockerComponent extends AbstractComponent {
    /*DockerComponent(String nameParam, ComponentId idParam, List<InPort> inPortsParam,
        List<OutPort> outPortsParam, Map<String, Class<?>> propertiesParam,
        HashMap<String, ? extends Serializable> propertyValuesParam) {
        super(nameParam, idParam, inPortsParam, outPortsParam, propertiesParam, propertyValuesParam);
    }*/

  private EntireDockerCommands entireDockerCommands;
  private String imageName;
  private String registryUri;
  private String imageFolder;
  private String tag;
  private String digestSHA256;

  public static class Builder extends AbstractComponent.Builder<Builder> {
    private final EntireDockerCommands entireDockerCommandsParam;
    private final String imageNameParam;
    private String registryUriParam;
    private String imageFolderParam;
    private String tagParam;
    private String digestSHA256Param;

    public Builder(EntireDockerCommands entireDockerCommands, String imageName) {
      this.entireDockerCommandsParam = entireDockerCommands;
      this.imageNameParam = imageName;
    }

    public Builder registryUri(String registryUri) {
      this.registryUriParam = registryUri;
      return this;
    }

    public Builder imageFolder(String imageFolder) {
      this.imageFolderParam = imageFolder;
      return this;
    }

    public Builder tag(String tag) {
      this.tagParam = tag;
      return this;
    }

    public Builder digestSHA256(String digestSHA256) throws NoSuchAlgorithmException, UnsupportedEncodingException {
      checkSHA256(digestSHA256);
      this.digestSHA256Param = digestSHA256;
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

    private static void checkSHA256(String sha256) throws NoSuchAlgorithmException, UnsupportedEncodingException {
      MessageDigest crypt = MessageDigest.getInstance("SHA-256");
      crypt.reset();
      //Java uses UTF-16 for the internal text representation
      crypt.update(sha256.getBytes("UTF-16"));

      if(crypt.getDigestLength() != 32)
        throw new UnsupportedEncodingException("SHA needs to be 256 bits long");
    }
  }

  public DockerComponent(Builder builder) {
    super(builder);
    entireDockerCommands = builder.entireDockerCommandsParam;
    imageName = builder.imageNameParam;

    if(builder.imageFolderParam == null)
      this.imageFolder = "";
    else
      this.imageFolder = builder.imageFolderParam;

    if(builder.registryUriParam == null)
      this.registryUri = "";
    else
      this.registryUri = builder.registryUriParam;

    if(builder.tagParam == null)
      this.tag = "";
    else
      this.tag = builder.tagParam;

    if(builder.digestSHA256Param == null)
      this.digestSHA256 = "";
    else
      this.digestSHA256 = builder.digestSHA256Param;
  }

  public EntireDockerCommands getEntireDockerCommands() {
    return this.entireDockerCommands;
  }

  public String getRegistryUri() {
    return this.registryUri;
  }

  public String getImageFolder() {
    return this.imageFolder;
  }

  public String getImageName () {
    return this.imageName;
  }

  public String getTag () {
    return this.tag;
  }

  public String getDigestSHA256 () {
    return this.digestSHA256;
  }

  public String getFullDockerCommand(DockerCommand cmd) throws DockerCommandException {
    StringBuilder builder = new StringBuilder();
    builder.append(mapCommandToString(cmd) + " ");
    builder.append(entireDockerCommands.getSetOptionsString(cmd) + " ");
    builder.append(getFullIdentifier(cmd) + " ");
    builder.append(entireDockerCommands.getSetOsCommandString(cmd) + " ");
    builder.append(entireDockerCommands.getSetArgsString(cmd));

    return builder.toString();
  }

  public void setContainerName(ComponentInstanceId id) throws DockerCommandException {
    entireDockerCommands.setOption(DockerCommand.CREATE, Option.NAME, buildNameOptionFromId(id));
  }

  public String getContainerName() throws DockerCommandException {
    return entireDockerCommands.getContainerName(DockerCommand.CREATE);
  }

  private static String buildNameOptionFromId(ComponentInstanceId id) {
    return "dockering__"+ id.toString();
  }

  public String getFullIdentifier(DockerCommand cmd) throws IllegalArgumentException {
    if (cmd == DockerCommand.CREATE) {
      return getFullImageName();
    }
    else {
      try {
        return entireDockerCommands.getContainerName(DockerCommand.CREATE);
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  public String getFullImageName() throws IllegalArgumentException {
    StringBuilder builder = new StringBuilder();

    builder.append(buildPrefixString(registryUri));
    builder.append(buildPrefixString(imageFolder));
    builder.append(imageName);

    if(tag.equals("") && !digestSHA256.equals(""))
      builder.append("@sha256");

    builder.append(":");

    //prioritize tag
    if(!tag.equals("") && !digestSHA256.equals(""))
      builder.append(tag);
    else if(tag.equals("") && digestSHA256.equals(""))
      builder.append("latest");
    else if(!tag.equals("") && digestSHA256.equals(""))
      builder.append(tag);
    // == else if(tag.equals("") && !digestSHA256.equals(""))
    else
      builder.append(digestSHA256);

    return builder.toString();
  }

  private static String buildPrefixString(String str) {
      String returnStr;

    if(!str.equals(""))
      returnStr = str + "/";
    else
      returnStr = str;

    return returnStr;
  }

  private static String mapCommandToString(DockerCommand cmd) throws IllegalArgumentException {
    if(cmd==DockerCommand.CREATE)
      return "create";
    if(cmd==DockerCommand.START)
      return "start";
    if(cmd==DockerCommand.STOP)
      return "stop";
    else
      //todo insert String representation of DockerCommand in exception String
      throw new IllegalArgumentException("No mapping for this Docker Command available");
  }
}
