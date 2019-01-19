package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.component.RemoteDockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandStack;

public class RemoteDockerContainerLogic extends DockerContainerLogic {

  private final RemoteDockerComponent comp;

  public RemoteDockerContainerLogic(
      DockerContainerLogic.Builder builder, RemoteDockerComponent comp) {
    super(builder);
    this.comp = comp;
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      DockerCommandStack dockerCommandStack = comp.getDockerCommandStack();
      final String fullImageName = comp.getFullImageNameRegStripped();
      final String fullIdentifier = comp.getFullIdentifier(Type.CREATE);

      execHandler.creatContainer(dockerCommandStack, envVarHandler, fullImageName, fullIdentifier);
    } catch (DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    }
  }
}

