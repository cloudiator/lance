package de.uniulm.omi.cloudiator.lance.lifecycles;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.HandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.Detector;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StartDetector;

public class TestLifecycleHandler implements InvocationHandler {

	public final HandlerType type;
	public volatile ExecutionContext ec = null; 
	public final boolean throwsException;
	
	public TestLifecycleHandler(HandlerType myType, boolean throwException) {
		type = myType;
		throwsException = throwException;
	}
	
	public TestLifecycleHandler(HandlerType myType) {
		this(myType, false);
	}
	
	public Object execute(ExecutionContext ec) {
		if(ec == null) 
			throw new NullPointerException();
		this.ec = ec;
		if(throwsException){
			throw new RuntimeException();
		}
		if(type == DetectorType.START) {
			return DetectorState.DETECTED;
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

	public boolean wasCalled() {
		return ec != null;
	}
}
