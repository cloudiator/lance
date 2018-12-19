package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;
import java.util.Arrays;

final class BootstrapTransitionAction implements TransitionAction {

  private final ErrorAwareContainer<?> theContainer;

  private BootstrapTransitionAction(ErrorAwareContainer<?> container) {
    theContainer = container;
  }

  static void checkForBootstrapParameters(Object[] o) {
    // if(o == null || o.length == 0 || o.length > 1 || !(o[0] instanceof OperatingSystem)) throw
    // new IllegalArgumentException(Arrays.toString(o));
    if (o == null || o.length > 0) throw new IllegalArgumentException(Arrays.toString(o));
    // return (OperatingSystem) o[0];
    return;
  }

  public static void create(
      ErrorAwareTransitionBuilder<ContainerStatus> transitionBuilder,
      ErrorAwareContainer<?> container) {

    BootstrapTransitionAction action = new BootstrapTransitionAction(container);

    // FIXME: add error handler //
    transitionBuilder
        .setStartState(ContainerStatus.CREATED)
        .setIntermediateState(ContainerStatus.BOOTSTRAPPING, false)
        .setEndState(ContainerStatus.BOOTSTRAPPED)
        .setErrorState(ContainerStatus.BOOTSTRAPPING_FAILED)
        .addTransitionAction(action);

    transitionBuilder.buildAndRegister();
  }

  public static boolean isSuccessfullEndState(ContainerStatus stat) {
    return stat == ContainerStatus.BOOTSTRAPPED;
  }

  public static boolean isKnownErrorState(ContainerStatus stat) {
    return stat == ContainerStatus.BOOTSTRAPPING_FAILED;
  }

  @Override
  public void transit(Object[] params) throws TransitionException {
    try {
      checkForBootstrapParameters(params);
      theContainer.logic.doInit(null);
      theContainer.postBootstrapAction();
      theContainer.registerStatus(ContainerStatus.BOOTSTRAPPED);
    } catch (ContainerException | RegistrationException ce) {
      ErrorAwareContainer.getLogger().error("could not initialise container", ce);
      throw new TransitionException(ce);
    }
  }
}
