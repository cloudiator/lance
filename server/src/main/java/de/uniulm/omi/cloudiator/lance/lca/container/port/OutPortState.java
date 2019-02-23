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

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class OutPortState {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutPort.class);

    private final Object lock = new Object();
    private final OutPort thePort;
    private final Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> possibleSinks;

    public OutPortState(OutPort outPortParam,
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> instancesParam) {
        thePort = outPortParam;
        possibleSinks = new HashMap<>(instancesParam);
    }

    public String getPortName() {
        return thePort.getName();
    }

    @Override public String toString() {
        return thePort + " => " + possibleSinks;
    }

    public boolean matchesPort(OutPort thatPort) {
        return thePort.namesMatch(thatPort);
    }

    PortDiff<DownstreamAddress> computeDiffSet(
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> newSinks) {
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> oldSinks =
            getCurrentSinkSet();
        PortDiff<DownstreamAddress> diff = new PortDiff<>(newSinks, oldSinks, thePort);
        return diff;
    }

    /**
     * @param diff the diff set to apply
     * @return true if the port of the OutPort of the diff set matches the OutPort maintained by this OutPortState.
     */
    boolean enactDiffSet(PortDiff<DownstreamAddress> diff) {
        if (!diff.portMatches(thePort)) {
            return false;
        }

        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> newSinks =
            diff.getCurrentSinkSet();
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> oldSinks =
            diff.getOldSinkSet();

        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> compOldSinks =
            installNewSinks(newSinks);
        if (!oldSinks.equals(compOldSinks)) {
            LOGGER.warn(
                "old sinks do not match; do we have concurrency pvroblems?" + oldSinks + " vs "
                    + compOldSinks);
        }

        return true;
    }

    @Deprecated PortDiff<DownstreamAddress> updateWithDiff(
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> newSinks) {
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> oldSinks =
            getCurrentSinkSet();
        PortDiff<DownstreamAddress> diff = new PortDiff<>(newSinks, oldSinks, thePort);

        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> compOldSinks =
            installNewSinks(newSinks);
        if (!oldSinks.equals(compOldSinks)) {
            LOGGER.warn("old sinks do not match; do we have concurrency problems?");
        }
        return diff;
    }

    public boolean requiredAndSet() {
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> sinks =
            getCurrentSinkSet();

        boolean ret = !(thePort.getLowerBound() > sinks.size());

        LOGGER.debug(String
            .format("Evaluating require and set - lower bound: %s; sinks: %s. Return %s.",
                thePort.getLowerBound(), sinks.size(), ret));

        return ret;
    }

    OutPort getPort() {
        return thePort;
    }

    Map<PortHierarchyLevel, List<DownstreamAddress>> sinksByHierarchyLevel() {
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> sinks =
            getCurrentSinkSet();
        return orderSinksByHierarchyLevel(sinks);
    }

    static Map<PortHierarchyLevel, List<DownstreamAddress>> orderSinksByHierarchyLevel(
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> sinks) {
        Map<PortHierarchyLevel, List<DownstreamAddress>> elements = new HashMap<>();
        for (Entry<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> entry : sinks
            .entrySet()) {
            // ComponentInstanceId id = entry.getKey();
            HierarchyLevelState<DownstreamAddress> state = entry.getValue();
            for (PortHierarchyLevel level : state) {
                List<DownstreamAddress> l = getElement(elements, level);
                DownstreamAddress value = state.valueAtLevel(level);
                l.add(value);
            }
        }
        return elements;
    }

    List<DownstreamAddress> adaptSinkListByBoundaries(List<DownstreamAddress> sinks) {
        final int a = sinks.size();

        if (a < thePort.getLowerBound()) {
            return null; // Collections.emptyList();
        }
        final int b = thePort.getUpperBound();
        final int upper = (b == OutPort.INFINITE_SINKS) ? a : Math.min(a, thePort.getUpperBound());

        while (sinks.size() > upper) {
            sinks.remove(sinks.size() - 1);
        }

        return sinks;
    }

    private static List<DownstreamAddress> getElement(
        Map<PortHierarchyLevel, List<DownstreamAddress>> elements, PortHierarchyLevel level) {
        List<DownstreamAddress> l = elements.get(level);
        if (l == null) {
            l = new LinkedList<>();
            elements.put(level, l);
        }
        return l;
    }

    private Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> installNewSinks(
        Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> newSinksParam) {
        HashMap<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> old = null;
        synchronized (lock) {
            old = new HashMap<>(possibleSinks);
            possibleSinks.clear();
            possibleSinks.putAll(newSinksParam);
        }
        return old;
    }

    private Map<ComponentInstanceId, HierarchyLevelState<DownstreamAddress>> getCurrentSinkSet() {
        synchronized (lock) {
            return possibleSinks;
        }
    }

}
