package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import static de.uniulm.omi.cloudiator.lance.application.component.ComponentType.DOCKER;

import de.uniulm.omi.cloudiator.lance.application.component.ComponentType;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;

public class LegacyDockerContainerLogic extends AbstractDockerContainerLogic {
  private final DeployableComponent myComponent;
  private final DockerImageHandler imageHandler;

  private LegacyDockerContainerLogic(Builder builder) {
    super(builder);
    this.myComponent = builder.myComponent;
    this.imageHandler = new DockerImageHandler(builder.osParam, new DockerOperatingSystemTranslator(),
        builder.client, builder.myComponent, builder.dockerConfig);
  }

  @Override
  public void doCreate() throws ContainerException {
    String target;
    try {
      ComponentType type = myComponent.getType();
      if (type == DOCKER) {
        String imageName = myComponent.getName();
        target = imageHandler.doPullImages(myId, imageName);
      }
      else
        target = imageHandler.doPullImages(myId);

      executeCreation(target);
    } catch(DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    }
  }

  @Override
  void setCompleteStaticEnvironment(PortDiff<DownstreamAddress> diff) throws ContainerException {
    BashExportBasedVisitor visitor = setupCompleteStaticEnvironment(diff);
    myComponent.accept(deploymentContext, visitor);
  }

  @Override
  void postPreInstall() {
    try {
      imageHandler.runPostInstallAction(myId);
    } catch (DockerException de) {
      LOGGER.warn("could not update finalise image handling.", de);
    }
  }

  static class Builder extends AbstractBuilder<DeployableComponent> {

    public Builder(){}

    @Override
    public AbstractDockerContainerLogic build() {
      return new LegacyDockerContainerLogic(this);
    }
  }
}
