package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import java.util.Map;

public class DockerContainerLogic extends AbstractDockerContainerLogic {
  private final DockerComponent myComponent;
  private final DockerImageHandler imageHandler;

  private DockerContainerLogic(Builder builder) {
    super(builder);
    this.myComponent = builder.myComponent;
    this.imageHandler = new DockerImageHandler(builder.osParam, new DockerOperatingSystemTranslator(),
        builder.client, builder.myComponent, builder.dockerConfig);
    myComponent.setContainerName(this.myId);
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      String fullImageName = myComponent.getFullImageName();
      client.pullImage(fullImageName);
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.CREATE));
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
  public void doInit(LifecycleStore store) throws ContainerException {
      try {
        //Environment still set (in logic.doInit call in BootstrapTransitionAction)
        //could return a shell
        executeGenericStart();
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
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.STOP));
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

  private DockerShell executeGenericStart() throws ContainerException {
    final DockerShell dshell;
    try {
      dshell = client.executeProgressingDockerCommand(myComponent.getFullDockerCommand(DockerCommand.START));
      setStaticEnvironment();
    } catch (DockerException de) {
      throw new ContainerException("cannot start container: " + myId, de);
    }

    return dshell;
  }

  static class Builder extends AbstractBuilder<DockerComponent> {

    public Builder(){}

    @Override
    public AbstractDockerContainerLogic build() {
      return new DockerContainerLogic(this);
    }
  }
}
