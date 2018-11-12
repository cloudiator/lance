package de.uniulm.omi.cloudiator.lance.application.component;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public class LifecycleComponent extends AbstractComponent {
    private final LifecycleStore lifecycle;

    public static class Builder extends AbstractComponent.Builder<Builder> {
      private volatile LifecycleStore lifecycle;

      public Builder(String name, ComponentId id) {
        //this.lifecycle = lifecycle;
        this.nameParam = name;
        this.myIdParam = id;
      }

      public static Builder createBuilder(String name, ComponentId componentId) {
        return new Builder(name,  componentId);
      }

      public Builder addLifecycleStore(LifecycleStore lifecycleStore) {
        this.lifecycle = lifecycleStore;
        return this;
      }

      @Override
      public LifecycleComponent build() {
        return new LifecycleComponent(this);
      }

      @Override
      protected Builder self() {
        return this;
      }
    }

    private LifecycleComponent(Builder builder) {
        super(builder);
        lifecycle = builder.lifecycle;
    }

    public LifecycleStore getLifecycleStore() {
        return lifecycle;
    }
}
