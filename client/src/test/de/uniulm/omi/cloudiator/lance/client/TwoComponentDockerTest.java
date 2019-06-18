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

import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters.ProvidedPortContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.util.ArrayList;
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
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortLinkage;
import java.util.concurrent.Callable;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.lang.Exception;

// Important: Java-re, docker daemon and Lance(Server) must be installed on the VM and related ports
// must be opened on the vm
// Install etcd on the vm via install_etcd.sh which can be found in the pinned installation
// repository

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TwoComponentDockerTest {

  private enum cont_type { WORDPRESS, MARIADB };

  private static ApplicationId applicationId;
  private static ApplicationInstanceId appInstanceId;

  private static String wordpressComponentUp, mariadbComponentDown, wordpressComponent_lifecycle;
  private static String wordpressInternalInportNameUp, mariadbInternalInportNameDown, wordpressInternalInportName_lifecycle;
  private static String wordpressOutportNameUp, wordpressOutportName_lifecycle;
  private static String imageNameUp, imageNameDown, imageName_remote;
  private static ComponentId wordpressComponentIdUp, mariadbComponentIdDown;
  private static int defaultWordpressInternalInportUp, defaultMariadbInternalInportDown, defaultWordpressInternalInport_lifecycle;
  private static ComponentInstanceId wordpressIdUp, mariadbIdDown;
  // adjust
  private static String publicIp = "x.x.x.x";
  private static LifecycleClient client;

  @BeforeClass
  public static void configureAppContext() {
    applicationId = new ApplicationId();
    appInstanceId = new ApplicationInstanceId();

    Random rand = new Random();
    wordpressComponentUp = "wordpressUp";
    mariadbComponentDown = "mariadbDown";
    wordpressComponent_lifecycle = "wordpress_lifecycle";
    wordpressInternalInportNameUp = "WORDPRESS_INT_INP_UP";
    mariadbInternalInportNameDown = "MARIADB_INT_INP_DOWN";
    wordpressInternalInportName_lifecycle = "WORDPRESS_INT_INP_LIFECYCLE";
    wordpressOutportNameUp = "WORDPRESS_OUT";
    wordpressOutportName_lifecycle = "WORDPRESS_OUT_LIFECYCLE";
    imageNameUp = "wordpress";
    imageNameDown = "mariadb";
    imageName_remote = "mariadb";
    wordpressComponentIdUp = new ComponentId();
    mariadbComponentIdDown = new ComponentId();
    defaultWordpressInternalInportUp = 80;
    // downstream-port must be opened on host
    defaultMariadbInternalInportDown =  3306;
    defaultWordpressInternalInport_lifecycle = (rand.nextInt(65563) + 1);

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
      String tag,
      String iName,
      cont_type type) {
    DockerComponent.Builder builder;
    if (type == cont_type.WORDPRESS) {
      builder = new DockerComponent.Builder(buildEntireWordpressCommands(), iName);
    } else {
      builder = new DockerComponent.Builder(buildEntireMariadbCommands(), iName);
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
      String tag,
      String iName,
      cont_type type) {

    return buildDockerComponentBuilder(client, compName, id, inInfs, outInfs, "", tag, iName, type);
  }

  private DockerComponent buildDockerComponent(
      LifecycleClient client,
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      String tag,
      String iName,
      cont_type type) {

    DockerComponent.Builder builder = buildDockerComponentBuilder(client, compName, id, inInfs, outInfs, tag, iName, type);
    DockerComponent comp = builder.build();
    return comp;
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
      client.registerComponentForApplicationInstance(appInstanceId, wordpressComponentIdUp);
      client.registerComponentForApplicationInstance(appInstanceId, mariadbComponentIdDown);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  @Test
  public void testCWordpressCompDescriptionUp() {
    List<InportInfo> inInfs = getInPortInfos(wordpressInternalInportNameUp, defaultWordpressInternalInportUp);
    List<OutportInfo> outInfs = getOutPortInfos(wordpressOutportNameUp);
    buildDockerComponent( client, wordpressComponentUp, wordpressComponentIdUp, inInfs, outInfs, "latest", imageNameUp, cont_type.WORDPRESS);
  }

  @Test
  public void testDMariadbCompDescriptionDown() {
    List<InportInfo> inInfs = getInPortInfos(mariadbInternalInportNameDown, defaultMariadbInternalInportDown);
    buildDockerComponent( client, mariadbComponentDown, mariadbComponentIdDown, inInfs, getOutPortInfos(), "latest", imageNameDown, cont_type.MARIADB);
  }

  @Test
  public void testEWordpressCompDescriptions_lifecycle() {
    List<InportInfo> inInfs = getInPortInfos(wordpressInternalInportName_lifecycle, defaultWordpressInternalInport_lifecycle);
    List<OutportInfo> outInfs = getOutPortInfos(wordpressOutportName_lifecycle);
    buildDeployableComponent( client, wordpressComponent_lifecycle, mariadbComponentIdDown,
        inInfs, outInfs, new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return createDefaultLifecycleStore();
          }
        });
  }

  private static List<InportInfo> getInPortInfos(String internalInportName, int internalInport) {
    List<InportInfo> inInfs = new ArrayList<>();
    InportInfo inInf =
        new InportInfo(internalInportName, PortProperties.PortType.INTERNAL_PORT, 1, internalInport);
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
            1);
    outInfs.add(outInf);

    return outInfs;
  }

  private static List<OutportInfo> getOutPortInfos() {
    List<OutportInfo> outInfs = new ArrayList<>();

    return outInfs;
  }

  private static DeploymentContext createWordpressContextUpStream() {
    DeploymentContext wordpress_context =
        client.initDeploymentContext(applicationId, appInstanceId);
    wordpress_context.setProperty(
        wordpressInternalInportNameUp,(Object) defaultWordpressInternalInportUp, InPort.class);
    wordpress_context.setProperty(
        wordpressOutportNameUp,
        (Object)
            new PortReference(mariadbComponentIdDown, mariadbInternalInportNameDown , PortLinkage.ALL),
        OutPort.class);
    return wordpress_context;
  }

  private static DeploymentContext createMariadbContextDownStream() {
    DeploymentContext wordpress_context =
        client.initDeploymentContext(applicationId, appInstanceId);
    wordpress_context.setProperty(
        mariadbInternalInportNameDown,(Object) defaultMariadbInternalInportDown, InPort.class);
    return wordpress_context;
  }

  EntireDockerCommands buildEntireWordpressCommands() {
    EntireDockerCommands.Builder cmdsBuilder = new EntireDockerCommands.Builder();
    try {
      Map<Option,List<String>> createOptionMap = new HashMap<>();
      createOptionMap.put(Option.ENVIRONMENT, Arrays.asList(
          "foo=bar",
          "john=doe",
          "WORDPRESS_DB_USER=user",
          "WORDPRESS_DB_PASSWORD=testpwd",
          "WORDPRESS_DB_HOST=$PUBLIC_WORDPRESS_OUT",
          "WORDPRESS_DB_NAME=testdb"
          ));
      String n = Integer.toString(defaultWordpressInternalInportUp);
      createOptionMap.put(Option.PORT, new ArrayList<>(Arrays.asList(n+":"+n)));
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

  EntireDockerCommands buildEntireMariadbCommands() {
    EntireDockerCommands.Builder cmdsBuilder = new EntireDockerCommands.Builder();
    try {
      Map<Option,List<String>> createOptionMap = new HashMap<>();
      createOptionMap.put(Option.ENVIRONMENT, Arrays.asList(
          "MYSQL_DATABASE=testdb",
          "MYSQL_PASSWORD=testpwd",
          "MYSQL_USER=user",
          "MYSQL_ROOT_PASSWORD=admin"
      ));
      String n = Integer.toString(3306);
      createOptionMap.put(Option.PORT, new ArrayList<>(Arrays.asList(n+":"+n)));
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


  @Test
  public void testFWordpressDeploymentContextUp() {
    createWordpressContextUpStream();
  }

  @Test
  public void testGMariadbDeploymentContextDown() {
    createMariadbContextDownStream();
  }

  @Ignore
  @Test
  public void testHWordpressDeploymentContext_lifecycle() {
    //todo: implement
  }

  @Test
  public void testIInsertExtDeplContext() {
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

  @Test
  public void testKDeploy() {
    try {
      List<InportInfo> inInfsUp =
          getInPortInfos(wordpressInternalInportNameUp, defaultWordpressInternalInportUp);
      List<OutportInfo> outInfsUp = getOutPortInfos(wordpressOutportNameUp);
      DockerComponent wordpressCompUp =
          buildDockerComponent( client, wordpressComponentUp, wordpressComponentIdUp, inInfsUp, outInfsUp, "latest", imageNameUp, cont_type.WORDPRESS);
      DeploymentContext wordpressContextUp = createWordpressContextUpStream();


      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            wordpressIdUp = client.deploy(wordpressContextUp, wordpressCompUp);
          } catch (DeploymentException e) {
            System.err.println("Exception during deployment!");
          };
        }
      });
      t.start();
      sleep(3000);

      List<InportInfo> inInfsDown =
          getInPortInfos(mariadbInternalInportNameDown, defaultMariadbInternalInportDown);
      DockerComponent mariadbCompDown =
          buildDockerComponent( client, mariadbComponentDown, mariadbComponentIdDown, inInfsDown, getOutPortInfos(), "latest", imageNameDown, cont_type.MARIADB);
      DeploymentContext mariadbContextDown = createMariadbContextDownStream();
      mariadbIdDown = client.deploy(mariadbContextDown, mariadbCompDown);

      t.join();
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy docker app");
    } catch (InterruptedException e) {
      System.err.println("Got interrupted");
    }
  }

  @Test
  public void testNComponentStatus() {
    ContainerStatus wordpressStatusUp, mariadbStatusDown;
    wordpressStatusUp = mariadbStatusDown = UNKNOWN;
    do {
      try {
        wordpressStatusUp = client.getComponentContainerStatus(wordpressIdUp, publicIp);
        mariadbStatusDown = client.getComponentContainerStatus(mariadbIdDown, publicIp);
        System.out.println("WORPRESS_UP STATUS:" + wordpressStatusUp);
        System.out.println("MARIADB_DOWN STATUS:" + mariadbStatusDown);
        sleep(5000);
      } catch (DeploymentException ex) {
        System.err.println("Exception during deployment!");
      } catch (InterruptedException ex) {
        System.err.println("Interrupted!");
      }
    } while (wordpressStatusUp != READY || mariadbStatusDown != READY);
  }

  @Test
  public void testOStopContainers() {
    try {
      sleep(2000);
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            client.undeploy(wordpressIdUp, false);
          } catch (DeploymentException e) {
            System.err.println("Exception during deployment!");
          };
        }
      });
      t.start();
      sleep(150000);
      client.undeploy(mariadbIdDown, false);
      t.join();
    } catch (DeploymentException ex) {
      System.err.println("Exception during deployment!");
    } catch (InterruptedException ex) {
      System.err.println("Interrupted!");
    }
  }
}