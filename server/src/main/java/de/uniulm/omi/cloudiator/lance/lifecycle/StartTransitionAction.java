package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StartHandler;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

final class StartTransitionAction implements TransitionAction {
	
	private final LifecycleStore store;
	private final ExecutionContext ctx;
	private final LifecycleActionInterceptor interceptor;
	
	StartTransitionAction(LifecycleStore storeParam, ExecutionContext ctxParam, LifecycleActionInterceptor interceptorParam) {
	
		store = storeParam;
		ctx = ctxParam;
		interceptor = interceptorParam;
	}
	
	static void createStartAction(ErrorAwareTransitionBuilder<LifecycleHandlerType> builder, LifecycleStore storeParam, ExecutionContext ctxParam, LifecycleActionInterceptor interceptorParam) {
		builder.setStartState(LifecycleHandlerType.PRE_START).
		setEndState(LifecycleHandlerType.START).
		setErrorState(LifecycleHandlerType.STARTUP_FAILED).
		addTransitionAction(new StartTransitionAction(storeParam, ctxParam, interceptorParam));
		builder.buildAndRegister();
	}

	@Override
	public void transit(Object[] params) throws TransitionException {
		StartHandler startHandler = store.getHandler(LifecycleHandlerType.START, StartHandler.class);
        try { 
        	startHandler.execute(ctx);
        	StartDetectorHandler.runStartDetector(interceptor, store.getStartDetector(), ctx);
        } catch(LifecycleException lce) {
        	throw new TransitionException(lce);
        }

	}

}
