package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.util.state.StateMachine;
import de.uniulm.omi.cloudiator.lance.util.state.StateMachineBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;

public class LifecycleControllerTransitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleControllerTransitions.class);

    static Logger getLogger() { 
    	return LOGGER; 
    }
    
    final LifecycleStore store;
    final ExecutionContext ec;
    
	private LifecycleControllerTransitions(LifecycleStore storeParam, ExecutionContext ecParam) {
		if(storeParam == null)
			throw new NullPointerException("store must be set.");
		store = storeParam;
		ec = ecParam;
	}
	
    static StateMachine<LifecycleHandlerType> buildStateMachine(LifecycleStore storeParam, ExecutionContext ecParam) {
    	LifecycleControllerTransitions transitions = new LifecycleControllerTransitions(storeParam, ecParam);
        return transitions.addInitTransition(
        		transitions.addInstallTransitions(
        				transitions.addStartTransitions(
                                transitions.addStopTransitions(
                                new  StateMachineBuilder<>(LifecycleHandlerType.NEW).
                                addAllState(LifecycleHandlerType.values())

                                )))).build();
    }
    
    private StateMachineBuilder<LifecycleHandlerType> addInitTransition(StateMachineBuilder<LifecycleHandlerType> b) {
        return b.addSynchronousTransition(LifecycleHandlerType.NEW, LifecycleHandlerType.INIT,
                new TransitionAction() {
                    @Override public void transit(Object[] params) {
                        InitHandler h = store.getHandler(LifecycleHandlerType.INIT, InitHandler.class);
                        try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                    }
                });
    }
    
    private StateMachineBuilder<LifecycleHandlerType> addInstallTransitions(StateMachineBuilder<LifecycleHandlerType> b) {
        return b.addSynchronousTransition(LifecycleHandlerType.INIT, LifecycleHandlerType.PRE_INSTALL,
                new TransitionAction() {
                    @Override public void transit(Object[] params) {
                        PreInstallHandler h = store.getHandler(LifecycleHandlerType.PRE_INSTALL, PreInstallHandler.class);
                        try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                    }
                }).
                addSynchronousTransition(LifecycleHandlerType.PRE_INSTALL, LifecycleHandlerType.INSTALL, 
                    new TransitionAction() {
                        @Override public void transit(Object[] params) {
                            InstallHandler h = store.getHandler(LifecycleHandlerType.INSTALL, InstallHandler.class);
                            try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                        }
                }). 
                addSynchronousTransition(LifecycleHandlerType.INSTALL, LifecycleHandlerType.POST_INSTALL, 
                    new TransitionAction() {
                        @Override public void transit(Object[] params) {
                            PostInstallHandler h = store.getHandler(LifecycleHandlerType.POST_INSTALL, PostInstallHandler.class);
                            try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                        }
                });        
    }
    
    private StateMachineBuilder<LifecycleHandlerType> addStartTransitions(StateMachineBuilder<LifecycleHandlerType> b) {
        return b.addSynchronousTransition(LifecycleHandlerType.POST_INSTALL, LifecycleHandlerType.PRE_START, 
                new TransitionAction() {
                    @Override public void transit(Object[] params) {
                            PreStartHandler h = store.getHandler(LifecycleHandlerType.PRE_START, PreStartHandler.class);
                            try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                    }
                }).
                addSynchronousTransition(LifecycleHandlerType.PRE_START, LifecycleHandlerType.START,
                    new TransitionAction() {
                        @Override public void transit(Object[] params) {
                            StartHandler h = store.getHandler(LifecycleHandlerType.START, StartHandler.class);
                            try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                        }
                }). 
                addSynchronousTransition(LifecycleHandlerType.START, LifecycleHandlerType.POST_START,
                    new TransitionAction() {
                        @Override public void transit(Object[] params) {
                            PostStartHandler h = store.getHandler(LifecycleHandlerType.POST_START, PostStartHandler.class);
                            try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                    }
                });
    }

    private StateMachineBuilder<LifecycleHandlerType> addStopTransitions(StateMachineBuilder<LifecycleHandlerType> b) {
        return b.addSynchronousTransition(LifecycleHandlerType.POST_START, LifecycleHandlerType.PRE_STOP,
                new TransitionAction() {
                    @Override public void transit(Object[] params) {
                        PreStopHandler h = store.getHandler(LifecycleHandlerType.PRE_STOP, PreStopHandler.class);
                        try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                    }
                }).
                addSynchronousTransition(LifecycleHandlerType.PRE_STOP, LifecycleHandlerType.STOP,
                        new TransitionAction() {
                            @Override public void transit(Object[] params) {
                                StopHandler h = store.getHandler(LifecycleHandlerType.STOP, StopHandler.class);
                                try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                            }
                        }).
                addSynchronousTransition(LifecycleHandlerType.STOP, LifecycleHandlerType.POST_STOP,
                        new TransitionAction() {
                            @Override public void transit(Object[] params) {
                                PostStopHandler h = store.getHandler(LifecycleHandlerType.POST_STOP, PostStopHandler.class);
                                try { h.execute(ec); } catch(LifecycleException lce) {throw new RuntimeException(lce);}
                            }
                        });
    }
}
