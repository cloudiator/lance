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
        //todo: extend, so that multiple outports can be required by this providedPort -> Map<Integer,List<ProvidedPortInfo> requiredPortsMap & adjust @2nd inner loop below
        Map<Integer,ComponentId> requiredComponentsMap = new HashMap<>();
        Map<Integer,ProvidedPortInfo> requiredPortsMap = new HashMap<>();

        for (ComponentInfo cInf : arch.getComponents()) {
            setProvidedPortInfoContext(cInf, requiredComponentsMap, requiredPortsMap);
        }
        for (ComponentInfo cInf : arch.getComponents()) {
            setRequiredPortInfoContext(cInf, requiredComponentsMap, requiredPortsMap);
        }
    }

    private void setProvidedPortInfoContext(ComponentInfo cInf, Map<Integer,ComponentId> requiredComponentsMap, Map<Integer,ProvidedPortInfo> requiredPortsMap) {
        for (ProvidedPortInfo provInf: cInf.getProvidedPortInfos()) {
            ctx.setProperty(provInf.getProvidedPortName(), (Object) provInf.getProvidedPort(), InPort.class);
            requiredComponentsMap.put(provInf.getPortRefNumber(),cInf.getComponentId());
            requiredPortsMap.put(provInf.getProvidedPort(),provInf);
        }
    }

    //call this method after setProvidedPortInfoContext
    private void setRequiredPortInfoContext(ComponentInfo cInf, Map<Integer,ComponentId> requiredComponentsMap, Map<Integer,ProvidedPortInfo> requiredPortsMap) {
        for (RequiredPortInfo reqInf : cInf.getRequiredPortInfos()) {
            if (reqInf.getMin() != OutPort.NO_SINKS && requiredPortsMap.get(reqInf.getRequiredPortNumber()) != null) {
                ComponentId compId = requiredComponentsMap.get(reqInf.getRequiredPortNumber());
                ProvidedPortInfo portRefInfo = requiredPortsMap.get(reqInf.getRequiredPortNumber());
                ctx.setProperty(reqInf.getRequiredPortName(), (Object) new PortReference(compId, portRefInfo.getProvidedPortName(), PortProperties.PortLinkage.ALL), OutPort.class);
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
        return reg.dumpComponent(arch.getAppInstanceId(), cId);
    }
}
