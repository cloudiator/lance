package de.uniulm.omi.cloudiator.lance.util.state;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniulm.omi.cloudiator.lance.util.execution.LoggingThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public final class ErrorAwareStateMachine<T extends Enum<?> & State> {

    private static final Object[] DEFAULT_PARAMS = new Object[] {};


    static interface TransitionHandle<S extends Enum<?> & State> {

        S waitForTransitionEnd();
    }


    final static Logger LOGGER = LoggerFactory.getLogger(ErrorAwareStateMachine.class);

    // only one transition at a time.
    private final ExecutorService executor;
    private final Set<T> states = new HashSet<>();
    private final List<ErrorAwareStateTransition<T>> transitions;

    private final ErrorAwareStateMachineState<T> localState;

    private final Stack<Throwable> collectedExceptions = new Stack<>();

    ErrorAwareStateMachine(T init, T genericErrorState, List<T> states,
        List<ErrorAwareStateTransition<T>> st) {
        assert init != null : "init state cannot be null";

        final ThreadFactory threadFactory =
            new ThreadFactoryBuilder().setNameFormat("ErrorAwareStateMachine-%d").build();
        executor = new LoggingThreadPoolExecutor(1, threadFactory);

        localState =
            new ErrorAwareStateMachineState<>(init, genericErrorState, collectedExceptions);

        this.transitions = st;
        this.states.addAll(states);
    }

    /**
     * @param fromState the start state of the transition
     * @param toState   the target state of the transition
     * @param params    set of application specific parameters to be passed through to the
     *                  transition
     */
    public final void transit(T fromState, T toState, Object[] params) {
    	final Future<?> f;
    	final ErrorAwareStateTransition<T> transition; 
        synchronized (localState) {
            localState.cleanTransitionExceptions();
            transition = findTransition(fromState, toState);
            // validation and triggering has to happen automatically
            localState.validateReadyForTransition(transition);
            f = transition.triggerTransitionExecution(localState, params, executor);
        }
        // now, postprocess the trigger //
        transition.postprocessExecutionTrigger(f);
    }

    public final T getState() {
        LOGGER.debug("Resolving state. StateMachine: " + this + " local state: " + localState);
        synchronized (localState) {
            localState.cleanTransitionExceptions();
            return localState.getCurrentState();
        }
    }

    public final boolean assertCurrentState(T checkStatus) {
        // no synchronization needed here //
        T currentStatus = getState();
        return currentStatus == checkStatus;
    }

    public T waitForEndOfCurrentTransition() {
        TransitionHandle<T> handle = null;
        synchronized (localState) {
            localState.cleanTransitionExceptions();
            if (!localState.hasOngoingTransition()) {
                return localState.getCurrentState();
            }
            handle = localState.getTransitionHandle();
        }
        return handle.waitForTransitionEnd();
    }

    public void setStateToGenericError() {
      waitForEndOfCurrentTransition();

      synchronized (localState) {
        transit(localState.getCurrentState(),localState.getGenericErrorState());
      }
    }

    private ErrorAwareStateTransition<T> findTransition(T fromState, T toState) {

        ErrorAwareStateTransition<T> found = null;

        for (ErrorAwareStateTransition<T> t : transitions) {
            if (t.isStartState(fromState) && t.hasEndState(toState)) {
                if (found == null)
                    found = t;
                else {
                    LOGGER.error(
                        "multiple transitions with same start and end state found. picking any.");
                }
            }
        }

        if (found != null)
            return found;

        throw new IllegalStateException(
            "no transition found for startState: " + fromState + ": " + transitions);
    }

    public Throwable collectExceptions() {
        synchronized (localState) {
            if (collectedExceptions.isEmpty())
                throw new IllegalStateException(
                    "collectException shall only be called when something bad has actually happened.");

            Throwable ret = collectedExceptions.pop();
            while (!collectedExceptions.isEmpty()) {
                Throwable up = collectedExceptions.pop();
                ret = addAsLastCause(up, ret);
            }
            collectedExceptions.clear();
            return ret;
        }
    }

    private static Throwable addAsLastCause(Throwable t, Throwable cause) {
        assert t != null;
        Throwable x = t;
        while (x.getCause() != null)
            x = x.getCause();
        x.initCause(cause);
        return t;
    }

    public boolean isGenericErrorState(T stat) {
        return localState.isGenericErrorState(stat);
    }

    public void transit(T from, T to) {
        transit(from, to, DEFAULT_PARAMS);
    }
}
