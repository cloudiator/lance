/*
 * Copyright (c) 2014-2015 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DockerImageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDockerContainerLogic.class);

  private OperatingSystem os;
  private final DockerConnector client;
  private final AbstractComponent myComponent;
  private final DockerConfiguration dockerConfig;

  private volatile ImageCreationType initSource;

  DockerImageHandler(OperatingSystem osParam, DockerConnector clientParam,
      AbstractComponent componentParam, DockerConfiguration dockerConfigParam) {
    if (osParam == null) {
      throw new NullPointerException("operating system has to be set.");
    }

    dockerConfig = dockerConfigParam;
    os = osParam;
    client = clientParam;
    myComponent = componentParam;
  }

  DockerImageHandler(DockerConnector clientParam, AbstractComponent componentParam,
      DockerConfiguration dockerConfigParam) {

    dockerConfig = dockerConfigParam;
    client = clientParam;
    myComponent = componentParam;
  }

  static String createComponentInstallId(AbstractComponent myComponent) {
    return "dockering." + "component." + myComponent.getComponentId().toString();
  }

  private String createComponentInstallId() {
    return createComponentInstallId(myComponent);
  }

  private String buildImageTagName(ImageCreationType type, String componentInstallId) throws DockerException {
    final String key;
    switch (type) {
      case COMPONENT:
        key = imageFromComponent(componentInstallId);
        break;
      case OPERATING_SYSTEM:
        key = os.dockerImage().orElseThrow(() -> new DockerException(
            String.format("Operating system %s does not provide a docker image", os)));
        break;
      case COMPONENT_INSTANCE:
        throw new UnsupportedOperationException();
      default:
        throw new IllegalArgumentException();
    }
    return key;
  }

  private String imageFromComponent(String componentInstallId) {
    String tmpkey = componentInstallId;
    String ostag = os.toString();
    ostag = ostag.replaceAll(":", "_");
    String tmp = tmpkey.toLowerCase() + ":" + ostag.toLowerCase();
    if (!dockerConfig.registryCanBeUsed()) {
      return tmp;
    }
    return dockerConfig.prependRegistry(tmp);
  }

  private String doGetSingleImage(String key) throws DockerException {
    if (client.findImage(key) != null) {
      return key;
    }

    try {
      client.pullImage(key);
      return key;
    } catch (DockerException de) {
      LOGGER.debug("could not pull image: " + key + " creating a new one.");
      return null;
    }
  }

  /**
   * @param myId the instance id of the container
   */
  String doPullImages(ComponentInstanceId myId) throws DockerException {
    // first step: try to find matching image for configured component
    String result = searchImageInLocalCache();
    if (result == null) {
      // second step: try to find matching image for prepared component
      // in case a custom docker registry is configured
      result = getImageFromPrivateRepository();
      if (result == null) {
        // third step: fall back to the operating system //
        result = getImageFromDefaultLocation();
      }
    }
    if (result != null) {
      return result;
    }

    throw new DockerException("cannot pull image: " + myId);
  }

  String doPullImages(String imageName) throws DockerException {
    // first step: try to find matching image for configured component
    String result = searchImageInLocalCache(imageName);
    if (result == null) {
      // second step: try to find matching image for prepared component
      // in case a custom docker registry is configured
      result = getImageFromPrivateRepository(imageName);
      if (result == null) {
        // third step: fall back to the operating system //
        result = getImageFromDefaultLocation(imageName);
      }
    }
    if (result != null) {
      return result;
    }

    throw new DockerException("cannot pull image: " + imageName);
  }

  private String searchImageInLocalCache(String imageName) {
    // currently not implemented;
    return null;
  }

  private String searchImageInLocalCache() {
    // currently not implemented;
    return null;
  }

  private String getImageFromPrivateRepository(String imageName) throws DockerException {
    if (!dockerConfig.registryCanBeUsed()) {
      return null;
    }

    String regExpandedImageName = dockerConfig.prependRegistry(imageName);
    String result = null;
    String userName = dockerConfig.getUserName();
    String password = dockerConfig.getPassword();
    boolean useCredentials = dockerConfig.useCredentials();
    String hostName = DockerConfiguration.DockerConfigurationFields.getHostname();
    int port = DockerConfiguration.DockerConfigurationFields.getPort();
    String endPoint = hostName + (!(port < 0) && port < 65555 ? ":" + Integer.toString(port) : "");

    if (useCredentials) {
      client.loginReg(userName, password, endPoint);
    }

    result = doGetSingleImage(regExpandedImageName);

    if (useCredentials) {
      client.logoutReg(endPoint);
    }

    if (result != null) {
      LOGGER.info("pulled image: " + regExpandedImageName + " from private registry");
      initSource = ImageCreationType.COMPONENT_INSTANCE;
    }

    return result;
  }

  private String getImageFromPrivateRepository() throws DockerException {
    if (!dockerConfig.registryCanBeUsed()) {
      return null;
    }

    String componentInstallId = createComponentInstallId();
    String target = buildImageTagName(ImageCreationType.COMPONENT, componentInstallId);
    String result = doGetSingleImage(target);
    if (result != null) {
      LOGGER.info("pulled prepared image: " + result);
      initSource = ImageCreationType.COMPONENT;
      return result;
    }
    return null;
  }

  private String getImageFromDefaultLocation(String imageName) throws DockerException {
    String result = doGetSingleImage(imageName);
    if (result != null) {
      LOGGER.info("pulled default image: " + result);
      initSource = ImageCreationType.COMPONENT_INSTANCE;
      return result;
    }
    return null;
  }

  private String getImageFromDefaultLocation() throws DockerException {
    String target = buildImageTagName(ImageCreationType.OPERATING_SYSTEM, null);
    String result = doGetSingleImage(target);
    if (result != null) {
      LOGGER.info("pulled default image: " + result);
      initSource = ImageCreationType.OPERATING_SYSTEM;
      return result;
    }
    return null;
  }

  void runPostInstallAction(ComponentInstanceId myId) throws DockerException {
    if (initSource == ImageCreationType.OPERATING_SYSTEM) {
      String componentInstallId = createComponentInstallId();
      String target = buildImageTagName(ImageCreationType.COMPONENT, componentInstallId);
      // we probably will not need this return value
      // let's keep it for debugging purposes, though
      // @SuppressWarnings("unused") String imageSnapshot =
      client.createSnapshotImage(myId, target);
      client.pushImage(target);
    }
  }

  static enum ImageCreationType {
    COMPONENT,
    COMPONENT_INSTANCE,
    OPERATING_SYSTEM,
  }

  OperatingSystem getOperatingSystem() {
    return os;
  }
}
