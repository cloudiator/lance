package de.uniulm.omi.cloudiator.lance.util.state;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ErrorAwareStateMachine<T extends Enum<?> & State > {

    private final static Logger LOGGER = LoggerFactory.getLogger(ErrorAwareStateMachine.class);
    private static final Object[] DEFAULT_PARAMS = new Object[0];
    
    private T status;

    private ErrorAwareStateTransition<T> ongoingTransition = null;
    private Future<?> endOfTransition = null;
    
    // only one transition at a time.
    private final ExecutorService executor = Executors.newFixedThreadPool(1);	
    private final T init;
    private final T genericErrorState;
    private final Set<T> states = new HashSet<>();
    private final List<ErrorAwareStateTransition<T>> transitions;
    
    private final TransitionState callback = new TransitionState();
    
    private final Stack<Throwable> collectedExceptions = new Stack<>();
	
	ErrorAwareStateMachine(T init, T genericErrorState, List<T> states, List<ErrorAwareStateTransition<T>> st) {
		assert init != null : "init state cannot be null";
		this.init = init;
		status = init;
		this.transitions = st;
		this. genericErrorState = genericErrorState;
		this.states.addAll(states);
	}
	
	/**
     * @param startState the start state of the transition 
     * @param params set of application specific parameters to be passed through to the
     * transition
     */
    public synchronized void transit(T fromState, T toState, Object[] params) {
    	ErrorAwareStateTransition<T> transition = findTransition(fromState, toState);
        if(transition.isIntermediateOrEndState(status)) 
            return ; // we are already done //
        if(!transition.isStartState(status)) 
            throw new IllegalStateException("we are in the wrong state: " + status);
        if(endOfTransition != null) 
            throw new IllegalStateException("we are in the wrong state: endOfTransition is set");
        
        // everything is fine. let's invoke the transition //
        ongoingTransition = transition;
        transition.executeTransition(callback, params, executor);
    }
	
    public synchronized T getState() {
        return status;
    }

    public synchronized boolean assertCurrentState(T checkStatus) {
        return status == checkStatus;
    }
    
    @SuppressWarnings("unchecked")
	public T waitForEndOfCurrentTransition() {
        Object[] o = checkIfTransitionDone();
       
        if(Boolean.TRUE.equals(o[0])) {
        	return (T) o[1]; 
        }
        
        waitLoop((Future<?>) o[1]);
        // NOTE: this is not entirely thread safe. it is possible that the next transition 
        // is triggered before the recursion and that we build up an infinite stack. Yet, 
        // this is unlikely in the envisioned usage scenario.
        return waitForEndOfCurrentTransition();
    }
    
    private Object[] checkIfTransitionDone() {
    	 synchronized(this) {
             if(status == null) {
                 throw new IllegalStateException("status not set");
             }

             // no ongoing transition; return immediately //
             if(ongoingTransition == null && endOfTransition == null)
             	return new Object[] {Boolean.TRUE, status};
             if((ongoingTransition == null && endOfTransition != null) ||
             		ongoingTransition != null && endOfTransition == null)
             	throw new IllegalStateException("inconsistent state between ongoing transaction and endOfTransaction");
                         
             if(endOfTransition == null) 
                 throw new IllegalStateException("no synchronisation entity");
             
          	return new Object[] {Boolean.FALSE, endOfTransition};
         }
    }
    
    private static void waitLoop(Future<?> f) {
        while(true) {
            if(f.isDone()) {
                return;
            }
            
            try { 
                f.get(); 
                return;
            } catch(InterruptedException ie){
                // we were interrupted; ignore and re-try
                 // FIXME: implement in a correct way
                LOGGER.error("interrupted", ie);
            } catch(CancellationException ce) {
                // task cancelled => state not reached
                // FIXME: set back status or set to error state
                // FIXME: revert changes
                throw new IllegalStateException(ce);
             } catch(ExecutionException ee){
                 // an exception occurred during execution => state not reached
                // FIXME: set back status or set to error state
                 // FIXME: revert changes
                throw new IllegalStateException(ee);
            }
        }
    }
    
    private ErrorAwareStateTransition<T> findTransition(T fromState, T toState) {
    	
    	ErrorAwareStateTransition<T> found = null;
    	
    	for(ErrorAwareStateTransition<T> t : transitions) {
    		if(t.isStartState(fromState) && t.hasEndState(toState)) {
    			if(found == null)
    				found = t;
    			else {
    				LOGGER.error("multiple transitions with same start and end state found. picking any.");
    			}
    		}
    	}
    	
    	if(found != null)
    		return found;
        
        throw new IllegalStateException("no transition found for startState: " + fromState + ": " + transitions);
    }
    
    class TransitionState implements ErrorAwareTransitionState<T> {

		@Override
		public void transitionStarted(T intermediate, Future<?> object) {
			synchronized(ErrorAwareStateMachine.this){
				status = intermediate;
				endOfTransition = object;
			}
		}

		@Override
		public void transitionComplete(T to, Throwable t) {
			synchronized(ErrorAwareStateMachine.this) {
				if(t == null && to == null) {
					moveToFutureException();
					return;
				}
				if(t != null && to == null)
					status = genericErrorState;
				else
					status = to;
				if(t != null) {
					collectedExceptions.push(t);
				}
	            endOfTransition = null;
	            ongoingTransition = null;
			}
		}
		
		private void moveToFutureException() {
			// this method is invoked when we face an exception
			// that is thrown but the system is not prepared for.
			// at that point, we even don't know what the exception
			// will be. it is sure, however, that the transition 
			// ends with this exception.
			
			// hence, we set ongoing transaction to null and move
			// into the generic error state
			ongoingTransition = null;
			status = genericErrorState;
			
			// but we keep endOfTransition where the exception
			// will eventually be stored.
			// endOfTransition = endOfTransition
		}
    }

	public synchronized Throwable collectExceptions() {
		if(collectedExceptions.isEmpty())
			throw new IllegalStateException("collectException shall only be called when something bad has actually happened.");
		
		Throwable ret = collectedExceptions.pop();
		while(!collectedExceptions.isEmpty()) {
			Throwable up = collectedExceptions.pop();
			ret = addAsLastCause(up, ret);
		}
		collectedExceptions.clear();
		return ret;
	}
	
	private Throwable addAsLastCause(Throwable t, Throwable cause) {
		assert t != null;
		Throwable x = t;
		while(x.getCause() != null)
			x = x.getCause();
		x.initCause(cause);
		return t;
	}
}
