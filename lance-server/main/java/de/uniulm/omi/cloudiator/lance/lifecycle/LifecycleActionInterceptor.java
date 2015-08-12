package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

public interface LifecycleActionInterceptor {

    void prepare(LifecycleHandlerType type);

    void postprocess(LifecycleHandlerType type);

	ComponentInstanceId getComponentId();

}
