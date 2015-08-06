package de.uniulm.omi.cloudiator.lance.lca.registry.rmi;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

final class ComponentInstanceContainer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LcaRegistry.class);

    private final AtomicInteger counter = new AtomicInteger(0);
    private final AppInstanceContainer myContainer;
    private final ComponentId cid;
    private final Map<ComponentInstanceId, Map<String,Object>> instances = new HashMap<>();
    
    public ComponentInstanceContainer(AppInstanceContainer cnt, ComponentId _cid, 
    		@SuppressWarnings("unused") String name) { 
    	myContainer = cnt; 
    	cid = _cid; 
    }
    
    public String getComponentProperty(ComponentInstanceId myId, String name) {
        Map<String,Object> props = instances.get(myId);
        if(props == null) throw new IllegalArgumentException("not known: " + myId);
        Object old = props.get(name);
        return old == null ? null : old.toString();
    }

    public Map<ComponentInstanceId, Map<String,String>> dumpInstances() {
        Map<ComponentInstanceId, Map<String,String>> copy = new HashMap<>();
        for(Entry<ComponentInstanceId, Map<String, Object>> entry : instances.entrySet()) {
            ComponentInstanceId id = entry.getKey();
            Map<String,Object> content = entry.getValue();
            Map<String,String> inner_copy = new HashMap<>();
            for(Entry<String, Object> innerEntry : content.entrySet()) {
                inner_copy.put(innerEntry.getKey(), innerEntry.getValue().toString());
            }
            copy.put(id, inner_copy);
        }
        return copy;
    }

    public void addComponentProperty(ComponentInstanceId cinstId, String property, Object value) {
        Map<String,Object> props = instances.get(cinstId);
        if(props == null) throw new IllegalArgumentException("not known: " + cinstId);
        Object old = props.put(property, value);
        if(old != null) RemoteRegistryImpl.LOGGER.warn("warning: overriding value!");
        //FIXME: wake up listeners (?)
        LOGGER.error("TODO: wake up listeners");
        
        LOGGER.info("LcaRegistry: added property: " + this + "/" + cinstId + "." + property + "=" + value);
    }

    public void addComponentInstance(ComponentInstanceId cinstId) {
        if(instances.containsKey(cinstId)) throw new IllegalArgumentException("alread exists: " + cinstId);
        Map<String,Object> map = new HashMap<>();
        Integer i = Integer.valueOf(counter.incrementAndGet());
        map.put(LcaRegistryConstants.INSTANCE_NR, i);
        instances.put(cinstId, map);
        
        LOGGER.info("LcaRegistry: added component instance: " + this + "/" + cinstId);
    }
    
    @Override
    public String toString() {
        return myContainer + "/" + cid;
    }
}