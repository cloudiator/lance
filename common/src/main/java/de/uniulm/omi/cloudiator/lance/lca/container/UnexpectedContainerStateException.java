package de.uniulm.omi.cloudiator.lance.lca.container;

public class UnexpectedContainerStateException extends ContainerException {

  public UnexpectedContainerStateException() {
    super();
  }

  public UnexpectedContainerStateException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnexpectedContainerStateException(String message) {
    super(message);
  }

  public UnexpectedContainerStateException(Throwable cause) {
    super(cause);
  }
}
