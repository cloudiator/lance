package de.uniulm.omi.cloudiator.lance.container.standard;

import java.util.Arrays;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareTransitionBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

final class CreateTransitionAction implements TransitionAction {

	private final ErrorAwareContainer<?> theContainer;
	
	CreateTransitionAction(ErrorAwareContainer<?> container) {
		theContainer = container;
	}

	@Override
	public void transit(Object[] params) throws TransitionException {
		try {
			theContainer.preCreateAction();
			checkForCreationParameters(params);
			theContainer.logic.doCreate();
			theContainer.postCreateAction();
			theContainer.registerStatus(ContainerStatus.CREATED);
        } catch (ContainerException | RegistrationException ce) {
            throw new TransitionException(ce); 
        }
	}
	
    private static void checkForCreationParameters(Object[] o){
        //if(o == null || o.length == 0 || o.length > 1 || !(o[0] instanceof OperatingSystem)) throw new IllegalArgumentException(Arrays.toString(o));
        if(o == null || o.length > 0) 
            throw new IllegalArgumentException(Arrays.toString(o));
        // return (OperatingSystem) o[0];
        return;
    }
    
	public static void create(ErrorAwareTransitionBuilder<ContainerStatus> transitionBuilder,
			ErrorAwareContainer<?> container) {
		
		CreateTransitionAction action = new CreateTransitionAction(container);
		
		// FIXME: add error handling //
		transitionBuilder.setStartState(ContainerStatus.NEW).
						setIntermediateState(ContainerStatus.CREATING, false).
						setEndState(ContainerStatus.CREATED).
						setErrorState(ContainerStatus.CREATION_FAILED).
						addTransitionAction(action);
		
		transitionBuilder.buildAndRegister();
	}

	public static boolean isSuccessfullEndState(ContainerStatus stat) {
		return stat == ContainerStatus.CREATED;
	}
	
	public static boolean isKnownErrorState(ContainerStatus stat) {
		return stat == ContainerStatus.CREATION_FAILED;
	}
}
