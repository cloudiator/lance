package de.uniulm.omi.cloudiator.lance.state;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareStateMachineBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionErrorHandler;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;
import org.junit.Before;
import org.junit.Test;

public class StateTransitionCreationTest {

  private MyTransactionAction action;
  private TransitionErrorHandler errorHandler;

  @Before
  public void init() {
    action = new MyTransactionAction();
    errorHandler = new MyErrorHandler();
  }

  private ErrorAwareStateMachineBuilder<ContainerStatus> createStateMachineBuilder() {
    return new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
  }

  @Test(expected = IllegalStateException.class)
  public void testVoidStateTransitionCreation() {
    ErrorAwareStateMachineBuilder<ContainerStatus> b = createStateMachineBuilder();
    b.getTransitionBuilder().buildAndRegister();
  }

  @Test(expected = IllegalStateException.class)
  public void testStateTransitionCreation() {
    ErrorAwareStateMachineBuilder<ContainerStatus> b = createStateMachineBuilder();
    b.getTransitionBuilder().buildAndRegister();
  }

  @Test
  public void testMinimalCorrectTransitionCreation() {
    ErrorAwareStateMachineBuilder<ContainerStatus> b = createStateMachineBuilder();
    b.getTransitionBuilder()
        .setStartState(ContainerStatus.NEW)
        .
        // setIntermediateState(ContainerStatus.CREATING, true).
        setEndState(ContainerStatus.CREATED)
        .
        // setErrorState(ContainerStatus.CREATION_FAILED).
        addTransitionAction(action)
        .
        // addErrorHandler(new MyErrorHandler());
        buildAndRegister();
  }

  @Test
  public void testMaximalCorrectTransitionCreation() {
    ErrorAwareStateMachineBuilder<ContainerStatus> b = createStateMachineBuilder();
    b.getTransitionBuilder()
        .setStartState(ContainerStatus.NEW)
        .setIntermediateState(ContainerStatus.CREATING, true)
        .setEndState(ContainerStatus.CREATED)
        .setErrorState(ContainerStatus.CREATION_FAILED)
        .addTransitionAction(action)
        .addErrorHandler(new MyErrorHandler())
        .buildAndRegister();
  }

  @Test(expected = IllegalStateException.class)
  public void testErrorHandlerWithoutStateTransitionCreation() {
    ErrorAwareStateMachineBuilder<ContainerStatus> b = createStateMachineBuilder();
    b.getTransitionBuilder()
        .setStartState(ContainerStatus.NEW)
        .setIntermediateState(ContainerStatus.CREATING, true)
        .setEndState(ContainerStatus.CREATED)
        .
        // setErrorState(ContainerStatus.CREATION_FAILED).
        addTransitionAction(action)
        .addErrorHandler(errorHandler)
        .buildAndRegister();
  }

  @Test(expected = IllegalStateException.class)
  public void testTransitionCreationWithoutAction() {
    ErrorAwareStateMachineBuilder<ContainerStatus> b = createStateMachineBuilder();
    b.getTransitionBuilder()
        .setStartState(ContainerStatus.NEW)
        .
        // setIntermediateState(ContainerStatus.CREATING, true).
        setEndState(ContainerStatus.CREATED)
        .
        // setErrorState(ContainerStatus.CREATION_FAILED).
        // addTransitionAction(action).
        // addErrorHandler(new MyErrorHandler());
        buildAndRegister();
  }

  static class MyTransactionAction implements TransitionAction {

    @Override
    public void transit(Object[] params) {}
  }

  static class MyErrorHandler implements TransitionErrorHandler {

    @Override
    public void run(TransitionException te, Enum from, Enum to) {
      // TODO Auto-generated method stub

    }
  }
}
