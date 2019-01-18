package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandStack;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;

public class DockerComponent extends AbstractComponent {
  private final DockerCommandStack dockerCommandStack;
  private final String imageName;
  private final String nameSpace;
  private final String tag;
  private final String digestSHA256;
  private String containerName;

  public DockerComponent(Builder builder) {
    super(builder);
    dockerCommandStack = builder.dockerCommandStackParam;
    imageName = builder.imageNameParam;

    if(builder.imageFolderParam == null)
      this.nameSpace = "";
    else
      this.nameSpace = builder.imageFolderParam;

    if(builder.tagParam == null)
      this.tag = "";
    else
      this.tag = builder.tagParam;

    if(builder.digestSHA256Param == null)
      this.digestSHA256 = "";
    else
      this.digestSHA256 = builder.digestSHA256Param;

    if(builder.containerNameParam == null)
      this.containerName = "<unknown container>";
    else
      this.containerName = builder.containerNameParam;
  }

  public DockerCommandStack getDockerCommandStack() {
    return this.dockerCommandStack;
  }

  public String getImageFolder() {
    return this.nameSpace;
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

  public void setContainerName(ComponentInstanceId id) throws DockerCommandException {
    //todo: better, overwrite option
    dockerCommandStack.appendOption(DockerCommand.Type.CREATE, Option.NAME, buildNameOptionFromId(id));
    containerName = buildNameOptionFromId(id);
  }

  public void setPort(Map<Integer, Integer> inPortsParam) throws DockerCommandException {
    if(inPortsParam.size()>1) {
      LOGGER.info("Trying to set multiple ports for Docker Component " +  containerName);
    }

    for(Entry<Integer, Integer> entry : inPortsParam.entrySet()) {
      Integer i = entry.getKey();
      Integer j = entry.getValue();
      if(j.intValue() < 0 || j.intValue() > 65536) {
        //todo: better, overwrite option
        dockerCommandStack.appendOption(DockerCommand.Type.CREATE, Option.PORT, i.toString());
      } else {
        //todo: better, overwrite option
        dockerCommandStack.appendOption(DockerCommand.Type.CREATE, Option.PORT, i.toString() + ":" + j.toString());
      }
    }
  }


  public String getContainerName() throws DockerCommandException {
    return dockerCommandStack.getContainerName(DockerCommand.Type.CREATE);
  }

  private static String buildNameOptionFromId(ComponentInstanceId id) {
    return "dockering__"+ id.toString();
  }

  public String getFullIdentifier(DockerCommand.Type cType) throws IllegalArgumentException {
    if (cType == DockerCommand.Type.CREATE || cType == DockerCommand.Type.RUN) {
      return getFullImageName();
    }
    else {
      return containerName;
    }
  }

  public String getFullImageName() throws IllegalArgumentException {
    StringBuilder builder = new StringBuilder();

    builder.append(buildPrefixString(nameSpace));
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

  public static class Builder extends AbstractComponent.Builder<Builder> {
    private final DockerCommandStack dockerCommandStackParam;
    private final String imageNameParam;
    private String imageFolderParam;
    private String tagParam;
    private String digestSHA256Param;
    private String containerNameParam;

    public Builder(DockerCommandStack dockerCommandStack, String imageName) {
      this.dockerCommandStackParam = dockerCommandStack;
      this.imageNameParam = imageName;
    }

    public Builder nameSpace(String nameSpace) {
      this.imageFolderParam = nameSpace;
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

    private static void checkSHA256(String sha256) throws NoSuchAlgorithmException, UnsupportedEncodingException {
      MessageDigest crypt = MessageDigest.getInstance("SHA-256");
      crypt.reset();
      //Java uses UTF-16 for the internal text representation
      crypt.update(sha256.getBytes("UTF-16"));

      if(crypt.getDigestLength() != 32)
        throw new UnsupportedEncodingException("SHA needs to be 256 bits long");
    }
  }

}
