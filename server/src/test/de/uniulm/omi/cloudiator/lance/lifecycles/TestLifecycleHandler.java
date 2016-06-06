package de.uniulm.omi.cloudiator.lance.lifecycles;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Stack;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.HandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;

public class TestLifecycleHandler implements InvocationHandler {

	public final HandlerType type;
	public volatile Stack<ExecutionContext> ec = new Stack<>(); 
	public final boolean throwsException;
	public final int count;
	
	public TestLifecycleHandler(HandlerType myType, boolean throwException) {
		type = myType;
		throwsException = throwException;
		count = 0;
	}
	
	public TestLifecycleHandler(LifecycleHandlerType myType) {
		this(myType, false);
	}
	
	public TestLifecycleHandler(DetectorType start, int i, boolean throwException) {
		type = start;
		throwsException = throwException;
		count = i;
	}

	public Object execute(ExecutionContext ec) throws LifecycleException {
		if(ec == null) 
			throw new NullPointerException();
		this.ec.push(ec);
		if(throwsException){
			throw new LifecycleException("cold not execute command 'ls' in " + type + " handler.");
		}
		if(type == DetectorType.START) {
			if(this.ec.size() == count)
				return DetectorState.DETECTED;
			if(this.ec.size() < count)
				return DetectorState.NOT_DETECTED;
			throw new IllegalStateException();
		}
		return null;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if("execute".equals(method.getName()) && args.length == 1) {
			return execute((ExecutionContext) args[0]);
		}
		throw new IllegalArgumentException(method.toString());
	}

	public boolean wasCalled(int i) {
		return ec.size() == i;
	}
}
