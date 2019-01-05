package de.uniulm.omi.cloudiator.lance.client;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.DockerShellTestAgent;
import de.uniulm.omi.cloudiator.lance.lca.LcaException;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.RewiringTestAgent;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistryFactory;
import de.uniulm.omi.cloudiator.lance.util.application.AppArchitecture;
import de.uniulm.omi.cloudiator.lance.util.application.ComponentInfo;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

// todo: implement in "enum" style
class ServerDelegate<T> {

  public static final String REWT_REGISTRY_KEY = "RewiringTestAgent";;
  public static final String DST_REGISTRY_KEY = "DockerShellTestAgent";
  protected static final LcaRegistry currentRegistry;
  public static volatile TestType tType;
  public static volatile String publicIp;
  protected static volatile RewiringTestAgent rwTestAgent = null;
  protected static volatile DockerShellTestAgent dsTestAgent = null;

  static {
    try {
      currentRegistry = RegistryFactory.createRegistry();
    } catch (RegistrationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public ServerDelegate() {}

  public static String getPublicIp() {
    return publicIp;
  };

  public static void setPublicIp(String pIp) {
    publicIp = pIp;
  }

  public static TestType getTType() {
    return tType;
  }

  public static void setTType(TestType tT) {
    tType = tT;
  }

  protected static void setRemoteAgent() throws RemoteException, NotBoundException {
    try {
      if (tType == null) throw new RemoteException("TestType not set");
      if (publicIp == null) throw new RemoteException("publicIp not set");

      RMISocketFactory.setSocketFactory(
          new RMISocketFactory() {

            private final RMISocketFactory delegate = RMISocketFactory.getDefaultSocketFactory();

            @Override
            public Socket createSocket(String host, int port) throws IOException {
              final Socket socket = delegate.createSocket(host, port);
              return socket;
            }

            @Override
            public ServerSocket createServerSocket(int i) throws IOException {
              return delegate.createServerSocket(i);
            }
          });
      Registry reg = LocateRegistry.getRegistry(publicIp);
      Object o;

      switch (tType) {
        case REWIRINGTEST:
          o = reg.lookup(REWT_REGISTRY_KEY);
          rwTestAgent = (RewiringTestAgent) o;
          dsTestAgent = null;
          break;
        case DOCKERSHELLTEST:
          o = reg.lookup(DST_REGISTRY_KEY);
          dsTestAgent = (DockerShellTestAgent) o;
          rwTestAgent = null;
          break;
        default:
          throw new RemoteException("Wrong test type");
      }
    } catch (IOException e) {
      // ignored
    }
  }

  protected static void registerApp(AppArchitecture arch) throws RegistrationException {
    String appName = arch.getApplicationName();
    ApplicationId appId = arch.getApplicationId();
    ApplicationInstanceId appInstId = arch.getAppInstanceId();
    currentRegistry.addApplicationInstance(appInstId, appId, appName);

    for (ComponentInfo cInfo : arch.getComponents()) {
      String cName = cInfo.getComponentName();
      ComponentId cId = cInfo.getComponentId();
      ComponentInstanceId cInstId = cInfo.getComponentInstanceId();
      currentRegistry.addComponent(appInstId, cId, cName);
      currentRegistry.addComponentInstance(appInstId, cId, cInstId);
    }
  }

  protected static Exception handleRemoteException(RemoteException re) {
    Throwable t = re.getCause();
    if (t == null) {
      return new LcaException("network exception occurred", re);
    }
    if (t instanceof LcaException) {
      return (LcaException) t;
    }
    if (t instanceof RegistrationException) {
      return (RegistrationException) t;
    }

    return new LcaException("downstream exception occurred.", re);
  }

  protected T makeRetryerCall(Retryer<T> retryer, Callable<T> callable) throws DeploymentException {
    try {
      return retryer.call(callable);
    } catch (ExecutionException e) {
      throw new DeploymentException(e.getCause());
    } catch (RetryException e) {
      if (e.getCause() instanceof RemoteException) {
        throw new DeploymentException(handleRemoteException((RemoteException) e.getCause()));
      } else {
        throw new IllegalStateException(e);
      }
    }
  }

  public enum TestType {
    REWIRINGTEST,
    DOCKERSHELLTEST
  }
}
