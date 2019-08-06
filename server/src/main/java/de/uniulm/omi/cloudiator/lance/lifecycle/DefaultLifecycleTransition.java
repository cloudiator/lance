package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.application.FailFastConfig;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultLifecycleTransition<T extends LifecycleHandler> implements TransitionAction {

  private static final boolean failFast = FailFastConfig.failFast;
  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultLifecycleTransition.class);

	private final LifecycleHandlerType handlerType;
	private final Class<T> handlerClass;
	
	private final LifecycleStore store;
	private final ExecutionContext ctx;
	
	DefaultLifecycleTransition(LifecycleStore storeParam, ExecutionContext ctxParam, 
										LifecycleHandlerType handlerTypeParam, Class<T> classParam) {
		handlerType = handlerTypeParam;
		handlerClass = classParam;
		
		store = storeParam;
		ctx = ctxParam;
	}
	
	@Override
	public void transit(Object[] params) throws TransitionException {
		T h = store.getHandler(handlerType, handlerClass);
    try { h.execute(ctx); }
    catch(LifecycleException lce) {

      if (failFast) {
        throw new TransitionException(lce);
      }

      LOGGER.warn(String.format("Commands of type: %s contained return values unequal to zero", handlerType), lce);
    }
	}
}
