/*
 * Copyright (c) 2014-2018 University of Ulm
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

package de.uniulm.omi.cloudiator.lance.client;

import static de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus.*;
import static java.lang.Thread.sleep;

import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters.ProvidedPortContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.util.ArrayList;
import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.component.*;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.util.Map;
import java.util.Random;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStoreBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.bash.BashBasedHandlerBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortLinkage;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import java.util.concurrent.Callable;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.lang.Exception;

// Important: Java-re, docker daemon and Lance(Server) must be installed on the VM and related ports
// must be opened on the vm
// Install etcd on the vm via install_etcd.sh which can be found in the pinned installation
// repository

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientDockerPullTest {

  private enum version { DOCKER, LIFECYCLE };

  private static ApplicationId applicationId;
  private static ApplicationInstanceId appInstanceId;

  private static String zookeeperComponent, zookeeperComponent_lifecycle, rubyComponent_remote;
  private static String zookeeperInternalInportName, zookeeperInternalInportName_lifecycle, rubyInternalInportName_remote;
  private static String zookeeperOutportName, zookeeperOutportName_lifecycle, rubyOutportName_remote;
  private static String imageName, imageName_remote;
  private static ComponentId zookeeperComponentId, zookeeperComponentId_lifecycle, rubyComponentId_remote;
  private static int defaultZookeeperInternalInport, defaultZookeeperInternalInport_lifecycle, defaultRubyInternalInport_remote;
  private static ComponentInstanceId zookId, zookId_lifecycle, zookId_remote;
  // adjust
  private static String publicIp = "x.x.x.x";
  private static LifecycleClient client;

  @BeforeClass
  public static void configureAppContext() {
    applicationId = new ApplicationId();
    appInstanceId = new ApplicationInstanceId();

    Random rand = new Random();
    zookeeperComponent = "zookeeper";
    zookeeperComponent_lifecycle = "zookeeper_lifecycle";
    rubyComponent_remote = "ruby_remote";
    zookeeperInternalInportName = "ZOOK_INT_INP";
    zookeeperInternalInportName_lifecycle = "ZOOK_INT_INP_LIFECYCLE";
    rubyInternalInportName_remote = "RUBY_INT_INP_REMOTE";
    zookeeperOutportName = "ZOOK_OUT";
    zookeeperOutportName_lifecycle = "ZOOK_OUT_LIFECYCLE";
    rubyOutportName_remote = "RUBY_OUT_REMOTE";
    imageName = "zookeeper";
    imageName_remote = "mysql";
    zookeeperComponentId = new ComponentId();
    zookeeperComponentId_lifecycle = new ComponentId();
    rubyComponentId_remote = new ComponentId();
    defaultZookeeperInternalInport = 3888;
    defaultZookeeperInternalInport_lifecycle = (rand.nextInt(65563) + 1);
    defaultRubyInternalInport_remote = (rand.nextInt(65563) + 1);

    System.setProperty("lca.client.config.registry", "etcdregistry");
    // adjust
    System.setProperty("lca.client.config.registry.etcd.hosts", "x.x.x.x:4001");
  }

  private DockerComponent.Builder buildDockerComponentBuilder(
      LifecycleClient client,
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      String imageFolder,
      String tag, boolean isRemote,
      boolean isDynComp, boolean isDynHandler) {
    DockerComponent.Builder builder;
    if (isRemote) {
      builder = new DockerComponent.Builder(buildEntireDockerCommands(isDynComp, isDynHandler), imageName_remote);
    } else {
      builder = new DockerComponent.Builder(buildEntireDockerCommands(isDynComp, isDynHandler), imageName);
    }
    builder.name(compName);
    builder.imageFolder(imageFolder);
    builder.tag(tag);
    builder.myId(id);

    for (int i = 0; i < inInfs.size(); i++)
      builder.addInport(
          inInfs.get(i).inportName,
          inInfs.get(i).portType,
          inInfs.get(i).cardinality,
          inInfs.get(i).inPort);


    for (int i = 0; i < outInfs.size(); i++)
      builder.addOutport(
          outInfs.get(i).outportName,
          outInfs.get(i).puHandler,
          outInfs.get(i).cardinality,
          outInfs.get(i).min);

    builder.deploySequentially(true);
    return builder;
  }

  private DockerComponent.Builder buildDockerComponentBuilder(
      LifecycleClient client,
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      String tag, boolean isRemote,
    boolean isDynComp, boolean isDynHandler) {

    return buildDockerComponentBuilder(client, compName, id, inInfs, outInfs, "", tag, isRemote, isDynComp, isDynHandler);
  }

  private DockerComponent buildDockerComponent(
      LifecycleClient client,
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      String tag,
      boolean isDynComp, boolean isDynHandler) {

    DockerComponent.Builder builder = buildDockerComponentBuilder(client, compName, id, inInfs, outInfs, tag, false, isDynComp, isDynHandler);
    DockerComponent comp = builder.build();
    return comp;
  }

  private RemoteDockerComponent buildRemoteDockerComponent( LifecycleClient client, String compName, ComponentId id, List<InportInfo> inInfs,
      List<OutportInfo> outInfs, String imageFolder, String tag, String hostName, int port, boolean useCredentials) {

    DockerComponent.Builder dCompBuilder = buildDockerComponentBuilder(client, compName, id, inInfs, outInfs, imageFolder, tag, true, false, false);
    RemoteDockerComponent.DockerRegistry dReg = new RemoteDockerComponent.DockerRegistry(hostName, port, "xxxxx", "xxxxx", useCredentials);
    RemoteDockerComponent rDockerComp = new RemoteDockerComponent(dCompBuilder, dReg);

    return rDockerComp;
  }

  private DeployableComponent buildDeployableComponent(
      LifecycleClient client,
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      Callable<LifecycleStore> createLifeCycleStore) {

    DeployableComponent.Builder builder = DeployableComponent.Builder.createBuilder(compName,id);

    for (int i = 0; i < inInfs.size(); i++)
      builder.addInport(
          inInfs.get(i).inportName,
          inInfs.get(i).portType,
          inInfs.get(i).cardinality,
          inInfs.get(i).inPort);

    for (int i = 0; i < outInfs.size(); i++)
      builder.addOutport(
          outInfs.get(i).outportName,
          outInfs.get(i).puHandler,
          outInfs.get(i).cardinality,
          outInfs.get(i).min);

    try {
      builder.addLifecycleStore(createLifeCycleStore.call());
    } catch (Exception ex) {
      System.err.println("Server not reachable");
    }

    builder.deploySequentially(true);
    DeployableComponent comp = builder.build();
    return comp;
  }

  static class InportInfo {
    public final String inportName;
    public final PortProperties.PortType portType;
    public final int cardinality;
    public final int inPort;

    InportInfo(String inportName, PortProperties.PortType portType, int cardinality, int inPort) {
      this.inportName = inportName;
      this.portType = portType;
      this.cardinality = cardinality;
      this.inPort = inPort;
    }
  }

  static class OutportInfo {
    public final String outportName;
    public final PortUpdateHandler puHandler;
    public final int cardinality;
    public final int min;

    OutportInfo(String outportName, PortUpdateHandler puHandler, int cardinality, int min) {
      this.outportName = outportName;
      this.puHandler = puHandler;
      this.cardinality = cardinality;
      this.min = min;
    }
  }

  private LifecycleStore createDefaultLifecycleStore() {
    LifecycleStoreBuilder store = new LifecycleStoreBuilder();
    // pre-install handler //
    BashBasedHandlerBuilder builder_pre = new BashBasedHandlerBuilder();
    // TODO: Extend possible OSes, e.g. alpine (openjdk:8-jre)
    builder_pre.setOperatingSystem(
        new OperatingSystemImpl(
            OperatingSystemFamily.UBUNTU,
            OperatingSystemArchitecture.AMD64,
            OperatingSystemVersions.of(1604,null)));
    builder_pre.addCommand("apt-get -y -q update");
    builder_pre.addCommand("apt-get -y -q upgrade");
    builder_pre.addCommand("export STARTED=\"true\"");
    store.setStartDetector(builder_pre.buildStartDetector());
    // add other commands
    store.setHandler(
        builder_pre.build(LifecycleHandlerType.PRE_INSTALL), LifecycleHandlerType.PRE_INSTALL);
    // weitere handler? //
    return store.build();
  }

  @Test
  public void testAClientGetter() {
    try {
      client = LifecycleClient.getClient(publicIp);
    } catch (RemoteException ex) {
      System.err.println("Server not reachable");
    } catch (NotBoundException ex) {
      System.err.println("Socket not bound");
    }
  }

  @Test
  public void testBRegister() {
    try {
      client.registerApplicationInstance(appInstanceId, applicationId);
      client.registerComponentForApplicationInstance(appInstanceId, zookeeperComponentId);
      client.registerComponentForApplicationInstance(appInstanceId, zookeeperComponentId_lifecycle);
      client.registerComponentForApplicationInstance(appInstanceId, rubyComponentId_remote);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  @Test
  public void testCZookCompDescriptions() {
    List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName);
    List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName);
    buildDockerComponent( client, zookeeperComponent, zookeeperComponentId, inInfs, outInfs, "3.4.12", true, false);
  }

  @Test
  public void testCZookCompDescriptions_lifecycle() {
    List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName_lifecycle);
    List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName_lifecycle);
    buildDeployableComponent( client, zookeeperComponent_lifecycle, zookeeperComponentId_lifecycle,
        inInfs, outInfs, new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return createDefaultLifecycleStore();
          }
        });
  }

  private static List<InportInfo> getInPortInfos(String internalInportName) {
    List<InportInfo> inInfs = new ArrayList<>();
    InportInfo inInf =
        new InportInfo(internalInportName, PortProperties.PortType.INTERNAL_PORT, 2, 3888);
    inInfs.add(inInf);

    return inInfs;
  }

  private static List<OutportInfo> getOutPortInfos(String outPortName) {
    List<OutportInfo> outInfs = new ArrayList<>();
    OutportInfo outInf =
        new OutportInfo(
            outPortName,
            DeploymentHelper.getEmptyPortUpdateHandler(),
            1,
            OutPort.NO_SINKS);
    outInfs.add(outInf);

    return outInfs;
  }

  private static DeploymentContext createZookeperContext(LifecycleClient client, ComponentId otherComponentId, String otherInternalInportName, version v) {
    String internalInportName;
    int defaultInternalInport;
    String outportName;

    if(v == version.DOCKER) {
      internalInportName = zookeeperInternalInportName;
      defaultInternalInport = defaultZookeeperInternalInport;
      outportName = zookeeperOutportName;
    } else if(v == version.LIFECYCLE) {
      internalInportName = zookeeperInternalInportName_lifecycle;
      defaultInternalInport = defaultZookeeperInternalInport_lifecycle;
      outportName = zookeeperOutportName_lifecycle;
    } else {
      internalInportName = rubyInternalInportName_remote;
      defaultInternalInport = defaultRubyInternalInport_remote;
      outportName = rubyOutportName_remote;
    }

    DeploymentContext zookeeper_context =
        client.initDeploymentContext(applicationId, appInstanceId);
    // saying that we want to use the default port as the actual port number //
    zookeeper_context.setProperty(
        internalInportName ,(Object) defaultInternalInport, InPort.class);
    // saying that we wire this outgoing port to the incoming ports of CASSANDRA //
    zookeeper_context.setProperty(
        outportName ,
        (Object)
            new PortReference(otherComponentId, otherInternalInportName , PortLinkage.ALL),
        OutPort.class);
    return zookeeper_context;
  }

  EntireDockerCommands buildEntireDockerCommands(boolean isDynComp, boolean isDynHandler) {
    Random rand = new Random();
    EntireDockerCommands.Builder cmdsBuilder = new EntireDockerCommands.Builder();
    try {
      Map<Option,List<String>> createOptionMap = new HashMap<>();
      createOptionMap.put(Option.ENVIRONMENT, Arrays.asList("foo=bar","john=doe"));
      if (isDynComp) {
        createOptionMap.put(Option.ENVIRONMENT, Arrays.asList("foo=bar","john=doe","dynamicgroup=group1"));
      }
      if (isDynHandler) {
        createOptionMap.put(Option.ENVIRONMENT, Arrays.asList("foo=bar","john=doe","dynamichandler=group1","updatescript=/the/script.sh"));
      }
      String  n = Integer.toString(rand.nextInt(65536) + 1);
      createOptionMap.put(Option.PORT, new ArrayList<>(Arrays.asList(n)));
      createOptionMap.put(Option.RESTART, new ArrayList<>(Arrays.asList("no")));
      createOptionMap.put(Option.INTERACTIVE, new ArrayList<>(Arrays.asList("")));
      List<OsCommand> createOsCommandList = new ArrayList<>();
      createOsCommandList.add(OsCommand.BASH);
      List<String> createArgsList = new ArrayList<>();
      createArgsList.add("--noediting");
      cmdsBuilder.setOptions(Type.CREATE, createOptionMap);
      cmdsBuilder.setCommand(Type.CREATE, createOsCommandList);
      cmdsBuilder.setArgs(Type.CREATE, createArgsList);

      Map<Option,List<String>> startOptionMap = new HashMap<>();
      startOptionMap.put(Option.INTERACTIVE, new ArrayList<>(Arrays.asList("")));
      cmdsBuilder.setOptions(Type.START, startOptionMap);
    } catch (DockerCommandException ce) {
      System.err.println("Error in creating docker commands");
    }

    return cmdsBuilder.build();
  }

  private void printCommandParts(EntireDockerCommands cmds) {
    try {
      System.out.println(cmds.getSetOptionsString(DockerCommand.Type.CREATE));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.Type.CREATE));
      System.out.println(cmds.getSetArgsString(DockerCommand.Type.CREATE));
      System.out.println("\n");
      System.out.println(cmds.getSetOptionsString(DockerCommand.Type.START));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.Type.START));
      System.out.println(cmds.getSetArgsString(DockerCommand.Type.START));
      System.out.println("\n");
      System.out.println(cmds.getSetOptionsString(DockerCommand.Type.STOP));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.Type.STOP));
      System.out.println(cmds.getSetArgsString(DockerCommand.Type.STOP));
    } catch (DockerCommandException e) {
      System.err.println("Error in printing docker command strings");
    }
  }

  @Test
  public void testFZookeeperDeploymentContext() {
    createZookeperContext(client, zookeeperComponentId_lifecycle, zookeeperInternalInportName_lifecycle, version.DOCKER );
  }

  @Test
  public void testFZookeeperDeploymentContext_lifecycle() {
    createZookeperContext(client, zookeeperComponentId, zookeeperInternalInportName, version.LIFECYCLE);
  }

  @Test
  public void testGLInsertExtDeplContext() {
    ExternalContextParameters.ProvidedPortContext inpC = new ProvidedPortContext("sparkJob1Port",9999);
    ExternalContextParameters.Builder builder = new ExternalContextParameters.Builder();
    builder.taskName("sparkJob1");
    builder.appId(appInstanceId);
    builder.compId(new ComponentId());
    builder.compInstId(new ComponentInstanceId());
    builder.pubIp(publicIp);
    builder.providedPortContext(inpC);
    builder.contStatus(ContainerStatus.READY);
    builder.compInstType(LifecycleHandlerType.START);

    ExternalContextParameters params = builder.build();

    try {
      client.injectExternalDeploymentContext(params);
    } catch (DeploymentException e) {
      System.err.println("Couldn't inject ExtContext");
    }
  }

  @Ignore
  @Test
  public void testHRemoteDockerComponent() {
    try {
      DeploymentContext dummyContext =
          client.initDeploymentContext(applicationId, appInstanceId);
      List<InportInfo> dummyInInfs = new ArrayList<>();
      /*InportInfo inInf =
          new InportInfo("dummInPort", PortProperties.PortType.INTERNAL_PORT, 2, 888);
      dummyInInfs.add(inInf);*/
      List<OutportInfo> dummyOutInfs = new ArrayList<>();
      /*OutportInfo outInf =
          new OutportInfo(
              "dummyOutPort",
              DeploymentHelper.getEmptyPortUpdateHandler(),
              1,
              OutPort.NO_SINKS);
      dummyOutInfs.add(outInf);*/
      //ssl Port
      RemoteDockerComponent rDockerComponent = buildRemoteDockerComponent( client, rubyComponent_remote, rubyComponentId_remote, dummyInInfs, dummyOutInfs,
          "fh/cdocker-reg", "latest","xxxx", 443, true);
      client.deploy(dummyContext, rDockerComponent);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy remote docker zookeeper component");
    }
  }

  @Test
  public void testIZookDeploy() {
    try {
      List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName);
      List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName);
      DockerComponent zookComp = buildDockerComponent( client, zookeeperComponent, zookeeperComponentId, inInfs, outInfs, "3.4.12", true, false);
      DeploymentContext zookContext = createZookeperContext(client, zookeeperComponentId, zookeeperInternalInportName, version.DOCKER );
      zookId =
          client.deploy(zookContext, zookComp);
      boolean isReady = false;
      do {
        System.out.println("zook not ready");
        isReady = client.isReady(zookId);
        sleep(50);
      } while (isReady != true);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy docker zookeeper component");
    } catch (InterruptedException ex) {
      System.err.println("Interrupted!");
    }

    System.out.println("zook is ready");
  }

  @Ignore
  @Test
  public void testIZookDeploy_lifecycle() {
    try {
      List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName_lifecycle);
      List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName_lifecycle);
      DeployableComponent zookComp = buildDeployableComponent( client, zookeeperComponent_lifecycle, zookeeperComponentId_lifecycle,
          inInfs, outInfs, new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return createDefaultLifecycleStore();
            }
          });
      DeploymentContext zookContext = createZookeperContext(client, zookeeperComponentId, zookeeperInternalInportName, version.LIFECYCLE);
      OperatingSystem os = new OperatingSystemImpl(
          OperatingSystemFamily.UBUNTU,
          OperatingSystemArchitecture.AMD64,
          OperatingSystemVersions.of(1604,null));
      zookId_lifecycle =
          client.deploy(zookContext, zookComp, os, ContainerType.DOCKER);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy lifecycle component");
    }
  }

  @Test
  public void testIZookDeploy_latest() {
    try {
      List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName_lifecycle);
      List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName_lifecycle);
      DockerComponent zookComp = buildDockerComponent( client, zookeeperComponent_lifecycle, zookeeperComponentId_lifecycle, inInfs, outInfs, "latest", false, true);
      DeploymentContext zookContext = createZookeperContext(client, zookeeperComponentId_lifecycle, zookeeperInternalInportName_lifecycle, version.LIFECYCLE );
      zookId_lifecycle =
          client.deploy(zookContext, zookComp);
      boolean isReady = false;
      do {
        System.out.println("zook latest not ready");
        isReady = client.isReady(zookId_lifecycle);
        sleep(50);
      } while (isReady != true);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy docker zookeeper component");
    } catch (InterruptedException ex) {
      System.err.println("Interrupted!");
    }

    System.out.println("zook latest is ready");
  }

  @Test
  public void testLComponentStatus() {
    ContainerStatus zookStatus, zookStatus_lifecycle;
    zookStatus = zookStatus_lifecycle = UNKNOWN;
    do {
      try {
        zookStatus = client.getComponentContainerStatus(zookId, publicIp);
        zookStatus_lifecycle  = client.getComponentContainerStatus(zookId_lifecycle , publicIp);
        System.out.println("ZOOKEEPER STATUS:" + zookStatus);
        System.out.println("ZOOKEEPER STATUS (LIFECYCLE):" + zookStatus_lifecycle );
        sleep(5000);
      } catch (DeploymentException ex) {
        System.err.println("Exception during deployment!");
      } catch (InterruptedException ex) {
        System.err.println("Interrupted!");
      }
    } while (zookStatus != READY || zookStatus_lifecycle != READY);
  }

  @Test
  public void testMStopContainers() {
    try {
      sleep(2000);
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            client.undeploy(zookId, false);
          } catch (DeploymentException e) {
            System.err.println("Exception during deployment!");
          };
        }
      });
      t.start();
      sleep(150000);
      client.undeploy(zookId_lifecycle, false);
      t.join();
    } catch (DeploymentException ex) {
      System.err.println("Exception during deployment!");
    } catch (InterruptedException ex) {
      System.err.println("Interrupted!");
    }
  }
}