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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import java.util.HashSet;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.component.*;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
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
public class TwoComponentDockerTest extends BaseTests {

  private enum cont_type { WORDPRESS, MARIADB };

  private static ApplicationInstanceId wrongAppInstanceId;

  private static String wordpressComponentUp, mariadbComponentDown;
  private static String wordpressInternalInportNameUp, mariadbInternalInportNameDown;
  private static String wordpressOutportNameUp;
  private static String imageNameUp, imageNameDown;
  private static ComponentId wordpressComponentIdUp, mariadbComponentIdDown;
  private static int defaultWordpressInternalInportUp, defaultMariadbInternalInportDown;
  private static ComponentInstanceId wordpressIdUp, mariadbIdDown;
  // adjust
  private static String publicIp = "x.x.x.x";
  private static String wrongIp = "x.x.x.x";
  private static LifecycleClient client;

  @BeforeClass
  public static void configureAppContext() {
    //should be different
    wrongAppInstanceId = new ApplicationInstanceId();

    Random rand = new Random();
    wordpressComponentUp = "wordpressUp";
    mariadbComponentDown = "mariadbDown";
    wordpressInternalInportNameUp = "WORDPRESS_INT_INP_UP";
    mariadbInternalInportNameDown = "MARIADB_INT_INP_DOWN";
    wordpressOutportNameUp = "WORDPRESS_OUT";
    imageNameUp = "wordpress";
    imageNameDown = "mariadb";
    wordpressComponentIdUp = new ComponentId();
    mariadbComponentIdDown = new ComponentId();
    defaultWordpressInternalInportUp = 80;
    // downstream-port must be opened on host
    defaultMariadbInternalInportDown =  3306;

    System.setProperty("lca.client.config.registry", "etcdregistry");
    // adjust
    System.setProperty("lca.client.config.registry.etcd.hosts", "x.x.x.x:4001");
  }

  @Test
  public void testABaseOne() {
    testClientGetter();
    testRegisterBase();
  }

  @Ignore
  @Test(expected = RemoteException.class)
  public void testAClientGetterWrongIp() throws RemoteException {
    try {
      client = LifecycleClient.getClient(wrongIp);
    } catch (NotBoundException ex) {
      System.err.println("Socket not bound");
    }
  }

  @Test
  public void testBRegister() {
    try {
      client.registerComponentForApplicationInstance(TestUtils.appInstanceId, wordpressComponentIdUp);
      client.registerComponentForApplicationInstance(TestUtils.appInstanceId, mariadbComponentIdDown);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  // EtcdImpl does not throw an exception if not yet existant appId is used -> todo: refactor?
  @Ignore
  @Test(expected = RegistrationException.class)
  public void testBRegisterCompWrongAppInstId() throws RegistrationException {
    client.registerComponentForApplicationInstance(wrongAppInstanceId, wordpressComponentIdUp);
  }

  @Test
  public void testCWordpressCompDescriptionUp() {
    List<TestUtils.InportInfo> inInfs = getInPortInfos(wordpressInternalInportNameUp, defaultWordpressInternalInportUp);
    List<TestUtils.OutportInfo> outInfs = getOutPortInfos(wordpressOutportNameUp);
    DockerComponent comp = buildDockerComponent( wordpressComponentUp, wordpressComponentIdUp, inInfs, outInfs, "latest", imageNameUp, cont_type.WORDPRESS);
    assertEquals(comp.getName(),wordpressComponentUp);
    assertEquals(comp.getComponentId(),wordpressComponentIdUp);
    assertTrue(listEqualsIgnoreOrder(comp.getExposedPorts().stream()
        .map(InPort::getPortName)
        .collect(Collectors.toList())
        ,inInfs.stream()
        .map(TestUtils.InportInfo::getInportName)
        .collect(Collectors.toList())));
    assertTrue(listEqualsIgnoreOrder(comp.getDownstreamPorts().stream()
            .map(OutPort::getName)
            .collect(Collectors.toList())
        ,outInfs.stream()
            .map(TestUtils.OutportInfo::getOutportName)
            .collect(Collectors.toList())));
    assertTrue(listEqualsIgnoreOrder(comp.getDownstreamPorts().stream()
            .map(OutPort::getLowerBound)
            .collect(Collectors.toList())
        ,outInfs.stream()
            .map(TestUtils.OutportInfo::getMin)
            .collect(Collectors.toList())));
    assertEquals("latest",comp.getTag());
    assertEquals(comp.getImageName(),imageNameUp);
  }

  @Test
  public void testDMariadbCompDescriptionDown() {
    List<TestUtils.InportInfo> inInfs = getInPortInfos(mariadbInternalInportNameDown, defaultMariadbInternalInportDown);
    DockerComponent comp = buildDockerComponent(mariadbComponentDown, mariadbComponentIdDown, inInfs, getOutPortInfos(), "latest", imageNameDown, cont_type.MARIADB);
    assertEquals(comp.getName(),mariadbComponentDown);
    assertEquals(comp.getComponentId(),mariadbComponentIdDown);
    assertTrue(listEqualsIgnoreOrder(comp.getExposedPorts().stream()
            .map(InPort::getPortName)
            .collect(Collectors.toList())
        ,inInfs.stream()
            .map(TestUtils.InportInfo::getInportName)
            .collect(Collectors.toList())));
    assertEquals("latest",comp.getTag());
    assertEquals(comp.getImageName(),imageNameDown);
  }

  @Test
  public void testKDeploy() {
    try {
      DeploymentTypesWrapper wrapperUp = setUpWordpressUpDeploy();
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            wordpressIdUp = client.deploy(wrapperUp.context, wrapperUp.comp);
          } catch (DeploymentException e) {
            System.err.println("Exception during deployment!");
          };
        }
      });
      t.start();
      sleep(3000);

