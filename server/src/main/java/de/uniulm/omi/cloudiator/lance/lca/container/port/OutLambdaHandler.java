package de.uniulm.omi.cloudiator.lance.lca.container.port;

import de.uniulm.omi.cloudiator.lance.application.component.AbstractComponent;
import de.uniulm.omi.cloudiator.lance.application.component.OutLambda;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.DynamicEnvVars;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.DynamicEnvVarsImpl;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

public class OutLambdaHandler implements DynamicEnvVars {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutPort.class);

    private static final Map<PortHierarchyLevel, List<String>> FUNCTION_HANDLER_MAP;

    private static final Predicate<OutPort> FUNCTION_HANDLER_PREDICATE = outPort -> outPort.getName().contains("Lambda");

    static {
        Map<PortHierarchyLevel, List<String>> functionHandlerMap = new HashMap<>();
        functionHandlerMap.put(PortRegistryTranslator.PORT_HIERARCHY_FUNCTION_HANDLER, Collections.emptyList());
        FUNCTION_HANDLER_MAP = Collections.unmodifiableMap(functionHandlerMap);
    }

    private final List<OutLambdaState> lambdaStates = new ArrayList<>();

    private final AbstractComponent myComponent;

    private DynamicEnvVarsImpl currentEnvVarsDynamic;

    public OutLambdaHandler(AbstractComponent myComponentParam) {
        myComponent = myComponentParam;
        currentEnvVarsDynamic = DynamicEnvVarsImpl.NETWORK_PORTS;
    }

    void initLambdaStates(PortRegistryTranslator accessor, PortHierarchy portHierarchy) throws RegistrationException {
        LOGGER.info("invoking initLambdaStates");

        List<OutPort> outPorts = myComponent.getDownstreamPorts();
        if(outPorts.isEmpty()) {
            return;
        }

        for (OutPort outPort : outPorts) {
            if (FUNCTION_HANDLER_PREDICATE.test(outPort)){
                LOGGER.info("OutPort name: {}", outPort.getName());
                Map<ComponentInstanceId, HierarchyLevelState<String>> instances = accessor.findDownstreamInstances2(outPort, portHierarchy);
                OutLambda outLambda = new OutLambda(outPort.getName());
                OutLambdaState outLambdaState = new OutLambdaState(outLambda, instances);
                lambdaStates.add(outLambdaState);
            }
        }
    }

    void accept(NetworkVisitor visitor) {
        for (Map.Entry<String, String> entry : currentEnvVarsDynamic.getEnvVars().entrySet()) {
            visitor.visitInFunctionHandler(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Map<String, String> getEnvVars() {
        return currentEnvVarsDynamic.getEnvVars();
    }

    @Override
    public void injectDynamicEnvVars(DynamicEnvVarsImpl vars) throws ContainerException {
        this.currentEnvVarsDynamic = vars;
    }

    @Override
    public void removeDynamicEnvVars(DynamicEnvVars vars) throws ContainerException {
        if(!currentEnvVarsDynamic.equals(vars)){
            LOGGER.error("Cannot remove vars " + vars + " as they are not currently available.");
        }
        this.currentEnvVarsDynamic = DynamicEnvVarsImpl.FUNCTION_HANDLER;
    }

    @Override
    public void generateDynamicEnvVars() {
        generateDynamicEnvVars(null);
    }

    public void generateDynamicEnvVars(PortDiff<String> diffSet) {

        for (OutLambdaState lambdaState : lambdaStates) {
            Map<PortHierarchyLevel, List<String>> elements = null;
            if(diffSet != null) {
                elements = OutLambdaState.orderSinksByHierarchyLevel(diffSet.getCurrentSinkSet());
            } else {
                elements = lambdaState.sinksByHierarchyLevel();
            }
            Map<PortHierarchyLevel, List<String>> toGenerate = elements.isEmpty() ? doCollect(FUNCTION_HANDLER_MAP) : doCollect(elements);

            doGenerateDynamicEnvVars(lambdaState, toGenerate);
        }
    }

    private static Map<PortHierarchyLevel, List<String>> doCollect(Map<PortHierarchyLevel, List<String>> elements) {
        Map<PortHierarchyLevel, List<String>> toVisit = new HashMap<>();
        for(Map.Entry<PortHierarchyLevel, List<String>> entry : elements.entrySet()) {
            PortHierarchyLevel level = entry.getKey();
            List<String> sinks = entry.getValue();

            if(sinks == null) {
                return Collections.emptyMap();
            }
            toVisit.put(level, sinks);
        }
        return toVisit;
    }

    private void doGenerateDynamicEnvVars(OutLambdaState out, Map<PortHierarchyLevel, List<String>> toGenerate) {
        for(Map.Entry<PortHierarchyLevel, List<String>> entry : toGenerate.entrySet()) {

            PortHierarchyLevel level = entry.getKey();
            String name = level.getName().toUpperCase() + "_" + out.getOutLambda().getName();
            String sinkValues = String.join(",", entry.getValue());

            DynamicEnvVarsImpl portsVar = DynamicEnvVarsImpl.FUNCTION_HANDLER;
            portsVar.setEnvVars(buildSingleElementMap(name,sinkValues));
            try {
                injectDynamicEnvVars(portsVar);
            } catch (ContainerException ex) {
                LOGGER.error("Cannot inject variable " + portsVar + "as it has a wrong type");
            }
        }
    }

    private static Map<String,String> buildSingleElementMap(String key, String value) {
        return Collections.singletonMap(key, value);
    }

}
