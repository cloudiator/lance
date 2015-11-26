/*
 * Copyright (c) 2014-2015 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.uniulm.omi.cloudiator.lance.lca.container.port;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;

final class DownstreamPortUpdater implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);
    
    private final OutPortHandler outPorts;
    private final PortRegistryTranslator portAccessor;
    private final PortHierarchy portHierarchy;
    private final LifecycleController controller;
    private final Object portUpdateLock = new Object();

    // protected by portUpdateLock
	private boolean updateInProgress;
    
    DownstreamPortUpdater(OutPortHandler outPortParams, PortRegistryTranslator portAccessorParam, 
                PortHierarchy portHierarchyParam, LifecycleController controllerParam) {
        outPorts = outPortParams;
        portAccessor = portAccessorParam;
        portHierarchy = portHierarchyParam;
        controller = controllerParam;
    }
    
    /*
    @Deprecated
    public void handleUpdate(OutPort port, PortDiff<?> diff) {
        //FIXME: ensure that we are in running state 
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
      
    }  */
    
    private List<PortDiff<DownstreamAddress>> getUpdatedPortSet() throws RegistrationException {
    	synchronized(portUpdateLock) {
    		if(updateInProgress) {
    			LOGGER.info("omitting port update. other update already in progress.");
    			return null;
    		}
    		List<PortDiff<DownstreamAddress>> diffs = outPorts.updateDownstreamPorts(portAccessor, portHierarchy);
    		if(! outPorts.requiredDownstreamPortsSet()) {
    			LOGGER.error("not all downstream ports are available. this may cause some issues");
    			// FIXME: what should happen is that we return to INSTALL state //
    			return null;
    		}
    		updateInProgress = true;
    		return diffs;
    	}
    }
    
    private void handleDiffSet(List<PortDiff<DownstreamAddress>> diffs) {
    	if(diffs.isEmpty()) {
    		LOGGER.info("omitting port update. diffSet empty. nothing to update.");
            return;
        }
        
        // now that we got all changed ports, for each of them
        // call the port update handler
        for(PortDiff<DownstreamAddress> diff : diffs) {
            OutPort port = diff.getPort();
            PortUpdateHandler handler = port.getUpdateHandler();
            LOGGER.info("calling update handler for port: " + diff);
            try {
            	controller.blockingUpdatePorts(port, handler, diff);
            	LOGGER.info("port update handler for port: " + diff + " done. manifesting changes.");
            	synchronized(portUpdateLock) {
            		outPorts.manifestChangeset(diff);
            	}
            } catch(ContainerException ce) {
            	LOGGER.warn("could not update ports: " + diff, ce);
            }
          }
    }

    //FIXME: ensure only one update process running at a time
    // add synchronized section here?
    private void doRun() throws RegistrationException {
    	try {
    		List<PortDiff<DownstreamAddress>> diffs = getUpdatedPortSet();
    		handleDiffSet(diffs);
    	} finally {
    		synchronized(portUpdateLock){
    			updateInProgress = false;
    		}
    	}
        
    }
    
    @Override
    public void run() {
        try {
            doRun();
        } catch(RegistrationException ex) {
            LOGGER.error("cannot access downstream ports. registry not available.", ex);
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
