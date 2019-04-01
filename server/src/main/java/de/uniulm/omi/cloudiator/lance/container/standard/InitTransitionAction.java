package de.uniulm.omi.cloudiator.lance.container.standard;

import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.DYN_GROUP_KEY;
import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.DYN_HANDLER_KEY;

import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerConfigurationException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

final class InitTransitionAction implements TransitionAction {

	private final ErrorAwareContainer<?> theContainer;
	
	private InitTransitionAction(ErrorAwareContainer<?> container) {
		theContainer = container;
	}

	@Override
	public void transit(Object[] params) throws TransitionException {
        // FIXME: add code for starting from snapshot (skip init and install steps)
        // probably has to be realised at a different place
        try {
            theContainer.logic.preInit();
            theContainer.preInitAction();
            theContainer.postInitAction();
            theContainer.registerStatus(ContainerStatus.READY);
            initializeDynamicProperties();
        } catch (ContainerException | LifecycleException | RegistrationException ce) {
        	ErrorAwareContainer.getLogger().error("could not initialise container;", ce);
          throw new TransitionException(ce);
        }
    }

  private void initializeDynamicProperties() throws ContainerException, RegistrationException {
    AbstractComponent myComp = theContainer.logic.getComponent();
    if(myComp.isDynamicComponent()) {
      theContainer.registerKeyValPair(LcaRegistryConstants.regEntries.get(DYN_GROUP_KEY), myComp.getDynamicGroup());
    }
    if(myComp.isDynamicHandler()) {
      theContainer.registerKeyValPair(LcaRegistryConstants.regEntries.get(DYN_HANDLER_KEY), myComp.getDynamicHandler());
      theContainer.logic.doStartDynHandling(theContainer.getAccessor());
    }

  }

  public static void create(ErrorAwareTransitionBuilder<ContainerStatus> transitionBuilder,
			ErrorAwareContainer<?> container) {
		
		InitTransitionAction action = new InitTransitionAction(container);
		
		// FIXME: add error handler //
		transitionBuilder.setStartState(ContainerStatus.BOOTSTRAPPED).
						setIntermediateState(ContainerStatus.INITIALISING, false).
						setEndState(ContainerStatus.READY).
						setErrorState(ContainerStatus.INITIALISATION_FAILED).
						addTransitionAction(action);
		
		transitionBuilder.buildAndRegister();
	}

	public static boolean isKnownErrorState(ContainerStatus stat) {
		return stat == ContainerStatus.INITIALISATION_FAILED;
	}

	public static boolean isSuccessfullEndState(ContainerStatus stat) {
		return stat == ContainerStatus.READY;
	}
}
