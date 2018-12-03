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
import de.uniulm.omi.cloudiator.lance.container.standard.ExternalContextParameters.InPortContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import de.uniulm.omi.cloudiator.lance.util.application.ProvidedPortInfo;
import de.uniulm.omi.cloudiator.lance.util.application.RequiredPortInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.component.*;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.util.Random;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
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

  private RemoteDockerComponent buildRemoteDockerComponent( LifecycleClient client, String compName, ComponentId id, Set<ProvidedPortInfo> provInfs,
      Set<RequiredPortInfo> reqInfs, String imageFolder, String tag, String hostName, int port, boolean useCredentials) {

    DockerComponent.Builder dCompBuilder = ClientTestUtils.buildDockerComponentBuilder(client, compName, id, provInfs, reqInfs, imageFolder, imageName_remote, tag);
    RemoteDockerComponent.DockerRegistry dReg = new RemoteDockerComponent.DockerRegistry(hostName, port, "xxxxx", "xxxxx", useCredentials);
    RemoteDockerComponent rDockerComp = new RemoteDockerComponent(dCompBuilder, dReg);

    return rDockerComp;
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
      client.registerComponentForApplicationInstance(appInstanceId, rubyComponentId_remote);
    } catch (RegistrationException ex) {
      System.err.println("Exception during registration");
    }
  }

  @Test
  public void testCZookCompDescriptions() {
    Set<ProvidedPortInfo> provInfs = getProvPortInfos(zookeeperInternalInportName);
    Set<RequiredPortInfo> reqInfs = getReqPortInfos(zookeeperOutportName);
    DockerComponent.Builder dBuilder = ClientTestUtils.buildDockerComponentBuilder( client, zookeeperComponent, zookeeperComponentId, provInfs, reqInfs, "", imageName, "3.4.12");
    dBuilder.build();
  }

  @Test
  public void testCZookCompDescriptions_lifecycle() {
    Set<ProvidedPortInfo> provInfs = getProvPortInfos(zookeeperInternalInportName_lifecycle);
    Set<RequiredPortInfo> reqInfs = getReqPortInfos(zookeeperOutportName_lifecycle);
    ClientTestUtils.buildDeployableComponent( client, zookeeperComponent_lifecycle, zookeeperComponentId_lifecycle,
        provInfs, reqInfs, new Callable<LifecycleStore>() {
          public LifecycleStore call() {
            return createDefaultLifecycleStore();
          }
        });
  }

  private static Set<ProvidedPortInfo> getProvPortInfos(String internalInportName) {
    Set<ProvidedPortInfo> provInfs = new HashSet<>();
    ProvidedPortInfo provInf =
        new ProvidedPortInfo(internalInportName, PortProperties.PortType.INTERNAL_PORT, 2, 3888);
    provInfs.add(provInf);

    return provInfs;
  }

  private static Set<RequiredPortInfo> getReqPortInfos(String reqPortName) {
    Set<RequiredPortInfo> reqInfs = new HashSet<>();
    RequiredPortInfo reqInf =
        new RequiredPortInfo(
            reqPortName,
            DeploymentHelper.getEmptyPortUpdateHandler(),
            1,
            1,
            OutPort.NO_SINKS);
    reqInfs.add(reqInf);

    return reqInfs;
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

  @Ignore
  @Test
  public void testHRemoteDockerComponent() {
    try {
      DeploymentContext dummyContext =
          client.initDeploymentContext(applicationId, appInstanceId);
      Set<ProvidedPortInfo> dummyProvInfs = new HashSet<>();
      Set<RequiredPortInfo> dummyReqInfs = new HashSet<>();
      //ssl Port
      RemoteDockerComponent rDockerComponent = buildRemoteDockerComponent( client, rubyComponent_remote, rubyComponentId_remote, dummyProvInfs, dummyReqInfs, "fh/docker-reg", "latest","xxxx", 443, true);
      client.deploy(dummyContext, rDockerComponent);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy remote docker zookeeper component");
    }
  }

  @Test
  public void testIZookDeploy() {
    try {
      Set<ProvidedPortInfo> provInfs = getProvPortInfos(zookeeperInternalInportName);
      Set<RequiredPortInfo> reqInfs = getReqPortInfos(zookeeperOutportName);
      DockerComponent zookComp = ClientTestUtils.buildDockerComponentBuilder( client, zookeeperComponent, zookeeperComponentId, provInfs, reqInfs, "", zookeeperComponent,  "3.4.12").build();
      DeploymentContext zookContext = createZookeperContext(client, zookeeperComponentId_lifecycle, zookeeperInternalInportName_lifecycle, version.DOCKER );
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

  @Test
  public void testIZookDeploy_lifecycle() {
    try {
      Set<ProvidedPortInfo> provInfs = getProvPortInfos(zookeeperInternalInportName_lifecycle);
      Set<RequiredPortInfo> reqInfs = getReqPortInfos(zookeeperOutportName_lifecycle);
      DeployableComponent zookComp = ClientTestUtils.buildDeployableComponent( client, zookeeperComponent_lifecycle, zookeeperComponentId_lifecycle,
          provInfs, reqInfs, new Callable<LifecycleStore>() {
            public LifecycleStore call() {
              return createDefaultLifecycleStore();
            }
          });
      DeploymentContext zookContext = createZookeperContext(client, zookeeperComponentId, zookeeperInternalInportName, version.LIFECYCLE);
      zookId_lifecycle =
          client.deploy(zookContext, zookComp, OperatingSystem.UBUNTU_14_04, ContainerType.DOCKER);
    } catch (DeploymentException ex) {
      System.err.println("Couldn't deploy lifecycle component");
    }
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
      client.undeploy(zookId, true);
      client.undeploy(zookId_lifecycle, true);
    } catch (DeploymentException ex) {
      System.err.println("Exception during deployment!");
    }
  }
}