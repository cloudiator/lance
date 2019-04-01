package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public class LifecycleDockerContainerLogic extends AbstractDockerContainerLogic {
  private final DeployableComponent myComponent;
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
      checkContainerStatus("created");
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
        checkContainerStatus("running");
      } catch (ContainerException ce) {
        throw ce;
      } catch (Exception ex) {
        throw new ContainerException(ex);
      }
  }

  @Override
  public void doDestroy(boolean force, boolean remove) throws ContainerException {
    /* currently docker ignores both flags */
    try {
      //Environment still set (in logic.preDestroy call in DestroyTransitionAction)
      client.stopContainer(myId);
      checkContainerStatus("exited");
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
  protected void checkContainerStatus(String status) throws DockerException {
    LOGGER.warn("Method getContainerId not implemented for Docker Lifecycle Component");
  }

  @Override
  protected String getContainerId() throws DockerException {
    LOGGER.warn("Method getContainerId not implemented for Docker Lifecycle Component");
    return "";
  }

  @Override
  void collectDynamicEnvVars() {
    envVarsDynamic.putAll(myComponent.getEnvVars());
    envVarsDynamic.putAll(networkHandler.getEnvVars());
  }

  @Override
  public AbstractComponent getComponent() {
    return myComponent;
  }

  //No dynamic properties settable for LifecycleComponents
  @Override
  public boolean isValidDynamicProperty(String key) {
    return false;
  }

  @Override
  public void doStartDynHandling(GlobalRegistryAccessor accessor) throws ContainerException {
    throw new ContainerException("No dynamic handling possible for LifecycleDockerContainer");
  }

  @Override
  public void doStopDynHandling() throws ContainerException {
    throw new ContainerException("No dynamic handling possible for LifecycleDockerContainer");
  }

  String getFullImageName() {
    //ubuntu 14.04
    return "ubuntu";
  }

  public static class Builder extends AbstractDockerContainerLogic.Builder<DeployableComponent,Builder> {
    private final OperatingSystem osParam;

    public Builder(OperatingSystem osParam){
      this.osParam = osParam;
    }

    @Override
    public LifecycleDockerContainerLogic build() {
      return new LifecycleDockerContainerLogic(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
