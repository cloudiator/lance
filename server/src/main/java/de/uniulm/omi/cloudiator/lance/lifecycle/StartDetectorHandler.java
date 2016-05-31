package de.uniulm.omi.cloudiator.lance.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StartDetector;

final class StartDetectorHandler {
    
	private static final Logger LOGGER = LoggerFactory.getLogger(StartDetectorHandler.class);
	
	/** waiting 5 minutes per default */
	private static final int MAXIMUM_START_WAIT_TIME = 900000;
	/** waiting 30 seconds per loop */
	private static final int START_LOOP_WAIT_TIME = 30000;
	
	static Logger getLogger() { 
        return LOGGER; 
    }
	
	private final LifecycleActionInterceptor interceptor;
	private final StartDetector detector;
	private final ExecutionContext ec;
	
    static void runStartDetector(LifecycleActionInterceptor interceptor, StartDetector detector, ExecutionContext ec) throws LifecycleException {
    	StartDetectorHandler handler = new  StartDetectorHandler(interceptor, detector, ec);
    	handler.doRun();
    }
    
    private void doRun() throws LifecycleException {
    	final long maxEndTime = System.currentTimeMillis() + MAXIMUM_START_WAIT_TIME;
    	while(true) {
    		if(maxEndTime < System.currentTimeMillis()) {
    			break;
    		}
    		DetectorState state = runStartDetectorLoop();
    		switch(state){
	    		case DETECTED: 
	    			return;
	    		case DETECTION_FAILED: 
	    			throw new LifecycleException();
	    		case NOT_DETECTED:
	    			 break;
	    		default:
	    			throw new IllegalStateException("state " + state + " not captured");
    		}
		getLogger().info("container not ready, sleeping.");
    		sleep();
    	}
    	throw new LifecycleException("application would not start. abort to wait.");
    }
    
    private static void sleep() {
    	try {
			Thread.sleep(START_LOOP_WAIT_TIME);
		} catch(InterruptedException ie) {
			//TODO: handle this.
			getLogger().warn("interrupted exception", ie);
		}
    }
    
    private DetectorState runStartDetectorLoop() {
    	boolean preprocessed = false;
    	 try {
		 getLogger().info("running start detector");
    		 interceptor.preprocessDetector(DetectorType.START);
    		 preprocessed = true;
    		 return detector.execute(ec);
    	 } catch (ContainerException ce) {
		 getLogger().warn("detection failed with exception", ce);
 			return DetectorState.DETECTION_FAILED;
 		} finally {
			if(preprocessed) {
		        interceptor.postprocessDetector(DetectorType.START);
			}
		}
    }
	
	private StartDetectorHandler(LifecycleActionInterceptor interceptorP, StartDetector detectorP, ExecutionContext ecP) {
		interceptor = interceptorP;
		detector = detectorP;
		ec = ecP;
	}
}
