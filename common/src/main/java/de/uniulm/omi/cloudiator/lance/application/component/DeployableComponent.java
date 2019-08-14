package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public class DeployableComponent extends AbstractComponent {
  private final LifecycleStore lifecycle;

  @Override
  public boolean isDynamicComponent() {
    return false;
  }

  @Override
  public boolean isDynamicHandler() {
    return false;
  }

  @Override
  public String getDynamicGroup() throws ContainerException {
    throw new ContainerException("Deployable Component cannot be in a dynamic group");
  }

  @Override
  public String getDynamicHandler() throws ContainerException {
    throw new ContainerException("Deployable Component cannot be a dynamic group handler");
  }

  public static class Builder extends AbstractComponent.Builder<Builder> {
    private volatile LifecycleStore lifecycle;

    public Builder(String name, ComponentId id) {
      //this.lifecycle = lifecycle;
      this.nameParam = name;
      this.myIdParam = id;
    }

    public Builder addLifecycleStore(LifecycleStore lifecycleStore) {
      this.lifecycle = lifecycleStore;
      return this;
    }

    public static Builder createBuilder(String name, ComponentId componentId) {
      return new Builder(name,  componentId);
    }

    @Override
    public DeployableComponent build() {
      return new DeployableComponent(this);
    }

    @Override
    protected Builder self() {
        return this;
      }
  }

  private DeployableComponent(Builder builder) {
      super(builder);
      lifecycle = builder.lifecycle;
  }

  public LifecycleStore getLifecycleStore() {
    return lifecycle;
  }
}
