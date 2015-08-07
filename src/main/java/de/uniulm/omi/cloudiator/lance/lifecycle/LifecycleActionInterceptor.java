package de.uniulm.omi.cloudiator.lance.lifecycle;

public interface LifecycleActionInterceptor {

	void prepare(LifecycleHandlerType type);

	void postprocess(LifecycleHandlerType type);

}
