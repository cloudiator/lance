package de.uniulm.omi.cloudiator.lance.lifecycle;

import com.typesafe.config.Config;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StartHandler;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.util.configuration.Configuration;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StartTransitionAction implements TransitionAction {

  private static final Config config = Configuration.conf().getConfig("failfast");
  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultLifecycleTransition.class);

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
      final boolean failFast = config.getBoolean("failfast");

      if (failFast) {
        throw new TransitionException(lce);
      }

      LOGGER.warn(String.format("Commands of type: %s contained return values unequal to zero", LifecycleHandlerType.START), lce);
    }
  }
}
