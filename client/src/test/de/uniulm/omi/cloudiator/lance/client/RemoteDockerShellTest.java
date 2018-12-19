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

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.DockerShellTestAgent;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitecture;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitectureBuilder;
import de.uniulm.omi.cloudiator.lance.util.application.ComponentInfo;
import de.uniulm.omi.cloudiator.lance.util.application.InportInfo;
import de.uniulm.omi.cloudiator.lance.util.application.OutportInfo;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

// Important: Java-re, docker daemon and Lance(remote-testing) must be installed on the VM and
// related ports must be opened on the vm
// Install etcd on the vm via install_etcd.sh which can be found in the pinned installation
// repository

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoteDockerShellTest {

  // adjust
  private static final String publicIp = "x.x.x.x";
  private static AppArchitecture arch;
  private static LifecycleClient client;
  private static DockerShellServerDelegate del;

  @BeforeClass
  public static void configureAppContext() {
    AppArchitectureBuilder builder =
        new AppArchitectureBuilder(
            "ShellTestApp", new ApplicationId(), new ApplicationInstanceId());
    ComponentInfo zookCompInfo =
        new ComponentInfo(
            "zookeeper",
            new ComponentId(),
            new ComponentInstanceId(),
            new HashSet<InportInfo>(),
            new HashSet<OutportInfo>(),
            OperatingSystem.UBUNTU_14_04);
    arch = builder.addComponentInfo(zookCompInfo).build();

    System.setProperty("lca.client.config.registry", "etcdregistry");
    // adjust
    System.setProperty("lca.client.config.registry.etcd.hosts", "x.x.x.x:4001");
  }

  @Test
  public void testADelegateGetter() {
    try {
      del = DockerShellServerDelegate.getDelegate(publicIp);
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
  public void testCSetupContainer() {
    try {
      del.setupContainer();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDOpenShellAndCreateContainer() {
    ComponentInfo info = (ComponentInfo) arch.getComponents().toArray()[0];
    openShellAndCreateContainer(info.getComponentName(), new HashMap<Integer, Integer>());
  }

  @Test
  public void testEStartContainer() {
    startContainer();
  }

  @Test
  public void testIStopContainer() {
    stopContainer();
  }

  @Test
  public void testJStartContainer() {
    startContainer();
  }

  @Test
  public void testLOpenAndInstallShell() {
    openAndInstallShell();
  }

  @Test
  public void testMSetEnvironment() {
    setEnvironment();
  }

  @Test
  public void testNCloseShell() {
    closeShell();
  }

  @Test
  public void testOOpenAndInstallShell() {
    openAndInstallShell();
  }

  @Test
  public void testPSetEnvironment() {
    setEnvironment();
  }

  @Test
  public void testQCloseShell() {
    closeShell();
  }

  @Test
  public void testROpenAndInstallShell() {
    openAndInstallShell();
  }

  @Test
  public void testSSetEnvironment() {
    setEnvironment();
  }

  @Test
  public void testTCloseShell() {
    closeShell();
  }

  @Test
  public void testUOpenAndInstallShell() {
    openAndInstallShell();
  }

  @Test
  public void testVSetEnvironment() {
    setEnvironment();
  }

  @Test
  public void testWCloseShell() {
    closeShell();
  }

  @Test
  public void testXOpenAndInstallShell() {
    openAndInstallShell();
  }

  @Test
  public void testYStopContainer() {
    stopContainer();
  }

  @Test
  public void testZCloseShell() {
    closeShell();
  }

  void openShellAndCreateContainer(String imageName, Map<Integer, Integer> portMap) {
    try {
      del.openShellAndCreateContainer(imageName, portMap);
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  void startContainer() {
    try {
      del.startContainer();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  void setEnvironment() {
    try {
      del.setEnvironment();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  void stopContainer() {
    try {
      del.stopContainer();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  void closeShell() {
    try {
      del.closeShell();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  void openAndInstallShell() {
    try {
      del.openAndInstallShell();
    } catch (DeploymentException e) {
      e.printStackTrace();
    }
  }

  static class DockerShellServerDelegate extends ServerDelegate<ApplicationInstanceId> {

    private static volatile DockerShellServerDelegate instance;
    private static volatile DockerShellTestAgent testAgent;
    private static volatile String publicIp;

    private DockerShellServerDelegate() {};

    public static DockerShellServerDelegate getDelegate(String pIp)
        throws RemoteException, NotBoundException {
      if (instance == null) {
        instance = new DockerShellServerDelegate();
        publicIp = pIp;
        try {
          setTType(TestType.DOCKERSHELLTEST);
          setPublicIp(publicIp);
          setRemoteAgent();
          testAgent = dsTestAgent;
        } catch (IOException e) {
          // ignored
        }
      }
      return instance;
    }

    public ApplicationInstanceId setupContainer() throws DeploymentException {

      Retryer<ApplicationInstanceId> retryer =
          RetryerBuilder.<ApplicationInstanceId>newBuilder()
              .retryIfExceptionOfType(RemoteException.class)
              .withWaitStrategy(WaitStrategies.exponentialWait())
              .withStopStrategy(StopStrategies.stopAfterDelay(5, TimeUnit.MINUTES))
              .build();

      Callable<ApplicationInstanceId> callable =
          () -> {
            return testAgent.setupContainer(arch, publicIp, currentRegistry);
          };

      return instance.makeRetryerCall(retryer, callable);
    }

    public void openShellAndCreateContainer(String imageName, Map<Integer, Integer> portMap)
        throws DeploymentException {
      try {
        testAgent.openShellAndCreateContainer(imageName, portMap);
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    public void startContainer() throws DeploymentException {
      try {
        testAgent.startContainer();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    public void setEnvironment() throws DeploymentException {
      try {
        testAgent.setEnvironment();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    public void stopContainer() throws DeploymentException {
      try {
        testAgent.stopContainer();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    public void closeShell() throws DeploymentException {
      try {
        testAgent.closeShell();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    public void openAndInstallShell() throws DeploymentException {
      try {
        testAgent.openAndInstallShell();
      } catch (ContainerException e) {
        throw new DeploymentException(e.getCause());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }
  }
}
