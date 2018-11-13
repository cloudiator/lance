package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import static java.util.Collections.singletonMap;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.RegistryAuth;
import com.spotify.docker.client.messages.RegistryConfigs;
import de.uniulm.omi.cloudiator.lance.application.component.RemoteDockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;

public class RemoteDockerContainerLogic extends DockerContainerLogic {

  private final RemoteDockerComponent comp;

  //todo: Better construct a Builder that fits into the Class Hierarchy
  public RemoteDockerContainerLogic(DockerContainerLogic.Builder builder, RemoteDockerComponent comp) {
    super(builder);
    this.comp = comp;
  }

  @Override
  String getFullImageName() {
    return comp.getFullImageName();
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      String target = imageHandler.doPullImages(myId, getFullImageName());
      executeCreation(target);
    } catch(DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    }
  }
}
