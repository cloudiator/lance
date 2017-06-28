package de.uniulm.omi.cloudiator.lance.lca;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;

/**
 * Created by frankgriesinger on 28.06.2017.
 */
public class RestBodyDeployComponent {
  private byte[] deploymentContext;
  private byte[] deployableComponent;
  private byte[] operatingSystem;
  private byte[] containerType;

  public RestBodyDeployComponent(byte[] deploymentContext,
      byte[] deployableComponent, byte[] operatingSystem,
      byte[] containerType) {
    this.deploymentContext = deploymentContext;
    this.deployableComponent = deployableComponent;
    this.operatingSystem = operatingSystem;
    this.containerType = containerType;
  }

  public byte[] getDeploymentContext() {
    return deploymentContext;
  }

  public void setDeploymentContext(
      byte[] deploymentContext) {
    this.deploymentContext = deploymentContext;
  }

  public byte[] getDeployableComponent() {
    return deployableComponent;
  }

  public void setDeployableComponent(
      byte[] deployableComponent) {
    this.deployableComponent = deployableComponent;
  }

  public byte[] getOperatingSystem() {
    return operatingSystem;
  }

  public void setOperatingSystem(
      byte[] operatingSystem) {
    this.operatingSystem = operatingSystem;
  }

  public byte[] getContainerType() {
    return containerType;
  }

  public void setContainerType(byte[] containerType) {
    this.containerType = containerType;
  }
}
