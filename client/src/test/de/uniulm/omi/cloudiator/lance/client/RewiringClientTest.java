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

import static org.junit.Assert.*;
import com.github.rholder.retry.*;
import de.uniulm.omi.cloudiator.lance.LcaConstants;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.LcaException;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.util.application.*;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistryFactory;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import de.uniulm.omi.cloudiator.lance.lca.RewiringTestAgent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//Important: Java-re, docker daemon and Lance(remote-testing) must be installed on the VM and related ports must be opened on the vm
//Install etcd on the vm via install_etcd.sh which can be found in the pinned installation repository

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RewiringClientTest {

  private static AppArchitecture arch;
  //adjust
  private static String publicIp = "134.60.64.95";
  private static RewiringServerDelegate del;

  @BeforeClass
  public static void configureAppContext() {
    AppArchitectureBuilder builder = new AppArchitectureBuilder("RewiringApp", new ApplicationId(), new ApplicationInstanceId());
    //cassandra
    InportInfo cassInportInfo = new InportInfo("CASS_INT_INP", PortProperties.PortType.INTERNAL_PORT, 9160, 0, 1);
    HashSet<InportInfo> cassInSet = new HashSet<>();
    cassInSet.add(cassInportInfo);
    ComponentInfo cassCompInfo = new ComponentInfo("cassandra", new ComponentId(), new ComponentInstanceId(), cassInSet, new HashSet<OutportInfo>(), OperatingSystem.UBUNTU_14_04);
    //kafka
    InportInfo kafkaInportInfo = new InportInfo("KAFKA_INP", PortProperties.PortType.PUBLIC_PORT, 9092, 1, 1);
    HashSet<InportInfo> kafkaInSet = new HashSet<>();
    kafkaInSet.add(kafkaInportInfo);
    //hier! PU-Handler anpassen
    //OutportInfo kafkaOutportInfo = new OutportInfo("KAFKA_OUT", DeploymentHelper.getEmptyPortUpdateHandler(), 0, 1, 1);
    OutportInfo kafkaOutportInfo = new OutportInfo("KAFKA_OUT", DeploymentHelper.getEmptyPortUpdateHandler(), 0, 1, 0);
    HashSet<OutportInfo> kafkaOutSet = new HashSet<>();
    kafkaOutSet.add(kafkaOutportInfo);
    ComponentInfo kafkaCompInfo = new ComponentInfo("kafka", new ComponentId(), new ComponentInstanceId(), kafkaInSet, kafkaOutSet, OperatingSystem.UBUNTU_14_04);
    //setup Architecture
    arch = builder.addComponentInfo(kafkaCompInfo).addComponentInfo(cassCompInfo).build();

    System.setProperty("lca.client.config.registry", "etcdregistry");
    //adjust
    System.setProperty("lca.client.config.registry.etcd.hosts",  "134.60.64.95:4001");
  }

  @Test
  public void testADelegateGetter() {
    try {
      del = RewiringServerDelegate.getDelegate(publicIp );
    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (NotBoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testBRegisterApp() {
    try {
      del.registerApp(arch);
    } catch (RegistrationException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCSetUpInitialTopology() {
    try {
      ApplicationInstanceId appId = del.testNewTopology();
      assertEquals(appId.toString(),arch.getAppInstanceId().toString());
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDTraverseBeforeLcc() {
    try {
      del.testTraverseBeforeLcc();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testETraverseAndLccTraverseBeforeStop() {
    try {
      del.testTraverseAndLccTraverseBeforeStop();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testFStopSM() {
    try {
      del.testStopTransition();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testGStartPortUpdater() {
    try {
      del.testPortUpdater();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  static class RewiringServerDelegate extends ServerDelegate<ApplicationInstanceId> {

    private static volatile RewiringServerDelegate instance;
    private static volatile RewiringTestAgent testAgent;
    private static volatile String publicIp;

    private RewiringServerDelegate() {};

    public static RewiringServerDelegate getDelegate(String pIp) throws RemoteException, NotBoundException {
      if (instance == null) {
        instance = new RewiringServerDelegate();
        publicIp = pIp;
        try {
          setTType(TestType.REWIRINGTEST);
          setPublicIp(publicIp);
          setRemoteAgent();
          testAgent = rwTestAgent;
        } catch (IOException e) {
          // ignored
        }
      }
      return instance;
    }

    public ApplicationInstanceId testNewTopology() throws DeploymentException {

      Retryer<ApplicationInstanceId> retryer =
          RetryerBuilder.<ApplicationInstanceId>newBuilder()
              .retryIfExceptionOfType(RemoteException.class)
              .withWaitStrategy(WaitStrategies.exponentialWait())
              .withStopStrategy(StopStrategies.stopAfterDelay(5, TimeUnit.MINUTES))
              .build();

      Callable<ApplicationInstanceId> callable =
          () -> {
            return testAgent.testNewTopology(arch, publicIp, currentRegistry);
          };

        return instance.makeRetryerCall(retryer, callable);
    }

    public void testTraverseBeforeLcc() throws DeploymentException {
      try {
        testAgent.testSMTraversingBeforeLccSM();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    public void testTraverseAndLccTraverseBeforeStop() throws DeploymentException {
      try {
        testAgent.testSMsTraversingBeforeStop();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    public void testStopTransition() throws DeploymentException {
      try {
        testAgent.testSMsStopTransition();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    public void testPortUpdater() throws DeploymentException {
      try {
        testAgent.testPortUpdater();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }
  }
}

