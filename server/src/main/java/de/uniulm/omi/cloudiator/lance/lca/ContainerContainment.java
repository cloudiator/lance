package de.uniulm.omi.cloudiator.lance.lca;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.BasicContainer;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerController;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManager;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerManagerFactory;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;

final class ContainerContainment implements BasicContainer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ContainerContainment.class);
	
	private final Map<ContainerType, ContainerManager> managers = new HashMap<>();

	@Override
	public ContainerController getContainer(ComponentInstanceId id) {
		// a bit cumbersome, but should do the trick //
		ContainerController result = null;
		for(Map.Entry<ContainerType, ContainerManager> e : managers.entrySet()) {
			ContainerManager m = e.getValue(); 
        	if(m == null) 
        		continue;
        	ContainerController cc = m.getContainer(id);
        	if(cc == null) // not found here //  
        		continue;
        	if(result != null) {
        		LOGGER.error("container id " + id + " found more than one time in different containers.");
        		continue;
			}
        	// found one that was not taken
        	result = cc;
		}
		return result;
	}

    public ContainerManager getContainerManager(HostContext contex, OperatingSystem operatingSystem, ContainerType containerType) throws LcaException{
    	if(containerType.supportsOsFamily(operatingSystem.getFamily())) {
    		synchronized(managers) {
    			ContainerManager mng = managers.get(containerType);
    			if(mng != null) {
    				mng = ContainerManagerFactory.createContainerManager(contex, containerType);
    				managers.put(containerType, mng);
        		}
    			return mng;
    		}
    	} 
        throw new LcaException("Operating System and container do not match: " + operatingSystem.toString());
    }

	@Override
	public void terminate() {
        for(Map.Entry<ContainerType, ContainerManager> e : managers.entrySet()) {
        	if(e.getValue() == null) 
        		continue;
        	e.getValue().terminate();
        }
	}

	@Override
	public List<ComponentInstanceId> getAllContainers() {
		List<ComponentInstanceId> result = new ArrayList<ComponentInstanceId>();
		for(Map.Entry<ContainerType, ContainerManager> e : managers.entrySet()) {
			if(e.getValue() == null) 
        		continue;
			List<ComponentInstanceId> tmp = e.getValue().getAllContainers();
			result.addAll(tmp);
		}
		return result;
	}

	@Override
	public ContainerStatus getComponentContainerStatus(ComponentInstanceId cid) {
		ContainerController cc = getContainer(cid);
		return cc.getState();
	}
}
