package de.uniulm.omi.cloudiator.lance.util.state;

public class TransitionException extends Exception {

	public TransitionException() {
		super();
	}

	public TransitionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TransitionException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransitionException(String message) {
		super(message);
	}

	public TransitionException(Throwable cause) {
		super(cause);
	}
}
