package de.uniulm.omi.cloudiator.lance.lca;

public final class LcaException extends Exception {

    private static final long serialVersionUID = 1L;

    public LcaException() {
        super();
    }

    public LcaException(String message, Throwable cause) {
        super(message, cause);
    }

    public LcaException(String message) {
        super(message);
    }

    public LcaException(Throwable cause) {
        super(cause);
    }
}