      DeploymentTypesWrapper wrapperDown = setUpMariadbDownDeploy();
      mariadbIdDown = client.deploy(wrapperDown.context, wrapperDown.comp);

      t.join();
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy docker app");
    } catch (InterruptedException e) {
      System.err.println("Got interrupted");
    }
  }

  @Test(expected = DeploymentException.class)
  public void testKDeployFail() throws DeploymentException {
    client.deploy(createWordpressContextUpStream(), buildDockerComponent(
        "fail",
        new ComponentId(),
        new ArrayList<>(),
        new ArrayList<>(),
        "latest",
        "failimage",
       cont_type.WORDPRESS
    ));
  }

  @Test(timeout=120000)
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

  @Test(timeout=300000)
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

  private DeploymentTypesWrapper setUpWordpressUpDeploy() {
    List<TestUtils.InportInfo> inInfsUp =
        getInPortInfos(wordpressInternalInportNameUp, defaultWordpressInternalInportUp);
    List<TestUtils.OutportInfo> outInfsUp = getOutPortInfos(wordpressOutportNameUp);
    DockerComponent wordpressCompUp =
        buildDockerComponent(wordpressComponentUp, wordpressComponentIdUp, inInfsUp, outInfsUp, "latest", imageNameUp, cont_type.WORDPRESS);
    DeploymentContext wordpressContextUp = createWordpressContextUpStream();

    DeploymentTypesWrapper wrapper = new DeploymentTypesWrapper(wordpressCompUp, wordpressContextUp);
    return wrapper;
  }

  private DeploymentTypesWrapper setUpMariadbDownDeploy() {
    List<TestUtils.InportInfo> inInfsDown =
        getInPortInfos(mariadbInternalInportNameDown, defaultMariadbInternalInportDown);
    DockerComponent mariadbCompDown =
        buildDockerComponent(mariadbComponentDown, mariadbComponentIdDown, inInfsDown, getOutPortInfos(), "latest", imageNameDown, cont_type.MARIADB);
    DeploymentContext mariadbContextDown = createMariadbContextDownStream();

    DeploymentTypesWrapper wrapper = new DeploymentTypesWrapper(mariadbCompDown, mariadbContextDown);
    return wrapper;
  }

  private DockerComponent.Builder buildDockerComponentBuilder(
      String compName,
      ComponentId id,
      List<TestUtils.InportInfo> inInfs,
      List<TestUtils.OutportInfo> outInfs,
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
      String compName,
      ComponentId id,
      List<TestUtils.InportInfo> inInfs,
      List<TestUtils.OutportInfo> outInfs,
      String tag,
      String iName,
      cont_type type) {

    return buildDockerComponentBuilder(compName, id, inInfs, outInfs, "", tag, iName, type);
  }

  private DockerComponent buildDockerComponent(
      String compName,
      ComponentId id,
      List<TestUtils.InportInfo> inInfs,
      List<TestUtils.OutportInfo> outInfs,
      String tag,
      String iName,
      cont_type type) {

    DockerComponent.Builder builder = buildDockerComponentBuilder(compName, id, inInfs, outInfs, tag, iName, type);
    DockerComponent comp = builder.build();
    return comp;
  }

  static class DeploymentTypesWrapper {
    public final DockerComponent comp;
    public final DeploymentContext context;

    DeploymentTypesWrapper(DockerComponent comp, DeploymentContext context) {
      this.comp = comp;
      this.context = context;
    }
  }


  public static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
    return new HashSet<>(list1).equals(new HashSet<>(list2));
  }

  private static List<TestUtils.InportInfo> getInPortInfos(String internalInportName, int internalInport) {
    List<TestUtils.InportInfo> inInfs = new ArrayList<>();
    TestUtils.InportInfo inInf =
        new TestUtils.InportInfo(internalInportName, PortProperties.PortType.INTERNAL_PORT, 1, internalInport);
    inInfs.add(inInf);

    return inInfs;
  }

  private static List<TestUtils.OutportInfo> getOutPortInfos(String outPortName) {
    List<TestUtils.OutportInfo> outInfs = new ArrayList<>();
    TestUtils.OutportInfo outInf =
        new TestUtils.OutportInfo(
            outPortName,
            DeploymentHelper.getEmptyPortUpdateHandler(),
            1,
            1);
    outInfs.add(outInf);

    return outInfs;
  }

  private static List<TestUtils.OutportInfo> getOutPortInfos() {
    List<TestUtils.OutportInfo> outInfs = new ArrayList<>();

    return outInfs;
  }

  private static DeploymentContext createWordpressContextUpStream() {
    DeploymentContext wordpress_context =
        client.initDeploymentContext(TestUtils.applicationId, TestUtils.appInstanceId);
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
        client.initDeploymentContext(TestUtils.applicationId, TestUtils.appInstanceId);
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

}