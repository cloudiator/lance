package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DockerContainerLogic extends AbstractDockerContainerLogic {
  private final DockerComponent myComponent;
  protected final DockerImageHandler imageHandler;

  public DockerContainerLogic(Builder builder) {
    super(builder);
    this.myComponent = builder.myComponent;
    this.imageHandler = new DockerImageHandler(new DockerOperatingSystemTranslator(),
        builder.client, builder.myComponent, builder.dockerConfig);
    try {
      myComponent.setContainerName(this.myId);
    } catch (DockerCommandException ce) {
      LOGGER.error("Cannot set name for Docker container for component:" + myId, ce);
    }
  }

  @Override
  public void doCreate() throws ContainerException {
    try {
      String fullImageName = getFullImageName();
      client.pullImage(fullImageName);
      client.executeSingleDockerCommand(myComponent.getFullDockerCommand(DockerCommand.CREATE));
    } catch(DockerException de) {
      throw new ContainerException("docker problems. cannot create container " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  @Override
  void setStaticEnvironment(DockerShell shell, BashExportBasedVisitor visitor) throws ContainerException {
    Map<String,String> envVarsStaticTmp = new HashMap<>();

    for(Entry<String, String> entry: envVarsStatic.entrySet()) {
      //todo: not needed in post-colosseum version, as the environment-var names should be set correctly then
      if(!translateMap.containsKey(entry.getKey()))
        continue;

      envVarsStaticTmp.put(translateMap.get(entry.getKey()),entry.getValue());
    }

    executeGenericEnvSetting(envVarsStaticTmp);
  }

  @Override
  void setDynamicEnvironment(BashExportBasedVisitor visitor, PortDiff<DownstreamAddress> diff) throws ContainerException {
    this.myComponent.injectDeploymentContext(this.deploymentContext);
    networkHandler.generateDynamicEnvVars(diff);
    this.myComponent.generateDynamicEnvVars();
    collectDynamicEnvVars();
    executeGenericEnvSetting(envVarsDynamic);
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
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
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

  @Override
  String getFullImageName() {
    return myComponent.getFullImageName();
  }

  private DockerShell executeGenericStart() throws ContainerException {
    final DockerShell dshell;
    try {
      dshell = client.executeProgressingDockerCommand(myComponent.getFullDockerCommand(DockerCommand.START));
      BashExportBasedVisitor visitor = new BashExportBasedVisitor(dshell);
      setStaticEnvironment(dshell, visitor);
      //Setting Dynamic-Envvars here fails, because pub-ip would be set to <unknown> which is invalid bash syntax
      //setDynamicEnvironment(visitor, null);
    } catch (DockerException de) {
      throw new ContainerException("cannot start container: " + myId, de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }

    return dshell;
  }

  private void executeGenericEnvSetting(Map<String,String> vars) throws ContainerException {
    try {
      String envStr = buildEnvString(vars);
      String envCmdStr = "exec -i " + envStr +  myComponent.getContainerName() + " bash";
      client.executeProgressingDockerCommand(envCmdStr);
    } catch (DockerException de) {
      throw new ContainerException(de);
    } catch(DockerCommandException ce) {
      throw new ContainerException(ce);
    }
  }

  private String buildEnvString(Map<String,String> vars) {
    StringBuilder builder = new StringBuilder();

    for(Entry<String, String> var: vars.entrySet()) {
      builder.append("-e " + var.getKey()+"="+var.getValue() + " ");
    }

    return builder.toString();
  }

  public static class Builder extends AbstractDockerContainerLogic.Builder<DockerComponent,Builder> {

    @Override
    public DockerContainerLogic build() {
      return new DockerContainerLogic(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
