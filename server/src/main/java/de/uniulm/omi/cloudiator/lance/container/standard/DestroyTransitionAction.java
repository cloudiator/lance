package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerConfigurationException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

final class DestroyTransitionAction implements TransitionAction {

  private final static int synchronPollTime = 10;
  private final static int synchronTimeOut = 2000;
  private final ErrorAwareContainer<?> theContainer;

  private DestroyTransitionAction(ErrorAwareContainer<?> container) {
    theContainer = container;
  }

  @Override
  public void transit(Object[] params) throws TransitionException {
		theContainer.network.stopPortUpdaters();
    try {
      boolean forceShutdown = false;
      try {
        theContainer.preDestroyAction();
      } catch (Exception ex) {
        ErrorAwareContainer.getLogger().error("could not shut down component; trying to force shut down of container", ex);
        forceShutdown = true;
      }
      theContainer.logic.preDestroy();
      waitForDynamicSynchonisation();
      stopDynamicHandlerThread();
      theContainer.logic.doDestroy(forceShutdown, theContainer.shouldBeRemoved());
      theContainer.registerStatus(ContainerStatus.DESTROYED);
    } catch (ContainerException | RegistrationException ce) {
      ErrorAwareContainer.getLogger().error("could not shut down container;", ce);
      throw new TransitionException(ce);
    } finally {
      try {
        theContainer.setNetworking();
      } catch (ContainerException e) {
        ErrorAwareContainer.getLogger().error("could not update networking", e);
      }
      try {
        theContainer.network.publishLocalData(theContainer.containerId);
      } catch (ContainerException e) {
        ErrorAwareContainer.getLogger().error("could not publish local data", e);
      }
      try {
        theContainer.logic.completeShutDown();
      } catch (ContainerException e) {
        ErrorAwareContainer.getLogger().error("could not shutdown shell;", e);
      }
    }
  }

  /* This function waits for the dynamic handler to realize and take action of the state stopped.
  After the handler finished, he sets the COMPONENT_INSTANCE back to PRE_STOP and the waiting ends.
  To stay consistent, this function eventually sets the state back to STOP
   */
  //todo: better "threading this" via ExecutorService, because of possible failure if reg access in while
  //loop takes a lot of time
  private void waitForDynamicSynchonisation() throws RegistrationException {
    AbstractComponent myComp = theContainer.logic.getComponent();
    if(!myComp.isDynamicComponent()) {
      return;
  }

    long startTime = System.currentTimeMillis();
    while((System.currentTimeMillis()-startTime) < synchronTimeOut) {
      try {
        String cInstanceStatus = theContainer.readValFromRegistry(
            Identifiers.COMPONENT_INSTANCE_STATUS);

        //Sync-point here
        if(cInstanceStatus.equals(LifecycleHandlerType.PRE_STOP.name())) {
          theContainer.registerKeyValPair(LcaRegistryConstants.regEntries.get(
              Identifiers.COMPONENT_INSTANCE_STATUS), LifecycleHandlerType.STOP.name());
          return;
        }

        Thread.sleep(synchronPollTime);
      } catch (ContainerConfigurationException e) {
        throw new RegistrationException(String
            .format("Cannot read key: %s from registry for Component Instance %s.", LcaRegistryConstants.regEntries
                .get(Identifiers.COMPONENT_INSTANCE_STATUS), theContainer.containerId) , e);
      } catch (InterruptedException e) {
        throw new RegistrationException(String
            .format("Component Instance: %s got interrupted while waiting for its Component_Instance_Stateto get synchronized.",
                theContainer.containerId) , e);
      }
    }

    ErrorAwareContainer.getLogger().error(String
        .format("Timeout in Dynamic Component Instance: %s, while waiting for Dynamic Handler Instance to synchronize"
            + "the DELETION State.", theContainer.containerId));
  }

  private void stopDynamicHandlerThread() {
    AbstractComponent myComp = theContainer.logic.getComponent();
    if(!myComp.isDynamicHandler()) {
      return;
    }

    try {
      theContainer.logic.doStopDynHandling();
    } catch (ContainerException e) {
      ErrorAwareContainer.getLogger().error(String
          .format("Error in shutting down the worker thread of the Dynamic Handler: %s", theContainer.containerId));
    }
  }

  static void create(ErrorAwareTransitionBuilder<ContainerStatus> transitionBuilder, ErrorAwareContainer<?> container) {
		
    DestroyTransitionAction action = new DestroyTransitionAction(container);
    // FIXME: add error handler //
    transitionBuilder.setStartState(ContainerStatus.READY).
    setIntermediateState(ContainerStatus.SHUTTING_DOWN, false).
    setEndState(ContainerStatus.DESTROYED).
    setErrorState(ContainerStatus.UNKNOWN).
    addTransitionAction(action);

    transitionBuilder.buildAndRegister();
  }

	public static boolean isKnownErrorState(ContainerStatus stat) {
		return stat == ContainerStatus.UNKNOWN;
	}

	public static boolean isSuccessfullEndState(ContainerStatus stat) {
		return stat == ContainerStatus.DESTROYED;
	}
}
