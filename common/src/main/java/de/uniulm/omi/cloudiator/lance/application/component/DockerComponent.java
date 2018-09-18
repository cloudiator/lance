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

//TODO: Implement. DeployableComponent should among other things not depend on LifeCycleStore but LifecycleComponent should
public class DockerComponent extends DeployableComponent implements ComponentFactory<DockerComponent> {
    DockerComponent(String nameParam, ComponentId idParam, LifecycleStore lifecycleStoreParam,
                       List<InPort> inPortsParam, List<OutPort> outPortsParam, Map<String, Class<?>> propertiesParam,
                       HashMap<String, ? extends Serializable> propertyValuesParam) {
        super(nameParam, idParam, lifecycleStoreParam, inPortsParam, outPortsParam, propertiesParam, propertyValuesParam);
    }

  private static final Logger LOGGER = LoggerFactory.getLogger(DockerComponent.class);
  //todo: make this variable initializable via the constructor
  private EntireDockerCommands entireDockerCommands = new EntireDockerCommands();
  //todo: make this variable initializable via the constructor
  private String registryUri = "";
  //todo: make this variable initializable via the constructor
  private String imageFolder = "";
  //todo: make this variable initializable via the constructor
  private String imageName = "";
  //todo: make this variable initializable via the constructor
  private String tag = "";
  //todo: make this variable initializable via the constructor
  private String digestSHA256 = "";

  //needed temporarily to make ComponentBuilder class work, as long as DockerComponent and LifecycleComponent inherit from DeployableComponent
  public DockerComponent() {
                           super();
                                   }

  @Override
  public DockerComponent newObject(String nameParam, ComponentId idParam,
      LifecycleStore lifecycleStoreParam, List<InPort> inPortsParam, List<OutPort> outPortsParam,
      Map<String, Class<?>> propertiesParam,
      HashMap<String, ? extends Serializable> propertyValuesParam) {
      return new DockerComponent(nameParam, idParam, lifecycleStoreParam,  inPortsParam, outPortsParam, propertiesParam, propertyValuesParam);
  }

  public void setEntireDockerCommands(EntireDockerCommands cmds) {
    this.entireDockerCommands = cmds;
  }

  public EntireDockerCommands getEntireDockerCommands() {
    return this.entireDockerCommands;
  }

  public void setRegistryUri(String uri) {
      this.registryUri = uri;
  }

  public String getRegistryUri() {
    return this.registryUri;
  }

  public void setImageFolder(String folder) {
    this.imageFolder = folder;
  }

  public String getImageFolder() {
    return this.imageFolder;
  }

  public void setImageName (String name) {
    this.imageName = name;
  }

  public String getImageName () {
    return this.imageName;
  }

  public void setTag (String tag) {
    this.tag = tag;
  }

  public String getTag () {
    return this.tag;
  }

  public void setDigestSHA256 (String sha256) throws NoSuchAlgorithmException, UnsupportedEncodingException  {
    MessageDigest crypt = MessageDigest.getInstance("SHA-256");
    crypt.reset();
    //Java uses UTF-16 for the internal text representation
    crypt.update(sha256.getBytes("UTF-8"));

    if(crypt.getDigestLength() != 32)
      throw new UnsupportedEncodingException("SHA needs to be 256 bits long");
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

  private static String buildNameOptionFromId(ComponentInstanceId id) {
    return "dockering__"+ id.toString();
  }

  public String getFullIdentifier(DockerCommand cmd) throws IllegalArgumentException {
    if (cmd == DockerCommand.CREATE)
      return getFullImageName();
    else {
      try {
        return entireDockerCommands.getContainerName(DockerCommand.CREATE);
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  public String getFullImageName() {
    StringBuilder builder = new StringBuilder();
    builder.append(getPrefixString(registryUri));
    builder.append(getPrefixString(imageFolder));
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

  private static String getPrefixString(String str) {
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
