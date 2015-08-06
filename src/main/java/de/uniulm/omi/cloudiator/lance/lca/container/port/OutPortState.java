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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

public final class OutPortState {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OutPort.class);

    private final Object lock = new Object();
    private final OutPort thePort;
    private final Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> possibleSinks;
    
    public OutPortState(OutPort outPortParam, Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> instancesParam) {
        thePort = outPortParam;
        possibleSinks = new HashMap<>(instancesParam);
    }

    public String getPortName() { return thePort.getName(); }
    
    @Override
    public String toString() {
        return thePort + " => " + possibleSinks;
    }

    public boolean matchesPort(OutPort thatPort) {
        return thePort.namesMatch(thatPort);
    }

    public PortDiff<DownstreamAddress> updateWithDiff(Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> newSinks) {
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> oldSinks = getCurrentSinkSet();
        PortDiff<DownstreamAddress> diff = new PortDiff<>(newSinks, oldSinks, thePort.getName());
        
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> compOldSinks = installNewSinks(newSinks);
        if(!oldSinks.equals(compOldSinks)) {
            LOGGER.warn("old sinks do not match; do we have concurrency problems?"); 
        }
        return diff;
    }
    
    public boolean requiredAndSet() {
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> sinks = getCurrentSinkSet();
        return sinks.size() > thePort.getLowerBound();
    }
    
    OutPort getPort() { return thePort; }

    Map<PortHierarchyLevel, List<DownstreamAddress>> sinksByHierarchyLevel() {
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> sinks = getCurrentSinkSet();
        Map<PortHierarchyLevel,List<DownstreamAddress>> elements = new HashMap<>();
        for(Entry<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> entry : sinks.entrySet()) {
            // ComponentInstanceId id = entry.getKey();
            HierarchyLevelState<DownstreamAddress> state = entry.getValue();
            for(PortHierarchyLevel level : state){
                List<DownstreamAddress> l = getElement(elements, level);
                DownstreamAddress value = state.valueAtLevel(level);
                l.add(value);
            }
        }
        return elements;
    }
    
    List<DownstreamAddress> adaptSinkListByBoundaries(List<DownstreamAddress> sinks) {
        final int a = sinks.size();
        
        if(a < thePort.getLowerBound()) return null; // Collections.emptyList();
        final int b = thePort.getUpperBound();
        final int upper = (b == OutPort.INFINITE_SINKS) ? 
                a : Math.min(a, thePort.getUpperBound());
        
        while(sinks.size() > upper) {
            sinks.remove(sinks.size() - 1);
        }
        
        return sinks;
    }
    
    private static List<DownstreamAddress> getElement(Map<PortHierarchyLevel,List<DownstreamAddress>> elements, PortHierarchyLevel level) {
        List<DownstreamAddress> l = elements.get(level);
        if(l == null) {
            l = new LinkedList<>();
            elements.put(level, l);
        }
        return l;
    }
    
    private Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> installNewSinks(Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> newSinksParam) {
        HashMap<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> old = null;
        synchronized(lock) {
            old = new HashMap<>(possibleSinks);
            possibleSinks.clear(); 
            possibleSinks.putAll(newSinksParam);
        }
        return old;
    }
    
    private Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> getCurrentSinkSet() {
        synchronized(lock) { return possibleSinks; }
    }

    /*
    public List<String> getBoundaryAwareSinkList(String this_cloud_id,
            final Map<ComponentInstanceId, Map<String, String>> the_sinks) {
        
        List<String> addresses = new ArrayList<String>();
        final int a = the_sinks.size();
        
        if(a < the_port.getLowerBound()) return null; // Collections.emptyList();
        final int b = the_port.getUpperBound();
        final int upper = b == OutPort.INFINITE_SINKS ? 
                a : Math.min(a, the_port.getUpperBound());
        
        final String sink_name = registry.getSinkPortNameAsFullPortName(the_port); 
        
        for(ComponentInstanceId id : the_sinks.keySet()) {
            Map<String,String> map = the_sinks.get(id);
            addSinkAddress(this_cloud_id, sink_name, addresses, map);
            final int count = addresses.size();
            if(count == upper) break;
        }
        if(addresses.size() < a) {
            return null; // Collections.emptyList();    
        }
        return addresses;
    }
        /** returns a set of (possibly random) sinks that 
     * are connected to this out port. If too few 
     * sinks are available, an empty list is returned. 
     * if too many sinks are available, the list is 
     * cut to fit the boundary.
     * @return 
     * /
    public List<String> getBoundaryAwareSinkList() {
        final Map<ComponentInstanceId, HierarchyLevelState<Integer>> the_sinks;
        synchronized(lock) { the_sinks = new HashMap<ComponentInstanceId, HierarchyLevelState<Integer>>(possible_sinks); }
        
        return getBoundaryAwareSinkList(registry.getLocalCloudProdivder(), the_sinks);
    }
    private void revertFlag() {
        boolean flagged = flag.compareAndSet(false, true);
        if(!flagged) {System.err.println("ERROR: flag could not be set back");}
    }
    
        
    private static void addSinkAddress(String this_cloud_id, String portName, List<String> addresses, Map<String,String> map) {
        String ipAddress = GlobalRegistryAccessor.determineIpaddress(this_cloud_id, map);
        if(ipAddress == null) return;
        Integer port = GlobalRegistryAccessor.readPortProperty(portName, map);
        if(port != null) addresses.add(ipAddress + ":" + port);
    }

    
    public void enactScheduling(PortUpdateCallback updater) {
        Runnable runner = new Poller(updater);
        registry.schedulePortPolling(runner);
        return;
    }
    
    class Updater implements Runnable {
        private final PortUpdateCallback updater;
        private final PortDiff diff;
        public Updater(PortUpdateCallback _updater, PortDiff _diff) {
            updater = _updater;
            diff = _diff;
        }

        @Override public void run() {
            try {doRun();}
            catch(Throwable t){t.printStackTrace();}
            finally {
                revertFlag();
            }
        }

        private void doRun() {
            updater.handleUpdate(the_port, diff);
        }    
    }
    
    private final AtomicBoolean flag = new AtomicBoolean(true);
    class Poller implements Runnable {
        private final PortUpdateCallback updater;
        private AtomicInteger misses = new AtomicInteger(0);
        Poller(PortUpdateCallback _updater) {
            updater = _updater;
        }

        @Override public void run() {
            boolean entry = flag.compareAndSet(true, false);
            if(!entry) {System.err.println("skipping polling, updae in progress"); return;}
            try {doRun();}
            catch(Throwable t){t.printStackTrace();}
        }
        
        private void doRun() {
            try {
                PortDiff diff = getMatchingEntries();
                misses.set(0);
                if(! diff.hasDiffs()) {
                    revertFlag();
                    return;
                }
                registry.run(new Updater(updater, diff));
            } catch (RegistrationException re) {
                int j = misses.getAndIncrement();
                System.err.println("could not access registry; misses counter: " + j);
            }
        }
    }*/
}
