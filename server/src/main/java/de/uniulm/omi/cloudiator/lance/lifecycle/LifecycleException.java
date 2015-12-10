package de.uniulm.omi.cloudiator.lance.lifecycle;

public class LifecycleException extends Exception {

	private static final long serialVersionUID = 1L;

	public LifecycleException() {
		super();
	}

	public LifecycleException(String message, Throwable cause) {
		super(message, cause);
	}

	public LifecycleException(String message) {
		super(message);
	}

	public LifecycleException(Throwable cause) {
		super(cause);
	}
}
