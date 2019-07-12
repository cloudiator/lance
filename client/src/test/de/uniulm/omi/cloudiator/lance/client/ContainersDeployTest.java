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
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import org.junit.Test;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import java.util.concurrent.Callable;

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
      OperatingSystem os = new OperatingSystemImpl(
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
      OperatingSystem os = new OperatingSystemImpl(
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
}
