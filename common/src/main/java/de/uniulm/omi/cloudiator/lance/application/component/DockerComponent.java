package de.uniulm.omi.cloudiator.lance.application.component;

import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.DYN_GROUP_KEY;
import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.DYN_HANDLER_KEY;

import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;

import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DockerComponent extends AbstractComponent {
    /*DockerComponent(String nameParam, ComponentId idParam, List<InPort> inPortsParam,
        List<OutPort> outPortsParam, Map<String, Class<?>> propertiesParam,
        HashMap<String, ? extends Serializable> propertyValuesParam) {
        super(nameParam, idParam, inPortsParam, outPortsParam, propertiesParam, propertyValuesParam);
    }*/
  private final static String updateScriptKey = "updatescript";
  private static final long serialVersionUID = -1145375438483791542L;

  private final EntireDockerCommands entireDockerCommands;
  private final String imageName;
  private final String imageFolder;
  private final String tag;
  private final String digestSHA256;
  private final String dynGroupVal;
  private final String dynHandlerVal;
  private final String updateScriptFilePath;
  private String containerName;

  public DockerComponent(Builder builder) {
    super(builder);
    entireDockerCommands = builder.entireDockerCommandsParam;
    imageName = builder.imageNameParam;

    if(builder.imageFolderParam == null)
      this.imageFolder = "";
    else
      this.imageFolder = builder.imageFolderParam;

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

    List<String> createEnv = builder.entireDockerCommandsParam.getCreate().getSetOptions().get(Option.ENVIRONMENT);
    dynGroupVal = filterEnvVal(createEnv, LcaRegistryConstants.regEntries.get(DYN_GROUP_KEY));
    dynHandlerVal = filterEnvVal(createEnv, LcaRegistryConstants.regEntries.get(DYN_HANDLER_KEY));
    updateScriptFilePath = filterEnvVal(createEnv, updateScriptKey);
  }

  private static String filterEnvVal(List<String> envStrings, String envKey) {
    for(String envStr: envStrings) {
      String[] content = envStr.split("=");

      if(content.length==2 && content[0].equals(envKey)) {
        return content[1];
      }
    }

    return "";
  }

  public EntireDockerCommands getEntireDockerCommands() {
    return this.entireDockerCommands;
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

  public String getFullDockerCommand(DockerCommand.Type cType) throws DockerCommandException {
    StringBuilder builder = new StringBuilder();
    builder.append(DockerCommand.Type.mapCommandToString(cType) + " ");
    builder.append(entireDockerCommands.getSetOptionsString(cType) + " ");
    builder.append(getFullIdentifier(cType) + " ");
    builder.append(entireDockerCommands.getSetOsCommandString(cType) + " ");
    builder.append(entireDockerCommands.getSetArgsString(cType));

    return builder.toString();
  }

  public void setContainerName(ComponentInstanceId id) throws DockerCommandException {
    entireDockerCommands.setOption(DockerCommand.Type.CREATE, Option.NAME, buildNameOptionFromId(id));
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
        entireDockerCommands.setOption(DockerCommand.Type.CREATE, Option.PORT, i.toString());
      } else {
        entireDockerCommands.setOption(DockerCommand.Type.CREATE, Option.PORT, i.toString() + ":" + j.toString());
      }
    }
  }


  public String getContainerName() throws DockerCommandException {
    return entireDockerCommands.getContainerName(DockerCommand.Type.CREATE);
  }

  public String getUpdateScriptFilePath() {
    return updateScriptFilePath;
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

    if (!str.equals("")) {
      returnStr = str + "/";
    } else {
      returnStr = str;
    }

    return returnStr;
  }

  @Override
  public boolean isDynamicComponent() {
    if (dynGroupVal.equals("")) {
      return false;
    }

    return true;
  }

  @Override
  public boolean isDynamicHandler() {
    if (dynHandlerVal.equals("")) {
      return false;
    }

    return true;
  }

  @Override
  public String getDynamicGroup() throws ContainerException {
    if (!isDynamicComponent()) {
      throw new ContainerException(String.format(
          "No dynamic group name set for Docker component %s.", containerName));
    }

    return dynGroupVal;
  }

  @Override
  public String getDynamicHandler() throws ContainerException {
    if (!isDynamicHandler()) {
      throw new ContainerException(String.format(
          "No dynamic group name associated with dynamic handler for Docker component %s.", containerName));
    }

    return dynHandlerVal;
  }

  public static class Builder extends AbstractComponent.Builder<Builder> {
    private final EntireDockerCommands entireDockerCommandsParam;
    private final String imageNameParam;
    private String imageFolderParam;
    private String tagParam;
    private String digestSHA256Param;
    private String containerNameParam;

    public Builder(EntireDockerCommands entireDockerCommands, String imageName) {
      this.entireDockerCommandsParam = entireDockerCommands;
      this.imageNameParam = imageName;
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
