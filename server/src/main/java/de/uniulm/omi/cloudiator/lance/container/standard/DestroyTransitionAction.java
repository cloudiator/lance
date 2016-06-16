package de.uniulm.omi.cloudiator.lance.container.standard;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

final class DestroyTransitionAction implements TransitionAction {

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
            theContainer.logic.doDestroy(forceShutdown);
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
        }
    }

	static void create(ErrorAwareTransitionBuilder<ContainerStatus> transitionBuilder,
			ErrorAwareContainer<?> container) {
		
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
