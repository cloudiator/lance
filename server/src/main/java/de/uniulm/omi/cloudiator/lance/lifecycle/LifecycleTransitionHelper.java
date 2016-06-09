package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.InitHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.InstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostStartHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostStopHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreStartHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreStopHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StopHandler;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

final class LifecycleTransitionHelper {	
	
	private static final TransitionAction EMPTY_TRANSITION_ACTION = new TransitionAction() {
		@Override
		public void transit(Object[] params) throws TransitionException {
			LifecycleController.getLogger().info("running empty transition");
		}
	};

	static void createInitAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.NEW).
			setEndState(LifecycleHandlerType.INIT).
			setErrorState(LifecycleHandlerType.INIT_FAILED).
			addTransitionAction(new DefaultLifecycleTransition<InitHandler>(storeParam, ctxParam, LifecycleHandlerType.INIT, InitHandler.class));
		builder.buildAndRegister();
	}
	
	static void createPreInstallAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.INIT).
			setEndState(LifecycleHandlerType.PRE_INSTALL).
			setErrorState(LifecycleHandlerType.INSTALL_FAILED).
			addTransitionAction(new DefaultLifecycleTransition<PreInstallHandler>(storeParam, ctxParam, LifecycleHandlerType.PRE_INSTALL, PreInstallHandler.class));
		builder.buildAndRegister();
	}
	
	static void createInstallAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.PRE_INSTALL).
		setEndState(LifecycleHandlerType.INSTALL).
		setErrorState(LifecycleHandlerType.INSTALL_FAILED).
		addTransitionAction(new DefaultLifecycleTransition<InstallHandler>(storeParam, ctxParam, LifecycleHandlerType.INSTALL, InstallHandler.class));
		builder.buildAndRegister();
	}
	
	static void createPostInstallAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.INSTALL).
		setEndState(LifecycleHandlerType.POST_INSTALL).
		setErrorState(LifecycleHandlerType.INSTALL_FAILED).
		addTransitionAction(new DefaultLifecycleTransition<PostInstallHandler>(storeParam, ctxParam, LifecycleHandlerType.POST_INSTALL, PostInstallHandler.class));
		builder.buildAndRegister();
	}
	
	static void createPreStartAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.POST_INSTALL).
		setEndState(LifecycleHandlerType.PRE_START).
		setErrorState(LifecycleHandlerType.STARTUP_FAILED).
		addTransitionAction(new DefaultLifecycleTransition<PreStartHandler>(storeParam, ctxParam, LifecycleHandlerType.PRE_START, PreStartHandler.class));
		builder.buildAndRegister();
	}
	
	static void createPostStartAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.START).
		setEndState(LifecycleHandlerType.POST_START).
		setErrorState(LifecycleHandlerType.STARTUP_FAILED).
		addTransitionAction(new DefaultLifecycleTransition<PostStartHandler>(storeParam, ctxParam, LifecycleHandlerType.POST_START, PostStartHandler.class));
		builder.buildAndRegister();
	}
	
	static void createPreStopAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.POST_START).
		setEndState(LifecycleHandlerType.PRE_STOP).
		setErrorState(LifecycleHandlerType.TERMINATION_FAILED).
		addTransitionAction(new DefaultLifecycleTransition<PreStopHandler>(storeParam, ctxParam, LifecycleHandlerType.PRE_STOP, PreStopHandler.class));
		builder.buildAndRegister();
	}
	
	static void createStopAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.PRE_STOP).
		setEndState(LifecycleHandlerType.STOP).
		setErrorState(LifecycleHandlerType.TERMINATION_FAILED).
		addTransitionAction(new DefaultLifecycleTransition<StopHandler>(storeParam, ctxParam, LifecycleHandlerType.STOP, StopHandler.class));
		builder.buildAndRegister();
	}
	
	static void createPostStopAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam) {
		builder.setStartState(LifecycleHandlerType.STOP).
		setEndState(LifecycleHandlerType.POST_STOP).
		setErrorState(LifecycleHandlerType.TERMINATION_FAILED).
		addTransitionAction(new DefaultLifecycleTransition<PostStopHandler>(storeParam, ctxParam, LifecycleHandlerType.POST_STOP, PostStopHandler.class));
		builder.buildAndRegister();
	}

	static void createSkipInstallAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder) {
		builder.setStartState(LifecycleHandlerType.INIT).
			setEndState(LifecycleHandlerType.INSTALL).
			addTransitionAction(EMPTY_TRANSITION_ACTION).
			buildAndRegister();
	}

	public static void createSkipStopAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder) {
		builder.setStartState(LifecycleHandlerType.POST_START).
			setEndState(LifecycleHandlerType.UNEXPECTED_EXECUTION_STOP).
			addTransitionAction(EMPTY_TRANSITION_ACTION).
			buildAndRegister();
	}
}
