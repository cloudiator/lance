package de.uniulm.omi.cloudiator.lance.lifecycles;


import java.util.HashMap;
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.*;
import de.uniulm.omi.cloudiator.lance.lca.EnvContextWrapperRM;
import de.uniulm.omi.cloudiator.lance.lca.HostContext;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.util.application.*;

public class CoreElementsRemote {

    private final static String cloudIp = "127.0.0.1";

    public volatile static AppArchitecture arch;
    public volatile static HostContext context;

    public volatile DeploymentContext ctx;
    public volatile LcaRegistry reg;

    public CoreElementsRemote(LcaRegistry reg) {
        this.reg = reg;
        ctx = new DeploymentContext(arch.getApplicationId(), arch.getAppInstanceId(), reg);
        setPortInfoContext();
    }

    private void setPortInfoContext() {
        //todo: extend, so that multiple outports can be required by this inport -> Map<Integer,List<InportInfo> requiredPortsMap & adjust @2nd inner loop below
        Map<Integer,ComponentId> requiredComponentsMap = new HashMap<>();
        Map<Integer,InportInfo> requiredPortsMap = new HashMap<>();

        for (ComponentInfo cInf : arch.getComponents()) {
            setInportInfoContext(cInf, requiredComponentsMap, requiredPortsMap);
        }
        for (ComponentInfo cInf : arch.getComponents()) {
            setOutportInfoContext(cInf, requiredComponentsMap, requiredPortsMap);
        }
    }

    private void setInportInfoContext(ComponentInfo cInf, Map<Integer,ComponentId> requiredComponentsMap, Map<Integer,InportInfo> requiredPortsMap) {
        for (InportInfo inInf: cInf.getInportInfos()) {
            ctx.setProperty(inInf.getInportName(), (Object) inInf.getInPort(), InPort.class);
            requiredComponentsMap.put(inInf.getRequiredPortNumber(),cInf.getComponentId());
            requiredPortsMap.put(inInf.getRequiredPortNumber(),inInf);
        }
    }

    //call this method after setInportInfoContext
    private void setOutportInfoContext(ComponentInfo cInf, Map<Integer,ComponentId> requiredComponentsMap, Map<Integer,InportInfo> requiredPortsMap) {
        for (OutportInfo outInf : cInf.getOutportInfos()) {
            if (outInf.getMin() != OutPort.NO_SINKS && requiredPortsMap.get(outInf.getProvidedPortNumber()) != null) {
                ComponentId compId = requiredComponentsMap.get(outInf.getProvidedPortNumber());
                InportInfo portRefInfo = requiredPortsMap.get(outInf.getProvidedPortNumber());
                ctx.setProperty(outInf.getOutportName(), (Object) new PortReference(compId, portRefInfo.getInportName(), PortProperties.PortLinkage.ALL), OutPort.class);
            }
        }
    }

    public static void initHostContext(String publicIp) {

        System.setProperty("host.ip.public", publicIp);
        System.setProperty("host.ip.private", cloudIp);
        System.setProperty("host.vm.cloud.tenant.id", "tenant: 33033");
        System.setProperty("host.vm.id", "vm: 33033");
        System.setProperty("host.vm.cloud.id", "cloud: 33033");

        EnvContextWrapperRM.setPublicIp(publicIp);
        EnvContextWrapperRM.setCloudIp(cloudIp);
        context = EnvContextWrapperRM.create();
    }

    public void setUpRegistry() throws RegistrationException {
        reg.addApplicationInstance(arch.getAppInstanceId(), arch.getApplicationId(), arch.getApplicationName());
    }

    public void fillRegistry(ComponentId cId) throws RegistrationException {
        reg.addComponent(arch.getAppInstanceId(), cId, arch.getApplicationName());
        //this is done in the createNewContainerMethod
        //reg.addComponentInstance(arch.getAppInstanceId(), cId, cInstId);
    }

    public Map<ComponentInstanceId, Map<String, String>> checkBasicRegistryValues(ComponentId cId) throws RegistrationException {
        assert reg.applicationInstanceExists(arch.getAppInstanceId());
        assert reg.applicationComponentExists(arch.getAppInstanceId(), cId);
        return reg.dumpComponent(arch.getAppInstanceId(), cId, false);
    }
}
