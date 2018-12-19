package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

final class DefaultLifecycleTransition<T extends LifecycleHandler> implements TransitionAction {

  private final LifecycleHandlerType handlerType;
  private final Class<T> handlerClass;

  private final LifecycleStore store;
  private final ExecutionContext ctx;

  DefaultLifecycleTransition(
      LifecycleStore storeParam,
      ExecutionContext ctxParam,
      LifecycleHandlerType handlerTypeParam,
      Class<T> classParam) {
    handlerType = handlerTypeParam;
    handlerClass = classParam;

    store = storeParam;
    ctx = ctxParam;
  }

  @Override
  public void transit(Object[] params) throws TransitionException {
    T h = store.getHandler(handlerType, handlerClass);
    try {
      h.execute(ctx);
    } catch (LifecycleException lce) {
      throw new TransitionException(lce);
    }
  }
}
