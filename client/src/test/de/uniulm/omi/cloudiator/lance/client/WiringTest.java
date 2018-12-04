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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WiringTest {

  private static LifecycleClient client;
  private static AppArchitecture arch;
  // adjust
  private static String publicIp = "x.x.x.x";
  private static String imageName = "ubuntu";
  private static List<DockerDeployInfo> dInfos = new ArrayList<>();
  private static List<ComponentInstanceId> deployFirst = new ArrayList<>();
  private static List<ComponentInstanceId> deploySecond = new ArrayList<>();
  private static Map<Integer, String> providedPortName = new HashMap<>();
  private static Map<Integer, ComponentId> providedPortId = new HashMap<>();
  private static String extCompName = "SPARK";
  private static ComponentId extCompId;
  private static ComponentInstanceId extCompInstId;
  private static List<InPortContext> inpContextUnUsed = new ArrayList<>();
  private static List<InPortContext> inpContextUsed = new ArrayList<>();
  //Important: Name is written in reg with prefix "ACCESS_"
  private static String extComPortName = "SPARK_INPORT";
  private static int extComPortUnUsed = 9998;
  private static int extComPortUsed = 9999;
  private static ProvidedPortInfo extProvidedPortInfo;
  private static Thread tInjectOrig, tDeplRoot, tDeplDStream;

  @BeforeClass
  public static void configureAppContext() {
    AppArchitectureBuilder builder =
        new AppArchitectureBuilder("WiringApp", new ApplicationId(), new ApplicationInstanceId());
    // root-component
    ProvidedPortInfo rootProvidedPortInfo =
        new ProvidedPortInfo("ROOT_INPORT", PortProperties.PortType.PUBLIC_PORT, 800, 0, 0);
    HashSet<ProvidedPortInfo> rootProvSet = new HashSet<>();
    rootProvSet.add(rootProvidedPortInfo);
    RequiredPortInfo rootRequiredPortInfo1 =
        new RequiredPortInfo(
            "ROOT_OUTPORT_1", DeploymentHelper.getEmptyPortUpdateHandler(), 1, 1, 1);
    RequiredPortInfo rootRequiredPortInfo2 =
        new RequiredPortInfo(
            "ROOT_OUTPORT_2", DeploymentHelper.getEmptyPortUpdateHandler(), 2, 1, 1);
    RequiredPortInfo rootRequiredPortInfo3 =
        new RequiredPortInfo(
            "ROOT_OUTPORT_3", DeploymentHelper.getEmptyPortUpdateHandler(), 3, 1, 1);
    Set<RequiredPortInfo> rootReqSet = new HashSet<>();
    rootReqSet.add(rootRequiredPortInfo1);
    rootReqSet.add(rootRequiredPortInfo2);
    rootReqSet.add(rootRequiredPortInfo3);
    ComponentInfo rootCompInfo =
        new ComponentInfo(
            "root",
            new ComponentId(),
            new ComponentInstanceId(),
            rootProvSet,
            rootReqSet,
            OperatingSystem.UBUNTU_14_04);
    // downstream-component1
    ProvidedPortInfo downstream1ProvidedPortInfo =
        new ProvidedPortInfo("DOWNSTREAM1_INPORT", PortType.CLUSTER_PORT, 801, 1, 1);
    HashSet<ProvidedPortInfo> downstream1ProvSet = new HashSet<>();
    downstream1ProvSet.add(downstream1ProvidedPortInfo);
    ComponentInfo downstream1CompInfo =
        new ComponentInfo(
            "downstream_1",
            new ComponentId(),
            new ComponentInstanceId(),
            downstream1ProvSet,
            new HashSet<>(),
            OperatingSystem.UBUNTU_14_04);
    // downstream-component2
    ProvidedPortInfo downstream2ProvidedPortInfo =
        new ProvidedPortInfo("DOWNSTREAM2_INPORT", PortType.CLUSTER_PORT, 802, 2, 1);
    HashSet<ProvidedPortInfo> downstream2ProvSet = new HashSet<>();
    downstream2ProvSet.add(downstream2ProvidedPortInfo);
    ComponentInfo downstream2CompInfo =
        new ComponentInfo(
            "downstream_2",
            new ComponentId(),
            new ComponentInstanceId(),
            downstream2ProvSet,
            new HashSet<>(),
            OperatingSystem.UBUNTU_14_04);
    // setup Architecture Info
    arch =
        builder
            .addComponentInfo(rootCompInfo)
            .addComponentInfo(downstream1CompInfo)
            .addComponentInfo(downstream2CompInfo)
            .build();
    extCompId = new ComponentId();
    extCompInstId = new ComponentInstanceId();
    InPortContext inportContextUnUsed = new InPortContext(extComPortName, extComPortUnUsed);
    inpContextUnUsed.add(inportContextUnUsed);
    InPortContext inportContextUsed = new InPortContext(extComPortName, extComPortUsed);
    inpContextUsed.add(inportContextUsed);
    //Important: UpstreamComponent looks for the name with prefix "ACCESS_"
    extProvidedPortInfo =
        new ProvidedPortInfo(extComPortName, PortType.CLUSTER_PORT, extComPortUsed, 3, 1);

    System.setProperty("lca.client.config.registry", "etcdregistry");
    // adjust
    System.setProperty("lca.client.config.registry.etcd.hosts", "x.x.x.x:4001");
  }

  @Test
  public void test0DumpInstanceIds() {
    for(ComponentInfo cInf: arch.getComponents()) {
      System.out.println(cInf.getComponentInstanceId());
    }
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
    for (ComponentInfo cInfo : arch.getComponents()) {
      // Simplified, not really traversing a tree structure
      if (cInfo.getRequiredPortInfos().size() == 0) {
        deployFirst.add(cInfo.getComponentInstanceId());
      } else {
        buildDependencyMap(arch, cInfo.getRequiredPortInfos());
        deploySecond.add(cInfo.getComponentInstanceId());
      }
      DeploymentContext ctx = buildDeploymentContext(arch, cInfo);
      DockerComponent.Builder dBuilder =
          ClientTestUtils.buildDockerComponentBuilder(
              client,
              cInfo.getComponentName(),
              cInfo.getComponentId(),
              cInfo.getProvidedPortInfos(),
              cInfo.getRequiredPortInfos(),
              "",
              imageName,
              "");
      DockerComponent dComp = dBuilder.build();
      dInfos.add(new DockerDeployInfo(dComp, ctx, cInfo.getComponentInstanceId()));
    }
  }

  //Important, if ext. component is not registered, then the upstream component throws a key-not-found-exception while in the CreateTransition
  //Important: Already in pre-create-Action the downstream-Port must be found in the reg
  //!!!Important: If Instance with needed downstream port name is ready (in registry), then the requiredPort of the in creation state being
  //component must be equal with the providedPort in the registry. If not, exception is thrown!!!
  @Test
  public void test3Register() {
    try {
      client.registerApplicationInstance(arch.getAppInstanceId(), arch.getApplicationId());
      for (ComponentInfo cInfo : arch.getComponents()) {
        client.registerComponentForApplicationInstance(
            arch.getAppInstanceId(), cInfo.getComponentId());
      }
      //dito
      client.registerComponentForApplicationInstance(
          arch.getAppInstanceId(), extCompId);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  @Test
  public void test4InsertExtDeplContext() {
    tInjectOrig = new Thread(new ThreadUtils.InsertExtDeplContextThread());
    tInjectOrig.start();
  }

  @Test
  public void test5DeployRoot() {
    tDeplRoot = new Thread(new ThreadUtils.DeplRootCompsThread());
    tDeplRoot.start();
    try {
      tDeplRoot.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test5DeployDownstream() {
    tDeplDStream = new Thread(new ThreadUtils.DeplDownstrCompsThread());
    tDeplDStream.start();
  }

  @Test(expected = DeploymentException.class)
  public void test6UpdateExtDeplContextFail() throws DeploymentException {
    try {
      tInjectOrig.join();
      tDeplDStream.join();
      tDeplRoot.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    ExternalContextParameters.Builder builder = new ExternalContextParameters.Builder();
    ComponentInfo cInfo = buildRandomInternalComponentInfo();
    builder.name(cInfo.getComponentName());
    builder.appInstanceId(arch.getAppInstanceId());
    builder.compId(cInfo.getComponentId());
    builder.compInstId(cInfo.getComponentInstanceId());
    builder.status(ContainerStatus.UNKNOWN);

    ExternalContextParameters params = builder.build();
    client.injectExternalDeploymentContext(params);
  }

  private static ComponentInfo buildRandomInternalComponentInfo() {
    ComponentInfo cInfoTmp = arch.getComponents().iterator().next();
    ComponentId cId = cInfoTmp.getComponentId();
    try {
      final Map<ComponentInstanceId,Map<String,String>> dump = client.getComponentDumps(arch.getAppInstanceId(), cId);
      //just inspect first element
      Map.Entry<ComponentInstanceId,Map<String,String>> entry = dump.entrySet().iterator().next();
      final String isExtenal = entry.getValue().get("External_Component");
      assertEquals("false",isExtenal);
      ComponentInfo cInfo =
          new ComponentInfo( cInfoTmp.getComponentName(), cInfoTmp.getComponentId(), entry.getKey(), new HashSet<>(),
              new HashSet<>(), OperatingSystem.UBUNTU_14_04);
      return cInfo;
    } catch (RegistrationException e) {
      e.printStackTrace();
    }
    System.err.println("Returning component with false instanceID");
    return cInfoTmp;
  }

  @Test
  public void test7UpdateExtDeplContext() throws DeploymentException {
    ExternalContextParameters.Builder builder = new ExternalContextParameters.Builder();
    builder.name(extCompName);
    builder.appInstanceId(arch.getAppInstanceId());
    builder.compId(extCompId);
    builder.compInstId(extCompInstId);
    builder.inPortContext(inpContextUsed);
    builder.status(ContainerStatus.READY);

    ExternalContextParameters params = builder.build();

    try {
      client.injectExternalDeploymentContext(params);
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  private static void buildDependencyMap(AppArchitecture arch, Set<RequiredPortInfo> outportInfos) {
    for (RequiredPortInfo outInf : outportInfos) {
      int requPortRef = outInf.getRequiredPortNumber();

      for (ComponentInfo cInfo : arch.getComponents()) {
        for (ProvidedPortInfo inInf : cInfo.getProvidedPortInfos()) {
          int provPortRef = inInf.getPortRefNumber();
          // No nxm connections, only 1x1
          if (requPortRef == provPortRef) {
            providedPortName.put(provPortRef, inInf.getProvidedPortName());
            providedPortId.put(provPortRef, cInfo.getComponentId());
          }
        }
        // External Component
        int provPortRef = extProvidedPortInfo.getPortRefNumber();
        if (requPortRef == provPortRef) {
          providedPortName.put(provPortRef, extProvidedPortInfo.getProvidedPortName());
          providedPortId.put(provPortRef, extCompId);
        }
      }
    }
  }

  private static DeploymentContext buildDeploymentContext(
      AppArchitecture arch, ComponentInfo cInfo) {
    DeploymentContext ctx =
        client.initDeploymentContext(arch.getApplicationId(), arch.getAppInstanceId());

    for (ProvidedPortInfo provInf : cInfo.getProvidedPortInfos()) {
      ctx.setProperty(
          provInf.getProvidedPortName(), (Object) provInf.getProvidedPort(), InPort.class);
    }

    for (RequiredPortInfo reqInf : cInfo.getRequiredPortInfos()) {
      String depName = providedPortName.get(reqInf.getRequiredPortNumber());

      if (depName != null) {
        ctx.setProperty(
            reqInf.getRequiredPortName(),
            (Object)
                new PortReference(
                    providedPortId.get(reqInf.getRequiredPortNumber()), depName, PortLinkage.ALL),
            OutPort.class);
      }
    }
    return ctx;
  }

  private static class DockerDeployInfo {
    public DockerComponent comp;
    public DeploymentContext ctx;
    ComponentInstanceId cId;

    public DockerDeployInfo(DockerComponent comp, DeploymentContext ctx, ComponentInstanceId cId) {
      this.comp = comp;
      this.ctx = ctx;
      this.cId = cId;
    }
  }

  private static class ThreadUtils {

    private static void deploy(List<ComponentInstanceId> deployInstances)
        throws DeploymentException {
      for (ComponentInstanceId cInstId : deployInstances) {
        for (DockerDeployInfo info : dInfos) {
          ComponentInstanceId origId = info.cId;
          if (cInstId == origId) {
            client.deploy(info.ctx, info.comp);
          }
        }
      }
    }

    private static class InsertExtDeplContextThread implements Runnable {

      @Override
      public void run() {
        ExternalContextParameters.Builder builder = new ExternalContextParameters.Builder();
        builder.name(extCompName);
        builder.appInstanceId(arch.getAppInstanceId());
        builder.compId(extCompId);
        builder.compInstId(extCompInstId);
        builder.pubIp(publicIp);
        builder.inPortContext(inpContextUnUsed);
        builder.status(ContainerStatus.UNKNOWN);

        ExternalContextParameters params = builder.build();

        try {
          client.injectExternalDeploymentContext(params);
        } catch (DeploymentException e) {
          System.err.println("Couldn't inject ExtContext");
        }
      }
    }

    private static class DeplRootCompsThread implements Runnable {

      @Override
      public void run() {
        try {
          deploy(deploySecond);
        } catch (DeploymentException e) {
          e.printStackTrace();
        }
      }
    }

    private static class DeplDownstrCompsThread implements Runnable {

      @Override
      public void run() {
        try {
          deploy(deployFirst);
        } catch (DeploymentException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
