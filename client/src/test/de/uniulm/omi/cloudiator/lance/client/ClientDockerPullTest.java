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
import de.uniulm.omi.cloudiator.lance.client.TestUtils.OutportInfo;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters;
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters.ProvidedPortContext;
import de.uniulm.omi.cloudiator.domain.OperatingSystem;

import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import java.util.ArrayList;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.component.*;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import java.util.concurrent.Callable;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

// Important: Java-re, docker daemon and Lance(Server) must be installed on the VM and related ports
// must be opened on the vm
// Install etcd on the vm via install_etcd.sh which can be found in the pinned installation
// repository

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientDockerPullTest {

  @Test
  public void testAClientGetter() {
    try {
      TestUtils.client = LifecycleClient.getClient(TestUtils.publicIp);
    } catch (RemoteException ex) {
      System.err.println("Server not reachable");
    } catch (NotBoundException ex) {
      System.err.println("Socket not bound");
    }
  }

  @Test
  public void testBRegister() {
    try {
      TestUtils.client.registerApplicationInstance(TestUtils.appInstanceId, TestUtils.applicationId);
      TestUtils.client.registerComponentForApplicationInstance(TestUtils.appInstanceId, TestUtils.zookeeperComponentId);
      TestUtils.client.registerComponentForApplicationInstance(TestUtils.appInstanceId, TestUtils.zookeeperComponentId_lifecycle);
      TestUtils.client.registerComponentForApplicationInstance(TestUtils.appInstanceId, TestUtils.rubyComponentId_remote);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  @Test
  public void testCZookCompDescriptions() {
    List<TestUtils.InportInfo> inInfs = TestUtils.getInPortInfos(TestUtils.zookeeperInternalInportName);
    List<TestUtils.OutportInfo> outInfs = TestUtils.getOutPortInfos(TestUtils.zookeeperOutportName);
    TestUtils.buildDockerComponent(TestUtils.client, TestUtils.zookeeperComponent, TestUtils.zookeeperComponentId, inInfs, outInfs, "3.4.12", true, false);
  }

  @Test
  public void testCZookCompDescriptions_lifecycle() {
    List<TestUtils.InportInfo> inInfs = TestUtils.getInPortInfos(TestUtils.zookeeperInternalInportName_lifecycle);
    List<OutportInfo> outInfs = TestUtils.getOutPortInfos(TestUtils.zookeeperOutportName_lifecycle);
    TestUtils.buildDeployableComponent(TestUtils.client, TestUtils.zookeeperComponent_lifecycle, TestUtils.zookeeperComponentId_lifecycle,
        inInfs, outInfs, new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return TestUtils.createDefaultLifecycleStore();
          }
        });
  }

  @Test
  public void testFZookeeperDeploymentContext() {
    TestUtils.createZookeperContext(TestUtils.client, TestUtils.zookeeperComponentId_lifecycle, TestUtils.zookeeperInternalInportName_lifecycle, TestUtils.version.DOCKER );
  }

  @Test
  public void testFZookeeperDeploymentContext_lifecycle() {
    TestUtils.createZookeperContext(TestUtils.client, TestUtils.zookeeperComponentId, TestUtils.zookeeperInternalInportName, TestUtils.version.LIFECYCLE);
  }

  @Test
  public void testGLInsertExtDeplContext() {
    ExternalContextParameters.ProvidedPortContext inpC = new ProvidedPortContext("sparkJob1Port",9999);
    ExternalContextParameters.Builder builder = new ExternalContextParameters.Builder();
    builder.taskName("sparkJob1");
    builder.appId(TestUtils.appInstanceId);
    builder.compId(new ComponentId());
    builder.compInstId(new ComponentInstanceId());
    builder.pubIp(TestUtils.publicIp);
    builder.providedPortContext(inpC);
    builder.contStatus(ContainerStatus.READY);
    builder.compInstType(LifecycleHandlerType.START);

    ExternalContextParameters params = builder.build();

    try {
      TestUtils.client.injectExternalDeploymentContext(params);
    } catch (DeploymentException e) {
      System.err.println("Couldn't inject ExtContext");
    }
  }

  @Ignore
  @Test
  public void testHRemoteDockerComponent() {
    try {
      DeploymentContext dummyContext =
          TestUtils.client.initDeploymentContext(TestUtils.applicationId, TestUtils.appInstanceId);
      List<TestUtils.InportInfo> dummyInInfs = new ArrayList<>();
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
      RemoteDockerComponent rDockerComponent = TestUtils.buildRemoteDockerComponent( TestUtils.client, TestUtils.rubyComponent_remote, TestUtils.rubyComponentId_remote, dummyInInfs, dummyOutInfs,
          "fh/cdocker-reg", "latest","xxxx", 443, true);
      TestUtils.client.deploy(dummyContext, rDockerComponent);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy remote docker zookeeper component");
    }
  }

  @Test
  public void testIZookDeploy() {
    try {
      List<TestUtils.InportInfo> inInfs = TestUtils.getInPortInfos(TestUtils.zookeeperInternalInportName);
      List<OutportInfo> outInfs = TestUtils.getOutPortInfos(TestUtils.zookeeperOutportName);
      DockerComponent zookComp = TestUtils.buildDockerComponent( TestUtils.client, TestUtils.zookeeperComponent, TestUtils.zookeeperComponentId, inInfs, outInfs, "3.4.12", true, false);
      DeploymentContext zookContext = TestUtils.createZookeperContext(TestUtils.client, TestUtils.zookeeperComponentId, TestUtils.zookeeperInternalInportName, TestUtils.version.DOCKER );
      TestUtils.zookId =
          TestUtils.client.deploy(zookContext, zookComp);
      boolean isReady = false;
      do {
        System.out.println("zook not ready");
        isReady = TestUtils.client.isReady(TestUtils.zookId);
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
      List<TestUtils.InportInfo> inInfs = TestUtils.getInPortInfos(TestUtils.zookeeperInternalInportName_lifecycle);
      List<TestUtils.OutportInfo> outInfs = TestUtils.getOutPortInfos(TestUtils.zookeeperOutportName_lifecycle);
      DeployableComponent zookComp = TestUtils.buildDeployableComponent(TestUtils.client, TestUtils.zookeeperComponent_lifecycle, TestUtils.zookeeperComponentId_lifecycle,
          inInfs, outInfs, new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return TestUtils.createDefaultLifecycleStore();
            }
          });
      DeploymentContext zookContext = TestUtils.createZookeperContext(TestUtils.client, TestUtils.zookeeperComponentId, TestUtils.zookeeperInternalInportName, TestUtils.version.LIFECYCLE);
      OperatingSystem os = new OperatingSystemImpl(
          OperatingSystemFamily.UBUNTU,
          OperatingSystemArchitecture.AMD64,
          OperatingSystemVersions.of(1604,null));
      TestUtils.zookId_lifecycle =
          TestUtils.client.deploy(zookContext, zookComp, os, ContainerType.DOCKER);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy lifecycle component");
    }
  }

  @Test
  public void testIZookDeploy_latest() {
    try {
      List<TestUtils.InportInfo> inInfs = TestUtils.getInPortInfos(TestUtils.zookeeperInternalInportName_lifecycle);
      List<TestUtils.OutportInfo> outInfs = TestUtils.getOutPortInfos(TestUtils.zookeeperOutportName_lifecycle);
      DockerComponent zookComp = TestUtils.buildDockerComponent(TestUtils.client, TestUtils.zookeeperComponent_lifecycle, TestUtils.zookeeperComponentId_lifecycle, inInfs, outInfs, "latest", false, true);
      DeploymentContext zookContext = TestUtils.createZookeperContext(TestUtils.client, TestUtils.zookeeperComponentId_lifecycle, TestUtils.zookeeperInternalInportName_lifecycle, TestUtils.version.LIFECYCLE );
      TestUtils.zookId_lifecycle =
          TestUtils.client.deploy(zookContext, zookComp);
      boolean isReady = false;
      do {
        System.out.println("zook latest not ready");
        isReady = TestUtils.client.isReady(TestUtils.zookId_lifecycle);
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
        zookStatus = TestUtils.client.getComponentContainerStatus(TestUtils.zookId, TestUtils.publicIp);
        zookStatus_lifecycle  = TestUtils.client.getComponentContainerStatus(TestUtils.zookId_lifecycle , TestUtils.publicIp);
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
            TestUtils.client.undeploy(TestUtils.zookId, false);
          } catch (DeploymentException e) {
            System.err.println("Exception during deployment!");
          };
        }
      });
      t.start();
      sleep(150000);
      TestUtils.client.undeploy(TestUtils.zookId_lifecycle, false);
      t.join();
    } catch (DeploymentException ex) {
      System.err.println("Exception during deployment!");
    } catch (InterruptedException ex) {
      System.err.println("Interrupted!");
    }
  }
}