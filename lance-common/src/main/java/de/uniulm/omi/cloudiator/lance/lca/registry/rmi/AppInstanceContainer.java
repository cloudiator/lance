package de.uniulm.omi.cloudiator.lance.lca.registry.rmi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

final class AppInstanceContainer {

    //private final ApplicationId appId;
    private final ApplicationInstanceId appInstId;
    private final Map<ComponentId,ComponentInstanceContainer> comps = new HashMap<>();
    
    AppInstanceContainer(ApplicationInstanceId instId, @SuppressWarnings("unused") ApplicationId appIdParam, 
            @SuppressWarnings("unused") String name) { 
        //appId = _appId; 
        appInstId = instId; 
    }

    String getComponentProperty(ComponentId compId, ComponentInstanceId myId, String name) {
        ComponentInstanceContainer c = comps.get(compId);
        if(c == null) 
            throw new IllegalArgumentException("not known: " + compId);
        return c.getComponentProperty(myId, name);
    }

    Map<ComponentInstanceId, Map<String, String>> dumpAll(ComponentId compId) {
        ComponentInstanceContainer c = comps.get(compId);
        if(c == null) 
            return Collections.emptyMap();
        
        return c.dumpInstances();
    }

    void addComponentProperty(ComponentId cid, ComponentInstanceId cinstId, String property, Object value) {
        ComponentInstanceContainer c = comps.get(cid);
        if(c == null) 
            throw new IllegalArgumentException("not known: " + cid);
        c.addComponentProperty(cinstId, property, value);
    }

    void addComponent(ComponentId cid, String name) {
        if(comps.containsKey(cid)) 
            throw new IllegalArgumentException("alread exists: " + cid);
        comps.put(cid, new ComponentInstanceContainer(this, cid, name));
    }

    void addComponentInstance(ComponentId cid, ComponentInstanceId cinstId) {
        ComponentInstanceContainer c = comps.get(cid);
        if(c == null) 
            throw new IllegalArgumentException("not known: " + cid);
        c.addComponentInstance(cinstId);
    }
    
    boolean componentExists(ComponentId cid) {
        return comps.containsKey(cid);
    }
    
    @Override
    public String toString() {
        return appInstId.toString();
    }
}