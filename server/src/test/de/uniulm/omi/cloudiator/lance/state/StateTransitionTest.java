package de.uniulm.omi.cloudiator.lance.state;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareStateMachine;
import de.uniulm.omi.cloudiator.lance.util.state.ErrorAwareStateMachineBuilder;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionAction;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionErrorHandler;
import de.uniulm.omi.cloudiator.lance.util.state.TransitionException;

public class StateTransitionTest {

	private MyTransactionAction action;
	private MyErrorHandler errorHandler;
	
	// FIXME: what is missing is:
	// - the capability to find and collect pending exceptions.
	// - a mechanism to not loose the 
	
	@Before
	public void init() {
		action = new MyTransactionAction();
		errorHandler = new MyErrorHandler();
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionWithUnknownTransition() {
		ErrorAwareStateMachineBuilder<ContainerStatus> b = new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
		b.getTransitionBuilder().setStartState(ContainerStatus.NEW).
			// setIntermediateState(ContainerStatus.CREATING, true).
			setEndState(ContainerStatus.CREATED).
			// setErrorState(ContainerStatus.CREATION_FAILED).
			addTransitionAction(action).
			// addErrorHandler(new MyErrorHandler());
			buildAndRegister();
		ErrorAwareStateMachine<ContainerStatus> m = b.build();
		assertEquals(m.getState(), ContainerStatus.NEW);
		assertEquals(action.called, 0);
		m.transit(ContainerStatus.CREATED, ContainerStatus.NEW, null);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionWithWrongStartState() {
		ErrorAwareStateMachineBuilder<ContainerStatus> b = new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
		b.getTransitionBuilder().setStartState(ContainerStatus.CREATED).
			// setIntermediateState(ContainerStatus.CREATING, true).
			setEndState(ContainerStatus.BOOTSTRAPPED).
			// setErrorState(ContainerStatus.CREATION_FAILED).
			addTransitionAction(action).
			// addErrorHandler(new MyErrorHandler());
			buildAndRegister();
		ErrorAwareStateMachine<ContainerStatus> m = b.build();
		assertEquals(m.getState(), ContainerStatus.NEW);
		assertEquals(action.called, 0);
		m.transit(ContainerStatus.CREATED, ContainerStatus.BOOTSTRAPPED, null);
	}
		
	@Test
	public void testMinimalCorrectTransition() {
		ErrorAwareStateMachineBuilder<ContainerStatus> b = new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
		b.getTransitionBuilder().setStartState(ContainerStatus.NEW).
			// setIntermediateState(ContainerStatus.CREATING, true).
			setEndState(ContainerStatus.CREATED).
			// setErrorState(ContainerStatus.CREATION_FAILED).
			addTransitionAction(action).
			// addErrorHandler(new MyErrorHandler());
			buildAndRegister();
		ErrorAwareStateMachine<ContainerStatus> m = b.build();
		assertEquals(m.getState(), ContainerStatus.NEW);
		assertEquals(action.called, 0);
		m.transit(ContainerStatus.NEW, ContainerStatus.CREATED, null);
		assertEquals(m.getState(), ContainerStatus.CREATED);
		assertEquals(action.called, 1);
	}
	
	@Test
	public void testAsynchronousCorrectTransition() {
		action = new MyTransactionAction(1);
		
		ErrorAwareStateMachineBuilder<ContainerStatus> b = new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
		b.getTransitionBuilder().setStartState(ContainerStatus.NEW).
			setIntermediateState(ContainerStatus.CREATING, true).
			setEndState(ContainerStatus.CREATED).
			// setErrorState(ContainerStatus.CREATION_FAILED).
			addTransitionAction(action).
			// addErrorHandler(new MyErrorHandler());
			buildAndRegister();
		ErrorAwareStateMachine<ContainerStatus> m = b.build();
		assertEquals(m.getState(), ContainerStatus.NEW);
		assertEquals(action.called, 0);
		m.transit(ContainerStatus.NEW, ContainerStatus.CREATED, null);
		assertEquals(m.getState(), ContainerStatus.CREATING);
		action.decreaseLatch();
		ContainerStatus s = m.waitForEndOfCurrentTransition();
		assertEquals(action.called, 1);
		assertEquals(m.getState(), ContainerStatus.CREATED);
		assertEquals(m.getState(), s);
	}
	
	@Test
	public void testThrowingTransitionWithoutErrorHandler() {
		action = new MyTransactionAction(true);
		
		ErrorAwareStateMachineBuilder<ContainerStatus> b = new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
		b.getTransitionBuilder().setStartState(ContainerStatus.NEW).
			setIntermediateState(ContainerStatus.CREATING, false).
			setEndState(ContainerStatus.CREATED).
			// setErrorState(ContainerStatus.CREATION_FAILED).
			addTransitionAction(action).
			// addErrorHandler(new MyErrorHandler());
			buildAndRegister();
		ErrorAwareStateMachine<ContainerStatus> m = b.build();
		assertEquals(m.getState(), ContainerStatus.NEW);
		assertEquals(action.called, 0);
		m.transit(ContainerStatus.NEW, ContainerStatus.CREATED, null);
		assertEquals(action.called, 1);
		assertEquals(ContainerStatus.UNKNOWN, m.getState());
	}
	
	@Test
	public void testThrowingTransitionWithErrorState() {
		action = new MyTransactionAction(true);
		
		ErrorAwareStateMachineBuilder<ContainerStatus> b = new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
		b.getTransitionBuilder().setStartState(ContainerStatus.NEW).
			setIntermediateState(ContainerStatus.CREATING, false).
			setEndState(ContainerStatus.CREATED).
			setErrorState(ContainerStatus.CREATION_FAILED).
			addTransitionAction(action).
			// addErrorHandler(new MyErrorHandler());
			buildAndRegister();
		ErrorAwareStateMachine<ContainerStatus> m = b.build();
		assertEquals(m.getState(), ContainerStatus.NEW);
		assertEquals(action.called, 0);
		m.transit(ContainerStatus.NEW, ContainerStatus.CREATED, null);
		assertEquals(action.called, 1);
		assertEquals(ContainerStatus.CREATION_FAILED, m.getState());
	}
	
	@Test
	public void testThrowingTransitionWithErrorStateAndHandler() {
		action = new MyTransactionAction(true);
		errorHandler.thro = true;
		ErrorAwareStateMachineBuilder<ContainerStatus> b = new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
		b.getTransitionBuilder().setStartState(ContainerStatus.NEW).
			setIntermediateState(ContainerStatus.CREATING, false).
			setEndState(ContainerStatus.CREATED).
			setErrorState(ContainerStatus.CREATION_FAILED).
			addTransitionAction(action).
			addErrorHandler(errorHandler).
			buildAndRegister();
		ErrorAwareStateMachine<ContainerStatus> m = b.build();
		assertEquals(m.getState(), ContainerStatus.NEW);
		assertEquals(action.called, 0);
		m.transit(ContainerStatus.NEW, ContainerStatus.CREATED, null);
		assertEquals(action.called, 1);
		assertEquals(ContainerStatus.UNKNOWN, m.getState());
		assertEquals(errorHandler.called, 1);
	}
	
	
	@Test
	public void testThrowingTransitionWithBadBehaviour() {
		action = new MyBadTransactionAction();
		
		ErrorAwareStateMachineBuilder<ContainerStatus> b = new ErrorAwareStateMachineBuilder<>(ContainerStatus.NEW, ContainerStatus.UNKNOWN);
		b.getTransitionBuilder().setStartState(ContainerStatus.NEW).
			setIntermediateState(ContainerStatus.CREATING, false).
			setEndState(ContainerStatus.CREATED).
			// setErrorState(ContainerStatus.CREATION_FAILED).
			addTransitionAction(action).
			// addErrorHandler(new MyErrorHandler());
			buildAndRegister();
		ErrorAwareStateMachine<ContainerStatus> m = b.build();
		assertEquals(m.getState(), ContainerStatus.NEW);
		assertEquals(action.called, 0);
		m.transit(ContainerStatus.NEW, ContainerStatus.CREATED, null);
		assertEquals(action.called, 1);
		assertEquals(ContainerStatus.UNKNOWN, m.getState());
	}
	
	static class MyBadTransactionAction extends MyTransactionAction {

		MyBadTransactionAction(){
			super(0, false);
		}
		
		@Override
		public void transit(Object[] params) throws TransitionException {
			super.transit(params);
			throw new RuntimeException();
		}
	}
	
	static class MyTransactionAction implements TransitionAction {

		int called = 0;
		private volatile CountDownLatch latch;
		boolean thro;
		
		MyTransactionAction() {
			this(0, false);
		}
		
		MyTransactionAction(int latchCount) {
			this(latchCount, false);
		}
		
		MyTransactionAction(boolean throwException) {
			this(0, throwException);
		}
		
		MyTransactionAction(int latchCount, boolean throwException) {
			latch = new CountDownLatch(latchCount);
			thro = throwException;
		}
		
		@Override
		public void transit(Object[] params) throws TransitionException {
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			called++;
			if(thro)
				throw new TransitionException();
		}
		
		void decreaseLatch() {
			latch.countDown();
		}
	}
	
	static class MyErrorHandler implements TransitionErrorHandler<ContainerStatus> {

		int called = 0;
		boolean thro;
	
		@Override
		public void run(TransitionException te, ContainerStatus from, ContainerStatus to) {
			called++;
			if(thro)
				throw new RuntimeException();
		}
		
	}
}
