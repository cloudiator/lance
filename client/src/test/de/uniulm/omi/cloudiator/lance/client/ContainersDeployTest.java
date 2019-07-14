package de.uniulm.omi.cloudiator.lance.client;

import static java.lang.Thread.sleep;

import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;
import de.uniulm.omi.cloudiator.domain.OperatingSystem;

import de.uniulm.omi.cloudiator.lance.application.component.*;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.FixMethodOrder;
import org.junit.Test;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import java.util.concurrent.Callable;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContainersDeployTest extends BaseTests {

  @Test
  public void testABaseOne() {
    testClientGetter();
    testRegisterBase();
  }

  @Test
  public void testBRegister() {
    try {
      TestUtils.client.registerComponentForApplicationInstance(TestUtils.appInstanceId, TestUtils.zookeeperComponentIdDocker_lifecycle);
      TestUtils.client.registerComponentForApplicationInstance(TestUtils.appInstanceId, TestUtils.zookeeperComponentIdDocker);
      TestUtils.client.registerComponentForApplicationInstance(TestUtils.appInstanceId, TestUtils.kafkaComponentIdPlain_lifecycle);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  @Test
  public void testCBaseTwo() {
    testCompDescriptions_lifecycle(TestUtils.zookeeperInternalInportNameDocker_lifecycle, TestUtils.zookeeperOutportNameDocker_lifecycle,
        TestUtils.zookeeperComponentDocker_lifecycle, TestUtils.zookeeperComponentIdDocker_lifecycle,
        new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return TestUtils.createDefaultLifecycleStore();
          }
        });
    testCompDescriptionsDocker(TestUtils.zookeeperInternalInportNameDocker, TestUtils.zookeeperOutportNameDocker,
        TestUtils.zookeeperComponentDocker, TestUtils.zookeeperComponentIdDocker, TestUtils.tag);
    testCompDescriptions_lifecycle(TestUtils.kafkaInternalInportNamePlain_lifecycle, TestUtils.kafkaOutportNamePlain_lifecycle,
        TestUtils.kafkaComponentPlain_lifecycle, TestUtils.kafkaComponentIdPlain_lifecycle,
        new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return TestUtils.createKafkaLifecycleStore();
          }
        });

    testDeploymentContext(TestUtils.zookeeperInternalInportNameDocker_lifecycle, TestUtils.defaultZookeeperInternalInportDocker_lifecycle);
    testDeploymentContext(TestUtils.zookeeperInternalInportNameDocker, TestUtils.defaultZookeeperInternalInportDocker);
    testDeploymentContext(TestUtils.kafkaInternalInportNamePlain_lifecycle, TestUtils.defaultKafkaInternalInportPlain_lifecycle);
  }

  @Test
  public void testDZookDeployDocker_lifecycle() {
    try {
      DeployableComponent zookComp =
          testCompDescriptions_lifecycle(TestUtils.zookeeperInternalInportNameDocker_lifecycle, TestUtils.zookeeperOutportNameDocker_lifecycle,
              TestUtils.zookeeperComponentDocker_lifecycle, TestUtils.zookeeperComponentIdDocker_lifecycle,
              new Callable<LifecycleStore>() {
                public LifecycleStore call() {
                  return TestUtils.createDefaultLifecycleStore();
                }
              });
      DeploymentContext zookContext =
          testDeploymentContext(TestUtils.zookeeperInternalInportNameDocker_lifecycle, TestUtils.defaultZookeeperInternalInportDocker_lifecycle);
      OperatingSystemImpl os = new OperatingSystemImpl(
          OperatingSystemFamily.UBUNTU,
          OperatingSystemArchitecture.AMD64,
          OperatingSystemVersions.of(1604,null));
      TestUtils.zookIdDocker_lifecycle =
          TestUtils.client.deploy(zookContext, zookComp, os, ContainerType.DOCKER);
      boolean isReady = false;
      do {
        System.out.println("zook lifecycle-docker not ready");
        sleep(50);
        isReady = TestUtils.client.isReady(TestUtils.zookIdDocker_lifecycle);
      } while (isReady != true);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy docker-lifecycle component");
    } catch (InterruptedException e) {
      System.err.println("Interrupted!");
    }
  }

  @Test
  public void testDZookDeployDocker() {
    try {
      DockerComponent zookComp =
          testCompDescriptionsDocker(TestUtils.zookeeperInternalInportNameDocker, TestUtils.zookeeperOutportNameDocker,
              TestUtils.zookeeperComponentDocker, TestUtils.zookeeperComponentIdDocker, TestUtils.tag);
      DeploymentContext zookContext =
          testDeploymentContext(TestUtils.zookeeperInternalInportNameDocker, TestUtils.defaultZookeeperInternalInportDocker);
      TestUtils.zookIdDocker =
          TestUtils.client.deploy(zookContext, zookComp);
      boolean isReady = false;
      do {
        System.out.println("zook docker not ready");
        sleep(50);
        isReady = TestUtils.client.isReady(TestUtils.zookIdDocker);
      } while (isReady != true);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy docker component");
    } catch (InterruptedException e) {
      System.err.println("Interrupted!");
    }
  }

  @Test
  public void testDKafkaDeployPlain_lifecycle() {
    try {
      DeployableComponent kafkaComp =
          testCompDescriptions_lifecycle(TestUtils.kafkaInternalInportNamePlain_lifecycle, TestUtils.kafkaOutportNamePlain_lifecycle,
              TestUtils.kafkaComponentPlain_lifecycle, TestUtils.kafkaComponentIdPlain_lifecycle,
              new Callable<LifecycleStore>() {
                public LifecycleStore call() {
                  return TestUtils.createKafkaLifecycleStore();
                }
              });
      DeploymentContext kafkaContext =
          testDeploymentContext(TestUtils.kafkaInternalInportNamePlain_lifecycle, TestUtils.defaultKafkaInternalInportPlain_lifecycle);
      OperatingSystemImpl os = new OperatingSystemImpl(
          OperatingSystemFamily.UBUNTU,
          OperatingSystemArchitecture.AMD64,
          OperatingSystemVersions.of(1604,null));
      TestUtils.kafkaIdPlain_lifecycle =
          TestUtils.client.deploy(kafkaContext, kafkaComp, os, ContainerType.PLAIN);
      boolean isReady = false;
      do {
        System.out.println("kafka lifecycle-plain not ready");
        sleep(50);
        isReady = TestUtils.client.isReady(TestUtils.kafkaIdPlain_lifecycle);
      } while (isReady != true);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy plain-lifecycle component");
    } catch (InterruptedException e) {
      System.err.println("Interrupted!");
    }
  }

  private static List<TestUtils.InportInfo> getInPortInfos(String internalInportName) {
    List<TestUtils.InportInfo> inInfs = new ArrayList<>();
    TestUtils.InportInfo inInf =
        new TestUtils.InportInfo(internalInportName, PortProperties.PortType.INTERNAL_PORT, 2, 3888);
    inInfs.add(inInf);

    return inInfs;
  }

  private static List<TestUtils.OutportInfo> getOutPortInfos(String outPortName) {
    List<TestUtils.OutportInfo> outInfs = new ArrayList<>();
    /* TestUtils.OutportInfo outInf =
        new TestUtils.OutportInfo(
            outPortName,
            DeploymentHelper.getEmptyPortUpdateHandler(),
            1,
            OutPort.NO_SINKS);
    outInfs.add(outInf);*/

    return outInfs;
  }

  private static DeploymentContext createContext (String internalInportName, int defaultInternalInport) {
    DeploymentContext context =
        TestUtils.client.initDeploymentContext(TestUtils.applicationId, TestUtils.appInstanceId);
    // saying that we want to use the default port as the actual port number //
    context.setProperty(
        internalInportName ,(Object) defaultInternalInport, InPort.class);

    return context;
  }

  private static DockerComponent testCompDescriptionsDocker(String internalInportName, String outportName,
      String componentName, ComponentId compId, String tag) {
    List<TestUtils.InportInfo> inInfs = getInPortInfos(internalInportName);
    List<TestUtils.OutportInfo> outInfs = getOutPortInfos(outportName);
    return buildDockerComponent(componentName, compId, inInfs, outInfs, tag);
  }

  private static DeployableComponent testCompDescriptions_lifecycle(String internalInportName, String outportName,
      String componentName, ComponentId compId, Callable<LifecycleStore> storeCreateFct) {
    List<TestUtils.InportInfo> inInfs = getInPortInfos(internalInportName);
    List<TestUtils.OutportInfo> outInfs = getOutPortInfos(outportName);
    return TestUtils.buildDeployableComponent(componentName, compId, inInfs, outInfs, storeCreateFct);
  }

  private static DeploymentContext testDeploymentContext(String internalInportName, int defaultInternalInport ) {
    return createContext(internalInportName, defaultInternalInport);
  }

  private static DockerComponent buildDockerComponent(
      String compName,
      ComponentId id,
      List<TestUtils.InportInfo> inInfs,
      List<TestUtils.OutportInfo> outInfs,
      String tag) {

    DockerComponent.Builder builder = buildDockerComponentBuilder(compName, id, inInfs, outInfs,"", tag);
    DockerComponent comp = builder.build();
    return comp;
  }

  private static DockerComponent.Builder buildDockerComponentBuilder(
      String compName,
      ComponentId id,
      List<TestUtils.InportInfo> inInfs,
      List<TestUtils.OutportInfo> outInfs,
      String imageFolder,
      String tag) {
    DockerComponent.Builder builder = new DockerComponent.Builder(buildEntireDockerCommands(), TestUtils.imageName);
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

  private static EntireDockerCommands buildEntireDockerCommands() {
    Random rand = new Random();
    EntireDockerCommands.Builder cmdsBuilder = new EntireDockerCommands.Builder();
    try {
      Map<Option,List<String>> createOptionMap = new HashMap<>();
      createOptionMap.put(Option.ENVIRONMENT, Arrays.asList("foo=bar","john=doe"));
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
}
