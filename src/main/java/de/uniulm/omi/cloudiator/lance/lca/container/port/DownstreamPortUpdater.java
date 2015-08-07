package de.uniulm.omi.cloudiator.lance.lca.container.port;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;

final class DownstreamPortUpdater implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);
	
	private final OutPortHandler outPorts;
	private final PortRegistryTranslator portAccessor;
	private final PortHierarchy portHierarchy;
	private final LifecycleController controller;
	
	DownstreamPortUpdater(OutPortHandler outPortParams, PortRegistryTranslator portAccessorParam, 
				PortHierarchy portHierarchyParam, LifecycleController controllerParam) {
		outPorts = outPortParams;
		portAccessor = portAccessorParam;
		portHierarchy = portHierarchyParam;
		controller = controllerParam;
	}
	
	@Deprecated
    public void handleUpdate(OutPort port, PortDiff<?> diff) {
    /*    //FIXME: ensure that we are in running state 
        // if(!controller.isRunning()) return;
        
        // updating is rather easy. step 1: we get the update handler 
        // for this port from the deployable component and then either
        // do nothing or restart the application 
        try {
            DockerShell dshell = client.getSideShell(myId);
            flushEnvironmentVariables(dshell);
            flushSinglePort(dshell, port, diff.getCurrentSinkSet());
            shellFactory.installDockerShell(dshell);
            PortUpdateHandler handler = port.getUpdateHandler();
            controller.blockingUpdatePorts(handler);
        } catch(DockerException de) {
            logger.info("problem when accessing registry", de); 
        } catch (RegistrationException e) {
            logger.info("problem when accessing registry", e); 
        } finally {
            shellFactory.closeShell();
        }
        //FIXME: only *now* update the set in the OutPortState
        System.out.println("update the set in the OutPortState => ..."); //.printStackTrace();
        */
    }

    private void doRun() throws RegistrationException {
    	List<PortDiff<DownstreamAddress>> diffs = outPorts.updateDownstreamPorts(portAccessor, portHierarchy);
        if(! outPorts.requiredDownstreamPortsSet()) {
        	LOGGER.error("not all downstream ports are available. this may cause some issues");
        	// FIXME: what should happen is that we return to INSTALL state //
        	return;
        }
        if(diffs.isEmpty()) {
        	return;
        }
        
        // now that we got all changed ports, for each of them
        // call the port update handler
        for(PortDiff<DownstreamAddress> diff : diffs) {
        	OutPort port = diff.getPort();
        	PortUpdateHandler handler = port.getUpdateHandler();
        	controller.blockingUpdatePorts(port, handler, diff);
            // FIXME: only *now* update the set in the OutPortState, 
        	// ==> split updateDownstreamPorts in two parts
            System.out.println("update the set in the OutPortState => ..."); 
        }
    }
    
	@Override
	public void run() {
		try {
			doRun();
		} catch(RegistrationException ex) {
			LOGGER.error("cannot access downstream ports. registry not available.");
		} catch(RuntimeException re) {
			LOGGER.error("runtime exception occurred.", re);
			throw re;
		} catch(Error er) {
			LOGGER.error("error occurred.", er);
			throw er;
		}
	}
	
	static void pollForNeededConnections(OutPortHandler outPorts, PortRegistryTranslator portAccessor, PortHierarchy portHierarchy) {
        while(true) {
            try { 
                outPorts.updateDownstreamPorts(portAccessor, portHierarchy);
                if(outPorts.requiredDownstreamPortsSet()) {
                    return;
                }
            } catch (RegistrationException e) {
                LOGGER.warn("could not access registry for retrieving downstream ports", e);
            }
            LOGGER.info("did not find initial values for all required out ports; sleeping for some time... ");
            try { 
                Thread.sleep(3000); 
            } catch(InterruptedException ie) {
                LOGGER.info("thread interrupted (by system?)", ie);
            }
        }
        // throw new IllegalStateException();
	}
}
