package de.uniulm.omi.cloudiator.lance.lca.container.port;

import de.uniulm.omi.cloudiator.lance.application.component.OutLambda;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OutLambdaState {

    private final OutLambda outLambda;
    private final Map<ComponentInstanceId, HierarchyLevelState<String>> possibleSinks;

    public OutLambdaState(OutLambda outLambda, Map<ComponentInstanceId, HierarchyLevelState<String>> possibleSinks) {
        this.outLambda = outLambda;
        this.possibleSinks = possibleSinks;
    }

    public OutLambda getOutLambda() {
        return outLambda;
    }

    Map<PortHierarchyLevel, List<String>> sinksByHierarchyLevel() {
        return orderSinksByHierarchyLevel(possibleSinks);
    }

    static Map<PortHierarchyLevel, List<String>> orderSinksByHierarchyLevel(Map<ComponentInstanceId, HierarchyLevelState<String>> sinks) {

        Map<PortHierarchyLevel, List<String>> elements = new HashMap<>();
        for (Map.Entry<ComponentInstanceId, HierarchyLevelState<String>> entry : sinks.entrySet()) {
            HierarchyLevelState<String> state = entry.getValue();

            for (PortHierarchyLevel level : state) {
                List<String> l = getElement(elements, level);
                String value = state.valueAtLevel(level);
                l.add(value);
            }
        }
        return elements;
    }

    private static List<String> getElement(
            Map<PortHierarchyLevel, List<String>> elements, PortHierarchyLevel level) {
        return elements.computeIfAbsent(level, k -> new LinkedList<>());
    }

}
