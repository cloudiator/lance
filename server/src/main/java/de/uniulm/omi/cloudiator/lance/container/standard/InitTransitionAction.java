package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

final class InitTransitionAction implements TransitionAction {

	private final ErrorAwareContainer<?> theContainer;
	
	InitTransitionAction(ErrorAwareContainer<?> container) {
		theContainer = container;
	}

	@Override
	public void transit(Object[] params) throws TransitionException {
        //FIXME: add code for starting from snapshot (skip init and install steps)
		// has to be realised at a different place 
        try {
            theContainer.preInitAction();
            theContainer.logic.completeInit();
            theContainer.postInitAction();
            theContainer.registerStatus(ContainerStatus.READY);
        } catch (ContainerException | LifecycleException | RegistrationException ce) {
        	ErrorAwareContainer.getLogger().error("could not initialise container;", ce); 
        }
    }

	public static void create(ErrorAwareTransitionBuilder<ContainerStatus> transitionBuilder,
			ErrorAwareContainer<?> container) {
		
		CreateTransitionAction action = new CreateTransitionAction(container);
		
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
