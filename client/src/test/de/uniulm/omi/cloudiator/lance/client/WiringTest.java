package de.uniulm.omi.cloudiator.lance.client;

import static org.junit.Assert.assertEquals;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortLinkage;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortType;
import de.uniulm.omi.cloudiator.lance.application.component.PortReference;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters.InPortContext;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitecture;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitectureBuilder;
import de.uniulm.omi.cloudiator.lance.util.application.ComponentInfo;
import de.uniulm.omi.cloudiator.lance.util.application.ProvidedPortInfo;
import de.uniulm.omi.cloudiator.lance.util.application.RequiredPortInfo;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WiringTest {

  private static AppArchitecture arch;
  //adjust
  private static LifecycleClient client;
  private static String publicIp = "x.x.x.x";
  private static String imageName =  "ubuntu";
  private static List<DockerDeployInfo> dInfos = new ArrayList<>();
  private static List<ComponentInstanceId> deployFirst = new ArrayList<>();
  private static List<ComponentInstanceId> deploySecond = new ArrayList<>();
  private static Map<Integer,String> providedPortName = new HashMap<>();
  private static Map<Integer,ComponentId> providedPortId = new HashMap<>();

  @BeforeClass
  public static void configureAppContext() {
    AppArchitectureBuilder builder = new AppArchitectureBuilder("WiringApp", new ApplicationId(), new ApplicationInstanceId());
    //root-component
    ProvidedPortInfo rootProvidedPortInfo = new ProvidedPortInfo("ROOT_INPORT", PortProperties.PortType.PUBLIC_PORT, 800, 0, 0);
    HashSet<ProvidedPortInfo> rootProvSet = new HashSet<>();
    rootProvSet.add(rootProvidedPortInfo);
    RequiredPortInfo rootRequiredPortInfo1 = new RequiredPortInfo("ROOT_OUTPORT_1", DeploymentHelper.getEmptyPortUpdateHandler(), 1, 1, 1);
    RequiredPortInfo rootRequiredPortInfo2 = new RequiredPortInfo("ROOT_OUTPORT_2", DeploymentHelper.getEmptyPortUpdateHandler(), 2, 1, 1);
    HashSet<RequiredPortInfo> rootReqSet = new HashSet<>();
    rootReqSet.add(rootRequiredPortInfo1);
    rootReqSet.add(rootRequiredPortInfo2);
    ComponentInfo rootCompInfo = new ComponentInfo("root", new ComponentId(), new ComponentInstanceId(), rootProvSet, rootReqSet, OperatingSystem.UBUNTU_14_04);
    //downstream-component1
    ProvidedPortInfo downstream1ProvidedPortInfo = new ProvidedPortInfo("DOWNSTREAM1_INPORT", PortType.CLUSTER_PORT, 801, 1, 1);
    HashSet<ProvidedPortInfo> downstream1ProvSet = new HashSet<>();
    downstream1ProvSet.add(downstream1ProvidedPortInfo);
    ComponentInfo downstream1CompInfo  = new ComponentInfo("downstream_1", new ComponentId(), new ComponentInstanceId(), downstream1ProvSet, new HashSet<>(), OperatingSystem.UBUNTU_14_04);
    //downstream-component2
    ProvidedPortInfo downstream2ProvidedPortInfo = new ProvidedPortInfo("DOWNSTREAM2_INPORT", PortType.CLUSTER_PORT, 802, 2, 1);
    HashSet<ProvidedPortInfo> downstream2ProvSet = new HashSet<>();
    downstream2ProvSet.add(downstream2ProvidedPortInfo);
    ComponentInfo downstream2CompInfo  = new ComponentInfo("downstream_2", new ComponentId(), new ComponentInstanceId(), downstream2ProvSet, new HashSet<>(), OperatingSystem.UBUNTU_14_04);
    //setup Architecture Info
    arch = builder.addComponentInfo(rootCompInfo).addComponentInfo(downstream1CompInfo).addComponentInfo(downstream2CompInfo).build();

    System.setProperty("lca.client.config.registry", "etcdregistry");
    //adjust
    System.setProperty("lca.client.config.registry.etcd.hosts",  "x.x.x.x:4001");
  }

  @Test
  public void test1ClientGetter() {
    try {
      client = LifecycleClient.getClient(publicIp);
    } catch (RemoteException ex) {
      System.err.println("Server not reachable");
    } catch (NotBoundException ex) {
      System.err.println("Socket not bound");
    }
  }

  @Test
  public void test2BuildDeploymentInfos() {
    for(ComponentInfo cInfo: arch.getComponents()) {
      // Simplified, not really traversing a tree structure
      if (cInfo.getRequiredPortInfos().size() == 0) {
        deployFirst.add(cInfo.getComponentInstanceId());
      } else {
        buildDependencyMap(arch, cInfo.getRequiredPortInfos());
        deploySecond.add(cInfo.getComponentInstanceId());
      }
      DeploymentContext ctx = buildDeploymentContext(arch, cInfo);
      DockerComponent.Builder dBuilder =
          ClientTestUtils.buildDockerComponentBuilder(client,cInfo.getComponentName(),cInfo.getComponentId(),cInfo.getProvidedPortInfos(),cInfo.getRequiredPortInfos(),"", imageName, "");
      DockerComponent dComp = dBuilder.build();
      dInfos.add(new DockerDeployInfo(dComp, ctx, cInfo.getComponentInstanceId()));
    }
  }

  @Test
  public void test3Register() {
    try {
      client.registerApplicationInstance(arch.getAppInstanceId(), arch.getApplicationId());
      for(ComponentInfo cInfo: arch.getComponents()) {
        client.registerComponentForApplicationInstance(arch.getAppInstanceId(), cInfo.getComponentId());
      }
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  @Test
  public void test4Deploy() {
    try {
      deployCompsFirst();
      deployCompsSecond();
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy lifecycle component");
    }
  }

  @Test
  public void test4InsertExtDeplContext() {
    ExternalContextParameters.InPortContext inpC = new InPortContext("SPARKJOB1_INPORT",9999);
    List<InPortContext> inpCList = new ArrayList<>();
    inpCList.add(inpC);
    ExternalContextParameters.Builder builder = new ExternalContextParameters.Builder();
    builder.name("sparkJob1");
    builder.appInstanceId(arch.getAppInstanceId());
    builder.compId(new ComponentId());
    builder.compInstId(new ComponentInstanceId());
    builder.pubIp(publicIp);
    builder.inPortContext(inpCList);
    builder.status(ContainerStatus.READY);

    ExternalContextParameters params = builder.build();

    try {
      client.injectExternalDeploymentContext(params);
    } catch (DeploymentException e) {
      System.err.println("Couldn't inject ExtContext");
    }
  }


  private static void deployCompsFirst() throws DeploymentException {
    deploy(deployFirst);
  }

  private static void deployCompsSecond() throws DeploymentException {
    deploy(deploySecond);
  }

  private static void deploy(List<ComponentInstanceId> deployInstances) throws DeploymentException {
    for(ComponentInstanceId cInstId: deployInstances) {
      for(DockerDeployInfo info: dInfos) {
        ComponentInstanceId origId = info.cId;
        if(cInstId == origId) {
          client.deploy(info.ctx, info.comp);
        }
      }
    }
  }

  private static void buildDependencyMap(AppArchitecture arch, Set<RequiredPortInfo> outportInfos) {
    for(RequiredPortInfo outInf: outportInfos) {
      int requPortRef = outInf.getRequiredPortNumber();

      for(ComponentInfo cInfo: arch.getComponents()) {
        for(ProvidedPortInfo inInf: cInfo.getProvidedPortInfos()) {
          int provPortRef = inInf.getPortRefNumber();
          //No nxm connections, only 1x1
          if(requPortRef == provPortRef) {
            providedPortName.put(provPortRef , inInf.getProvidedPortName());
            providedPortId.put(provPortRef , cInfo.getComponentId());
          }
        }
      }
    }
  }

  private DeploymentContext buildDeploymentContext(AppArchitecture arch, ComponentInfo cInfo) {
    DeploymentContext ctx = client.initDeploymentContext(arch.getApplicationId(), arch.getAppInstanceId());

    for(ProvidedPortInfo provInf: cInfo.getProvidedPortInfos()) {
      ctx.setProperty(
          provInf.getProvidedPortName() ,(Object) provInf.getProvidedPort(), InPort.class);
    }

    for(RequiredPortInfo reqInf: cInfo.getRequiredPortInfos()) {
      String depName = providedPortName.get(reqInf.getRequiredPortNumber());

      if(depName != null) {
        ctx.setProperty(reqInf.getRequiredPortName(), (Object)
            new PortReference(providedPortId.get(reqInf.getRequiredPortNumber()) , depName , PortLinkage.ALL), OutPort.class);
      }
    }
    return ctx;
  }

  private static class DockerDeployInfo {
    public DockerComponent comp;
    public DeploymentContext ctx;
    ComponentInstanceId cId;

    public  DockerDeployInfo(DockerComponent comp, DeploymentContext ctx, ComponentInstanceId cId) {
      this.comp = comp;
      this.ctx = ctx;
      this.cId = cId;
    }
  }
}
