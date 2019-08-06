package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.application.FailFastConfig;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StopDetector;

final class StopDetectorHandler implements Runnable {
    
	private static final Logger LOGGER = LoggerFactory.getLogger(StopDetectorHandler.class);
  private static final boolean failFast = FailFastConfig.failFast;

	// /** waiting 5 minutes per default */
	private static final int MAXIMUM_STOP_WAIT_TIME = 900000;
	// /** waiting 30 seconds per loop */
	private static final int STOP_LOOP_WAIT_TIME = 30000;
	
	static Logger getLogger() { 
        return LOGGER; 
    }
	
	private final LifecycleActionInterceptor interceptor;
	private final StopDetector detector;
	private final ExecutionContext ec;
	private final StopDetectorCallback callback;
	
	private ScheduledFuture<?> future = null;
	
    static StopDetectorHandler create(LifecycleActionInterceptor interceptor, StopDetector detector, ExecutionContext ec, StopDetectorCallback callbackParam) {
    	StopDetectorHandler handler = new StopDetectorHandler(interceptor, detector, ec, callbackParam);
    	return handler;
    }
   
	@Override
	public void run() {
		try {
			doRun();
		} catch(Exception ex) {
			callback.exceptionOccurred(ex);
		}
	}
    
	/**
	 * 
	 * @param callCallback
	 * @return
	 * @throws LifecycleException
	 */
    private void doRun() {
      DetectorState state = runStopDetector();
      callback.state(state);
    }
    
    private DetectorState runDetector() throws LifecycleException {
    	if(detector != null) {
        return detector.execute(ec);
      } else {
    		getLogger().info("no stop detector set. assuming that application has not stopped.");
        return DetectorState.NOT_DETECTED;
    	}
    }
    
    private DetectorState runStopDetector() {
      boolean preprocessed = false;
      try {
    		 getLogger().info("running stop detector");
    		 interceptor.preprocessDetector(DetectorType.STOP);
    		 preprocessed = true;
    		 return runDetector();
      } catch (ContainerException ce) {
         getLogger().warn("detection failed with exception", ce);
        return DetectorState.DETECTION_FAILED;
      } catch (LifecycleException e) {

        if (failFast) {
          LOGGER.error(String.format("Commands of type: %s contained return values unequal to zero", detector), e);
          return DetectorState.DETECTION_FAILED;
        }

        return DetectorState.DETECTED;
      } finally {
        if(preprocessed) {
          try {
            interceptor.postprocessDetector(DetectorType.STOP);
          } catch (ContainerException ce) {
            // FIXME: what shall we do here? easiest is to disallow
            // exceptions in postprocessing ...
            getLogger().warn("error when postprocessing stop detection", ce);
            throw new IllegalStateException("wrong state: should be in error state?", ce);
          }
        }
      }
    }
	
	private StopDetectorHandler(LifecycleActionInterceptor interceptorP, StopDetector detector2, ExecutionContext ecP, StopDetectorCallback callbackParam) {
		interceptor = interceptorP;
		detector = detector2;
		ec = ecP;
		callback = callbackParam;
	}
	
	private static void sleep() {
    	try {
			Thread.sleep(STOP_LOOP_WAIT_TIME);
		} catch(InterruptedException ie) {
			//TODO: handle this?
			getLogger().warn("interrupted exception", ie);
		}
    }

	void waitForFinalShutdown() throws LifecycleException {
		if(detector == null) { 
			return;
		}
		
		final long maxEndTime = System.currentTimeMillis() + MAXIMUM_STOP_WAIT_TIME;
		while(true) {
			if(maxEndTime < System.currentTimeMillis()) {
				break;
			}
			DetectorState state = runStopDetector();
			switch(state){
	    		case DETECTED: 
	    			return;
	    		case DETECTION_FAILED: 
	    			throw new LifecycleException("stop detection failed. aborting.");
	    		case NOT_DETECTED:
	    			 break;
	    		default:
	    			throw new IllegalStateException("state " + state + " not captured");
			}
			sleep();
		}
		throw new LifecycleException("application stop cannot be detected. aborting waiting.");
	}

	synchronized void scheduleDetection(HostContext hostContext) {
		ScheduledFuture<?> f = hostContext.scheduleAction(this);
		future = f;
	}

	synchronized void clearSchedule() {
		if(future == null) 
			return;
		if(future.isCancelled())
			return;
		if(future.isDone())
			return;
		future.cancel(false);
	}
}
