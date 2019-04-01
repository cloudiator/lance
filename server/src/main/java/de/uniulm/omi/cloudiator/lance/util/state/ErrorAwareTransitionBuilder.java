package de.uniulm.omi.cloudiator.lance.util.state;

public final class ErrorAwareTransitionBuilder<T extends Enum<?> & State > {

	private final ErrorAwareStateMachineBuilder<T> builder;
	
    private T from;
    private T intermediate;
    private T to;
    private T error;
    private T genericError;
    private TransitionAction action;
    private TransitionErrorHandler errorHandler;
    private boolean isAsynchronous = false;
	
	ErrorAwareTransitionBuilder(ErrorAwareStateMachineBuilder<T> b, T genericError) {
		builder = b;
		this.genericError = genericError;
	}

	public ErrorAwareTransitionBuilder<T> setStartState(T start) {
		if(start == null) 
			throw new NullPointerException("start state cannot be null");
		if(this.from != null)
			throw new IllegalStateException("from state can only be set once");
		from = start;
		return this;
	}
	
	public ErrorAwareTransitionBuilder<T> setIntermediateState(T inter, boolean isAsynchronous) {
		if(inter == null) 
			throw new NullPointerException("when set, intermediate state cannot be null");
		if(this.intermediate != null)
			throw new IllegalStateException("intermediate state can only be set once");
		intermediate = inter;
		this.isAsynchronous = isAsynchronous;
		return this;
	}
	
	public ErrorAwareTransitionBuilder<T> setErrorState(T error) {
		if(error == null) 
			throw new NullPointerException("when set, error state cannot be null");
		if(this.error != null)
			throw new IllegalStateException("error state can only be set once");
		this.error = error;
		return this;
	}
	
	public ErrorAwareTransitionBuilder<T> setEndState(T end) {
		if(end == null) 
			throw new NullPointerException("end state cannot be null");
		if(to != null)
			throw new IllegalStateException("end state can only be set once");
		to = end;
		return this;
	}

	public ErrorAwareTransitionBuilder<T> addTransitionAction(TransitionAction action_) {
		if(action_ == null) 
			throw new NullPointerException("transition action cannot be null");
		if(action != null)
			throw new IllegalStateException("actio can only be set once");
		action = action_;
		return this;
	}

	public ErrorAwareTransitionBuilder<T> addErrorHandler(TransitionErrorHandler teh) {
		if(teh == null) 
			throw new NullPointerException("when set error handler must not be null");
		if(errorHandler != null)
			throw new IllegalStateException("error handler can only be set once");
		errorHandler = teh;
		return this;
	}
	
	public void buildAndRegister() {
	    if(from == null)
	    	throw new IllegalStateException("from state must be set");
	    if(to == null)
	    	throw new IllegalStateException("to state must be set");
	    if(action == null)
	    	throw new IllegalStateException("action must be set");
	    
	    if(errorHandler != null && error == null)
	    	throw new IllegalStateException("error handler can only be used with error state.");
	    
	    ErrorAwareStateTransition<T> t = new ErrorAwareStateTransition<>(from, intermediate, 
	    		to, error, action, isAsynchronous, errorHandler);
	    builder.addTransition(t);
	    //Add a generic error transition
      ErrorAwareStateTransition<T> tGeneric = new ErrorAwareStateTransition<>(from, intermediate,
          genericError, error, null, isAsynchronous, null);
      builder.addTransition(tGeneric);
	}
}
