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

import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.component.*;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStoreBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.bash.BashBasedHandlerBuilder;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
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

  private enum version {LIFECYCLE, DOCKER_DEPL};

  private static ApplicationId applicationId;
  private static ApplicationInstanceId appInstanceId;

  private static String zookeeperComponent, cassandraComponent, kafkaComponent;
  private static String zookeeperComponent_lifecycle, cassandraComponent_lifecycle, kafkaComponent_lifecycle;
  private static String zookeeperInternalInportName, cassandraInternalInportName, kafkaInternalInportName;
  private static String zookeeperInternalInportName_lifecycle, cassandraInternalInportName_lifecycle, kafkaInternalInportName_lifecycle;
  private static String zookeeperOutportName, cassandraOutportName, kafkaOutportName;
  private static String zookeeperOutportName_lifecycle, cassandraOutportName_lifecycle, kafkaOutportName_lifecycle;
  private static ComponentId zookeeperComponentId, cassandraComponentId, kafkaComponentId;
  private static ComponentId zookeeperComponentId_lifecycle, cassandraComponentId_lifecycle, kafkaComponentId_lifecycle;
  private static int defaultZookeeperInternalInport, defaultCassandraInternalInport, defaultKafkaInternalInport;
  private static int defaultZookeeperInternalInport_lifecycle, defaultCassandraInternalInport_lifecycle, defaultKafkaInternalInport_lifecycle;
  private static ComponentInstanceId zookId, cassId, kafkId;
  private static ComponentInstanceId zookId_lifecycle, cassId_lifecycle, kafkId_lifecycle;
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
    zookeeperInternalInportName = "ZOOK_INT_INP";
    zookeeperInternalInportName_lifecycle = "ZOOK_INT_INP_LIFECYCLE";
    zookeeperOutportName = "ZOOK_OUT";
    zookeeperOutportName_lifecycle = "ZOOK_OUT_LIFECYCLE";
    zookeeperComponentId = new ComponentId();
    zookeeperComponentId_lifecycle = new ComponentId();
    defaultZookeeperInternalInport = 3888;
    defaultZookeeperInternalInport_lifecycle = (rand.nextInt(65563) + 1);

    cassandraComponent = "cassandra";
    cassandraComponent_lifecycle = "cassandra_lifecycle";
    cassandraInternalInportName = "CASS_INT_INP";
    cassandraInternalInportName_lifecycle = "CASS_INT_INP_LIFECYCLE";
    cassandraOutportName = "CASS_OUT";
    cassandraOutportName_lifecycle = "CASS_OUT_LIFECYCLE";
    cassandraComponentId = new ComponentId();
    cassandraComponentId_lifecycle = new ComponentId();
    defaultCassandraInternalInport = 9160;
    defaultCassandraInternalInport_lifecycle = defaultCassandraInternalInport;

    kafkaComponent = "kafka";
    kafkaComponent_lifecycle = "kafka_lifecycle";
    kafkaInternalInportName = "KAFK_INT_INP";
    kafkaInternalInportName_lifecycle = "KAFK_INT_INP_LIFECYCLE";
    kafkaOutportName = "KAFK_OUT";
    kafkaOutportName_lifecycle = "KAFK_OUT_LIFECYCLE";
    kafkaComponentId = new ComponentId();
    kafkaComponentId_lifecycle = new ComponentId();
    defaultKafkaInternalInport = 9092;
    defaultKafkaInternalInport_lifecycle = defaultKafkaInternalInport;

    System.setProperty("lca.client.config.registry", "etcdregistry");
    // adjust
    System.setProperty("lca.client.config.registry.etcd.hosts", "x.x.x.x:4001");
  }

  private DockerComponent buildDockerComponent(
      LifecycleClient client,
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      Callable<LifecycleStore> createLifeCycleStore) {
    ComponentBuilder<DockerComponent> builder = new ComponentBuilder(DockerComponent.class, compName, id);

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
    DockerComponent comp = builder.build(DockerComponent.class);
    return comp;
  }

  private DeployableComponent buildDeployableComponent(
      LifecycleClient client,
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      Callable<LifecycleStore> createLifeCycleStore) {
    DeployableComponentBuilder builder = DeployableComponentBuilder.createBuilder(compName,id);

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
    builder_pre.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
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
      client.registerComponentForApplicationInstance(appInstanceId, cassandraComponentId);
      client.registerComponentForApplicationInstance(appInstanceId, cassandraComponentId_lifecycle);
      client.registerComponentForApplicationInstance(appInstanceId, kafkaComponentId);
      client.registerComponentForApplicationInstance(appInstanceId, kafkaComponentId_lifecycle);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  @Test
  public void testCZookCompDescriptions() {
    List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName);
    List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName);
    buildDeployableComponent(
        client,
        zookeeperComponent,
        zookeeperComponentId,
        inInfs,
        outInfs,
        new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return createDefaultLifecycleStore();
          }
        });
  }

  @Test
  public void testCZookCompDescriptions_lifecycle() {
    List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName_lifecycle);
    List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName_lifecycle);
    buildDeployableComponent(
        client,
        zookeeperComponent_lifecycle,
        zookeeperComponentId_lifecycle,
        inInfs,
        outInfs,
        new Callable<LifecycleStore>() {
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

  @Test
  public void testDCassCompDescriptions() {
    List<InportInfo> inInfs = getInPortInfos(cassandraInternalInportName);
    List<OutportInfo> outInfs = getOutPortInfos(cassandraOutportName);
    buildDeployableComponent(
        client,
        cassandraComponent,
        cassandraComponentId,
        inInfs,
        outInfs,
        new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return createDefaultLifecycleStore();
          }
        });
  }

  @Test
  public void testDCassCompDescriptions_lifecycle() {
    List<InportInfo> inInfs = getInPortInfos(cassandraInternalInportName_lifecycle);
    List<OutportInfo> outInfs = getOutPortInfos(cassandraOutportName_lifecycle);
    buildDeployableComponent(
        client,
        cassandraComponent_lifecycle,
        cassandraComponentId_lifecycle,
        inInfs,
        outInfs,
        new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return createDefaultLifecycleStore();
          }
        });
  }

  @Test
  public void testEKafkaCompDescriptions() {
    List<InportInfo> inInfs = getInPortInfos(kafkaInternalInportName);
    List<OutportInfo> outInfs = getOutPortInfos(kafkaOutportName);
    buildDeployableComponent(
        client,
        kafkaComponent,
        kafkaComponentId,
        inInfs,
        outInfs,
        new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return createDefaultLifecycleStore();
          }
        });
  }

  @Test
  public void testEKafkaCompDescriptions_lifecycle() {
    List<InportInfo> inInfs = getInPortInfos(kafkaInternalInportName_lifecycle);
    List<OutportInfo> outInfs = getOutPortInfos(kafkaOutportName_lifecycle);
    buildDeployableComponent(
        client,
        kafkaComponent_lifecycle,
        kafkaComponentId_lifecycle,
        inInfs,
        outInfs,
        new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return createDefaultLifecycleStore();
          }
        });
  }

  private static DeploymentContext createZookeperContext(LifecycleClient client, version v) {
    String internalInportName;
    int defaultInternalInport;
    String outportName;
    ComponentId cassComponentId;
    String cassInternalInportName;

    if(v == version.DOCKER_DEPL) {
      internalInportName = zookeeperInternalInportName;
      defaultInternalInport = defaultZookeeperInternalInport;
      outportName = zookeeperOutportName;
      cassComponentId = cassandraComponentId;
      cassInternalInportName = cassandraInternalInportName;
    } else {
      internalInportName = zookeeperInternalInportName_lifecycle;
      defaultInternalInport = defaultZookeeperInternalInport_lifecycle;
      outportName = zookeeperOutportName_lifecycle;
      cassComponentId = cassandraComponentId_lifecycle;
      cassInternalInportName = cassandraInternalInportName_lifecycle;
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
            new PortReference(cassComponentId, cassInternalInportName , PortLinkage.ALL),
        OutPort.class);
    return zookeeper_context;
  }

  private static DeploymentContext createCassandraContext(LifecycleClient client, version v) {
    String internalInportName;
    int defaultInternalInport;
    String outportName;
    ComponentId zookComponentId;
    String zookInternalInportName;

    if(v == version.DOCKER_DEPL) {
      internalInportName = cassandraInternalInportName;
      defaultInternalInport = defaultCassandraInternalInport;
      outportName = cassandraOutportName;
      zookComponentId = zookeeperComponentId;
      zookInternalInportName = zookeeperInternalInportName;
    } else {
      internalInportName = cassandraInternalInportName_lifecycle;
      defaultInternalInport = defaultCassandraInternalInport_lifecycle;
      outportName = cassandraOutportName_lifecycle;
      zookComponentId = zookeeperComponentId_lifecycle;
      zookInternalInportName = zookeeperInternalInportName_lifecycle;
    }

    DeploymentContext cassandra_context =
        client.initDeploymentContext(applicationId, appInstanceId);
    // saying that we want to use the default port as the actual port number //
    cassandra_context.setProperty(
        internalInportName ,(Object) defaultInternalInport, InPort.class);
    // saying that we wire this outgoing port to the incoming ports of CASSANDRA //
    cassandra_context.setProperty(
        outportName ,
        (Object)
            new PortReference(zookComponentId, zookInternalInportName , PortLinkage.ALL),
        OutPort.class);
    return cassandra_context;
  }

  private static DeploymentContext createKafkaContext(LifecycleClient client, version v) {
    String internalInportName;
    int defaultInternalInport;
    String outportName;
    ComponentId zookComponentId, cassComponentId;
    String zookInternalInportName, cassInternalInportName;

    if(v == version.DOCKER_DEPL) {
      internalInportName = kafkaInternalInportName;
      defaultInternalInport = defaultCassandraInternalInport;
      outportName = kafkaOutportName;
      zookComponentId = zookeeperComponentId;
      zookInternalInportName = zookeeperInternalInportName;
      cassComponentId = cassandraComponentId;
      cassInternalInportName = cassandraInternalInportName;
    } else {
      internalInportName = kafkaInternalInportName_lifecycle;
      defaultInternalInport = defaultCassandraInternalInport_lifecycle;
      outportName = kafkaOutportName_lifecycle;
      zookComponentId = zookeeperComponentId_lifecycle;
      zookInternalInportName = zookeeperInternalInportName_lifecycle;
      cassComponentId = cassandraComponentId_lifecycle;
      cassInternalInportName = cassandraInternalInportName_lifecycle;
    }

    DeploymentContext kafka_context =
        client.initDeploymentContext(applicationId, appInstanceId);
    // saying that we want to use the default port as the actual port number //
    kafka_context.setProperty(
        internalInportName ,(Object) defaultInternalInport, InPort.class);
    // saying that we wire this outgoing port to the incoming ports of CASSANDRA //
    kafka_context.setProperty(
        outportName ,
        (Object) new PortReference(zookComponentId, zookInternalInportName , PortLinkage.ALL),
        OutPort.class);
    kafka_context.setProperty(
        outportName ,
        (Object) new PortReference(cassComponentId, cassInternalInportName , PortLinkage.ALL),
        OutPort.class);
    return kafka_context;
  }

  EntireDockerCommands buildEntireDockerCommands(String name) {
    Random rand = new Random();
    DockerCommand.Builder createBuilder = new DockerCommand.Builder(DockerCommand.CREATE);
    DockerCommand.Builder startBuilder = new DockerCommand.Builder(DockerCommand.START);
    DockerCommand.Builder stopBuilder = new DockerCommand.Builder(DockerCommand.STOP);
    try {
      Map<Option,String> createOptionMap = new HashMap<>();
      createOptionMap.put(Option.ENVIRONMENT, "foo=bar");
      createOptionMap.put(Option.ENVIRONMENT, "john=doe");
      String  n = Integer.toString(rand.nextInt(65536) + 1);
      createOptionMap.put(Option.PORT, n);
      createOptionMap.put(Option.RESTART, "no");
      createOptionMap.put(Option.INTERACTIVE, "");
      List<OsCommand> createOsCommandList = new ArrayList<>();
      createOsCommandList.add(OsCommand.BASH);
      List<String> createArgsList = new ArrayList<>();
      createArgsList.add("--noediting");
      Map<Option,String> startOptionMap = new HashMap<>();
      startOptionMap.put(Option.INTERACTIVE, "");

      createBuilder.setOptions(createOptionMap);
      createBuilder.setCommand(createOsCommandList);
      createBuilder.setArgs(createArgsList);

      startBuilder.setOptions(startOptionMap);
    } catch (DockerCommandException ce) {
      System.err.println("Error in creating docker commands");
    }

    EntireDockerCommands cmds = new EntireDockerCommands(createBuilder.build(), startBuilder.build(), stopBuilder.build());
    return cmds;
  }

  private void printCommandParts(EntireDockerCommands cmds) {
    try {
      System.out.println(cmds.getSetOptionsString(DockerCommand.CREATE));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.CREATE));
      System.out.println(cmds.getSetArgsString(DockerCommand.CREATE));
      System.out.println("\n");
      System.out.println(cmds.getSetOptionsString(DockerCommand.START));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.START));
      System.out.println(cmds.getSetArgsString(DockerCommand.START));
      System.out.println("\n");
      System.out.println(cmds.getSetOptionsString(DockerCommand.STOP));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.STOP));
      System.out.println(cmds.getSetArgsString(DockerCommand.STOP));
    } catch (DockerCommandException e) {
      System.err.println("Error in printing docker command strings");
    }
  }

  @Test
  public void testFZookeeperDeploymentContext() {
    createZookeperContext(client,version.DOCKER_DEPL);
  }

  @Test
  public void testFZookeeperDeploymentContext_lifecycle() {
    createZookeperContext(client,version.LIFECYCLE);
  }

  @Test
  public void testGCassandraDeploymentContext() {
    createCassandraContext(client,version.DOCKER_DEPL);
  }

  @Test
  public void testGCassandraDeploymentContext_lifecycle() {
    createCassandraContext(client,version.LIFECYCLE);
  }

  @Test
  public void testHKafkaDeploymentContext() {
    createKafkaContext(client,version.DOCKER_DEPL);
  }

  @Test
  public void testHKafkaDeploymentContext_lifecycle() {
    createKafkaContext(client,version.LIFECYCLE);
  }

  @Test
  public void testIZookDeploy() {
    try {
      List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName);
      List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName);
      DockerComponent zookComp = buildDockerComponent(
      client,
      zookeeperComponent , zookeeperComponentId,
      inInfs,
      outInfs,
          new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return createDefaultLifecycleStore();
            }
          });
      DeploymentContext zookContext = createZookeperContext(client,version.DOCKER_DEPL);
      EntireDockerCommands cmds = buildEntireDockerCommands("zook");
      zookComp.setEntireDockerCommands(cmds);
      zookComp.setImageName("zookeeper");
      zookComp.setTag("3.4.12");
      zookId =
          client.deploy(zookContext, zookComp);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy zookeeper component");
    }
  }

  @Test
  public void testIZookDeploy_lifecycle() {
    try {
      List<InportInfo> inInfs = getInPortInfos(zookeeperInternalInportName_lifecycle);
      List<OutportInfo> outInfs = getOutPortInfos(zookeeperOutportName_lifecycle);
      DeployableComponent zookComp = buildDeployableComponent(
          client,
          zookeeperComponent_lifecycle, zookeeperComponentId_lifecycle,
          inInfs,
          outInfs,
          new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return createDefaultLifecycleStore();
            }
          });
      DeploymentContext zookContext = createZookeperContext(client,version.LIFECYCLE);
      zookId_lifecycle =
          client.deploy(zookContext, zookComp, OperatingSystem.UBUNTU_14_04, ContainerType.DOCKER);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy zookeeper component");
    }
  }

  @Test
  public void testJCassDeploy() {
    try {
      List<InportInfo> inInfs = getInPortInfos(cassandraInternalInportName);
      List<OutportInfo> outInfs = getOutPortInfos(cassandraOutportName);
      DockerComponent cassComp = buildDockerComponent(
          client,
          cassandraComponent , cassandraComponentId,
          inInfs,
          outInfs,
          new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return createDefaultLifecycleStore();
            }
          });
      DeploymentContext cassContext = createCassandraContext(client,version.DOCKER_DEPL);
      EntireDockerCommands cmds = buildEntireDockerCommands("cass");
      cassComp.setEntireDockerCommands(cmds);
      cassComp.setImageName("cassandra");
      try {
        cassComp.setDigestSHA256("7d6e2350f48133c7bdd3b6b3fba75e019e00466ef06dbb949de8e0f3f8e57750");
      } catch(NoSuchAlgorithmException nae) {
        System.err.println("Wrong algorithm for encoding the digest");
      } catch(UnsupportedEncodingException uee) {
         System.err.println("Wrong encoding used for encoding the digest");
      }
      cassId =
          client.deploy(cassContext, cassComp);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy cassandra component");
    }
  }

  @Test
  public void testJCassDeploy_lifecycle() {
    try {
      List<InportInfo> inInfs = getInPortInfos(cassandraInternalInportName_lifecycle);
      List<OutportInfo> outInfs = getOutPortInfos(cassandraOutportName_lifecycle);
      DeployableComponent cassComp = buildDeployableComponent(
          client,
          cassandraComponent_lifecycle, cassandraComponentId_lifecycle,
          inInfs,
          outInfs,
          new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return createDefaultLifecycleStore();
            }
          });
      DeploymentContext cassContext = createCassandraContext(client,version.LIFECYCLE);
      cassId_lifecycle =
          client.deploy(cassContext, cassComp, OperatingSystem.UBUNTU_14_04, ContainerType.DOCKER);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy cassandra component");
    }
  }

  @Test
  public void testKKafkDeploy() {
    try {
      List<InportInfo> inInfs = getInPortInfos(kafkaInternalInportName);
      List<OutportInfo> outInfs = getOutPortInfos(kafkaOutportName);
      DockerComponent kafkComp = buildDockerComponent(
          client,
          kafkaComponent , kafkaComponentId,
          inInfs,
          outInfs,
          new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return createDefaultLifecycleStore();
            }
          });
      DeploymentContext kafkContext = createKafkaContext(client,version.DOCKER_DEPL);
      EntireDockerCommands cmds = buildEntireDockerCommands("kafk");
      kafkComp.setEntireDockerCommands(cmds);
      kafkComp.setImageFolder("wurstmeister");
      kafkComp.setImageName("kafka");
      kafkId =
          client.deploy(kafkContext, kafkComp);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy kafka component");
    }
  }

  @Test
  public void testKKafkDeploy_lifecycle() {
    try {
      List<InportInfo> inInfs = getInPortInfos(kafkaInternalInportName_lifecycle);
      List<OutportInfo> outInfs = getOutPortInfos(kafkaOutportName_lifecycle);
      DeployableComponent kafkComp = buildDeployableComponent(
          client,
          kafkaComponent_lifecycle, kafkaComponentId_lifecycle,
          inInfs,
          outInfs,
          new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return createDefaultLifecycleStore();
            }
          });
      DeploymentContext kafkContext = createKafkaContext(client,version.LIFECYCLE);
      kafkId_lifecycle =
          client.deploy(kafkContext, kafkComp, OperatingSystem.UBUNTU_14_04, ContainerType.DOCKER);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy kafka component");
    }
  }

  @Test
  public void testLComponentStatus() {
    ContainerStatus zookStatus, cassStatus, kafkStatus;
    ContainerStatus zookStatus_lifecycle, cassStatus_lifecycle, kafkStatus_lifecycle;
    zookStatus = cassStatus = kafkStatus = UNKNOWN;
    zookStatus_lifecycle = cassStatus_lifecycle  = kafkStatus_lifecycle  = UNKNOWN;
    do {
      try {
        zookStatus = client.getComponentContainerStatus(zookId, publicIp);
        cassStatus = client.getComponentContainerStatus(cassId, publicIp);
        kafkStatus = client.getComponentContainerStatus(kafkId, publicIp);
        zookStatus_lifecycle  = client.getComponentContainerStatus(zookId_lifecycle , publicIp);
        cassStatus_lifecycle  = client.getComponentContainerStatus(cassId_lifecycle , publicIp);
        kafkStatus_lifecycle  = client.getComponentContainerStatus(kafkId_lifecycle , publicIp);
        System.out.println("ZOOKEEPER STATUS:" + zookStatus);
        System.out.println("CASSANDRA STATUS:" + cassStatus);
        System.out.println("KAFKA STATUS:" + kafkStatus);
        System.out.println("ZOOKEEPER STATUS (LIFECYCLE):" + zookStatus_lifecycle );
        System.out.println("CASSANDRA STATUS (LIFECYCLE):" + cassStatus_lifecycle );
        System.out.println("KAFKA STATUS(LIFECYCLE):" + kafkStatus_lifecycle );
        sleep(5000);
      } catch (DeploymentException ex) {
        System.err.println("Exception during deployment!");
      } catch (InterruptedException ex) {
        System.err.println("Interrupted!");
      }
    } while (zookStatus != READY || cassStatus != READY || kafkStatus != READY ||
        zookStatus_lifecycle != READY || cassStatus_lifecycle != READY || kafkStatus_lifecycle != READY);
  }

  @Test
  public void testMStopContainers() {
    try {
      client.undeploy(zookId, ContainerType.DOCKER);
      client.undeploy(cassId, ContainerType.DOCKER);
      client.undeploy(kafkId, ContainerType.DOCKER);
      client.undeploy(zookId_lifecycle, ContainerType.DOCKER);
      client.undeploy(cassId_lifecycle, ContainerType.DOCKER);
      client.undeploy(kafkId_lifecycle, ContainerType.DOCKER);
    } catch (DeploymentException ex) {
      System.err.println("Exception during deployment!");
    }
  }

  @Test
  public void testNHostEnvironment() {
    try {
      System.out.println(client.getHostEnv());
    } catch (DeploymentException ex) {
      System.err.println("Exception during deployment!");
    }
  }
}