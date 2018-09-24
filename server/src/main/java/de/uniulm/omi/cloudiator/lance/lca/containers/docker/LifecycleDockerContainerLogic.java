package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.component.LifecycleComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public class LifecycleDockerContainerLogic extends AbstractDockerContainerLogic {
  private final LifecycleComponent myComponent;
  private final DockerImageHandler imageHandler;

  private LifecycleDockerContainerLogic(Builder builder) {
    super(builder);
    this.myComponent = builder.myComponent;
    this.imageHandler = new DockerImageHandler(builder.osParam, new DockerOperatingSystemTranslator(),
        builder.client, builder.myComponent, builder.dockerConfig);
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      String target = imageHandler.doPullImages(myId);
      executeCreation(target);
    } catch(DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    }
  }

  @Override
  void setDynamicEnvironment(BashExportBasedVisitor visitor,
      PortDiff<DownstreamAddress> diff) throws ContainerException {
    this.myComponent.injectDeploymentContext(this.deploymentContext);
    networkHandler.generateDynamicEnvVars(diff);
    this.myComponent.generateDynamicEnvVars();
    collectDynamicEnvVars();
    networkHandler.accept(visitor);
    this.myComponent.accept(visitor);
  }

  @Override
  public void doInit(LifecycleStore store) throws ContainerException {
      try {
        //Environment still set (in logic.doInit call in BootstrapTransitionAction)
        //could return a shell
        doStartContainer();
      } catch (ContainerException ce) {
        throw ce;
      } catch (Exception ex) {
        throw new ContainerException(ex);
      }
  }

  @Override
  public void doDestroy(boolean force) throws ContainerException {
    /* currently docker ignores the flag */
    try {
      //Environment still set (in logic.preDestroy call in DestroyTransitionAction)
      client.stopContainer(myId);
    } catch (DockerException de) {
      throw new ContainerException(de);
    }
  }

  @Override
  void postPreInstall() {
    try {
      imageHandler.runPostInstallAction(myId);
    } catch (DockerException de) {
      LOGGER.warn("could not update finalise image handling.", de);
    }
  }

  @Override
  void collectDynamicEnvVars() {
    envVarsDynamic.putAll(myComponent.getEnvVars());
    envVarsDynamic.putAll(networkHandler.getEnvVars());
  }

  static class Builder extends AbstractBuilder<LifecycleComponent> {

    public Builder(){}

    @Override
    public AbstractDockerContainerLogic build() {
      return new LifecycleDockerContainerLogic(this);
    }
  }
}
