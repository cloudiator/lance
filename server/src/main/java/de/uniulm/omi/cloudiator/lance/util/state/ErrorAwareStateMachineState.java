package de.uniulm.omi.cloudiator.lance.util.state;

import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareStateMachine.TransitionHandle;
import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @param <T> the enum representing the state
 * @author Jörg Domaschka
 *     <p>objects of this class expect that all accesses to their non-public methods have been
 *     synchronized via this.
 */
final class ErrorAwareStateMachineState<T extends Enum<?> & State>
    implements ErrorAwareTransitionState<T> {

  private final T genericErrorState;
  private final Stack<Throwable> exceptions;
  private T status;
  private ErrorAwareStateTransition<T> ongoingTransition = null;
  private Future<?> endOfTransition = null;

  ErrorAwareStateMachineState(
      T initState, T genericErrorStateParam, Stack<Throwable> exceptionStore) {
    status = initState;
    genericErrorState = genericErrorStateParam;
    exceptions = exceptionStore;
  }

  @Override
  public void transitionStarted(T intermediate, Future<?> object) {
    synchronized (this) {
      status = intermediate;
      endOfTransition = object;
    }
  }

  @Override
  public void transitionComplete(T to, TransitionException t) {
    synchronized (this) {
      if (isFutureUnexpectedException(to, t)) {
        moveToFutureUnexpectedException();
        return;
      }
      storeExceptionAndSetState(to, t);

      clearEndOfTransition();
      clearOngoingTransition();
    }
  }

  void validateReadyForTransition(ErrorAwareStateTransition<T> transition) {
    if (transition.isIntermediateOrEndState(status)) return; // we are already done //
    if (!transition.isStartState(status))
      throw new IllegalStateException("we are in the wrong state: " + status);
    if (ongoingTransition != null)
      throw new IllegalStateException("another transition is currently ongoing");
    if (endOfTransition != null) {
      throw new IllegalStateException("result of previous transition has not been collected");
    }
    // everything is fine. let's mark the transition //
    ongoingTransition = transition;
  }

  void cleanTransitionExceptions() {
    ErrorAwareStateMachine.LOGGER.debug("Cleaning transition exceptions of " + this);
    if (hasUnclearedState()) {
      TransitionHandle<T> h = getTransitionHandle();
      h.waitForTransitionEnd();
    }
    ErrorAwareStateMachine.LOGGER.debug("Finished cleaning transition exceptions of " + this);
  }

  boolean hasOngoingTransition() {
    return hasOngoingTransition(true);
  }

  /**
   * @param enforceClearance if intermediate (i.e. uncleared) states are allowed
   * @return whether or not there are ongoing transitions
   */
  private boolean hasOngoingTransition(boolean enforceClearance) {
    // no ongoing transition; return immediately //
    if (ongoingTransition == null && endOfTransition == null) return false;
    if (enforceClearance
        && ((ongoingTransition == null && endOfTransition != null)
            || (ongoingTransition != null && endOfTransition == null)))
      throw new IllegalStateException(
          "inconsistent state between ongoing transaction and endOfTransaction");

    return true;
  }

  T getCurrentState() {
    ErrorAwareStateMachine.LOGGER.debug(String.format("Current state of %s is %s", this, status));
    return status;
  }

  TransitionHandle<T> getTransitionHandle() {
    if (!hasOngoingTransition(false)) {
      throw new IllegalStateException(
          "transition handle only available when transitions are ongoing.");
    }
    final Future<?> f = endOfTransition;
    return new TransitionHandle<T>() {

      @Override
      public T waitForTransitionEnd() {
        return ErrorAwareStateMachineState.this.waitForFuture(f);
      }
    };
  }

  private void moveToFutureUnexpectedException() {
    // this method is invoked when we face an exception
    // that is thrown but the system is not prepared for.
    // at that point, we even don't know what the exception
    // will be. it is sure, however, that the transition
    // ends with this exception.

    // hence, we set ongoing transaction to null and move
    // into the generic error state
    clearOngoingTransition();
    status = genericErrorState;

    // but we keep endOfTransition where the exception
    // will eventually be stored.
    // endOfTransition = endOfTransition
    // this also serves as a marker for clearance
  }

  private void storeExceptionAndSetState(T to, Throwable t) {
    if (t != null) {
      exceptions.push(t);
      if (to == null) {
        status = genericErrorState;
      }
    }
    status = to;
  }

  private boolean isFutureUnexpectedException(T to, Throwable t) {
    return t == null && to == null;
  }

  private void clearOngoingTransition() {
    if (ongoingTransition == null) throw new IllegalStateException();
    ongoingTransition = null;
  }

  private void clearEndOfTransition() {
    if (endOfTransition == null) throw new IllegalStateException();
    endOfTransition = null;
  }

  private T waitForFuture(Future<?> f) {
    // while(true) {
    try {
      f.get();
    } catch (InterruptedException ie) {
      // TODO: implement properly, when needed:
      // (i.e. anyone interrupts a thread to stop something)
      // currently, just ignore and re-try
      ErrorAwareStateMachine.LOGGER.error("interrupted", ie);
    } catch (CancellationException ce) {
      // TODO: implement properly, when needed:
      // (i.e. anyone stops the execution)
      ErrorAwareStateMachine.LOGGER.error("cancelled", ce);
      throw new IllegalStateException(ce);
    } catch (ExecutionException ee) {
      ErrorAwareStateMachine.LOGGER.error("execution exception", ee);
      // this branch means, that there was an exception in execution
      // i.e. the code ran, but failed uncaught.
      handleExecutionException(f, ee);
    }
    return getCurrentState();
    // }
  }

  private synchronized void handleExecutionException(Future<?> thisFuture, ExecutionException ee) {
    // we not necessarily find an uncleared state as there is a small chance
    // that clearance happened already. in that case, someone else may already
    // have reported on the outcome of this future.
    final boolean b = hasUnclearedState();
    if (b && endOfTransition == thisFuture) {
      // now register this exeception while keeping the status
      storeExceptionAndSetState(status, ee.getCause());
      clearEndOfTransition();
    } else if (!b) {
      // all fine. someone else took care already // do nothing //
    } else {
      ErrorAwareStateMachine.LOGGER.error("found uncleaerd state, but wrong Future");
    }
  }

  private boolean hasUnclearedState() {
    if (ongoingTransition == null && endOfTransition != null && status != genericErrorState) {
      throw new IllegalStateException();
    }
    return ongoingTransition == null && endOfTransition != null && status == genericErrorState;
  }

  boolean isGenericErrorState(T stat) {
    return genericErrorState == stat;
  }
}
