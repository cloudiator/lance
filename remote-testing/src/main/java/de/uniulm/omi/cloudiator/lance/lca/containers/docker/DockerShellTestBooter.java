package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.LcaConstants;
import de.uniulm.omi.cloudiator.lance.lca.DockerShellTestAgent;
import de.uniulm.omi.cloudiator.lance.util.Registrator;

public class DockerShellTestBooter {

  public static final String DST_REGISTRY_KEY = "DockerShellTestAgent";
  private static final Registrator<DockerShellTestAgent> reg =
      Registrator.create(DockerShellTestAgent.class);
  private static volatile DockerShellTestImpl dst = createDSImplementation();
  private static volatile DockerShellTestAgent stub = reg.export(dst, LcaConstants.AGENT_RMI_PORT);

  public static void main(String[] args) {
    if (stub != null && reg.addToRegistry(stub, DST_REGISTRY_KEY)) {
      // from here on RMI takes over //
      Thread idle = new Thread(new IdleRunnable());
      idle.setDaemon(true);
      idle.start();
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    unregister(dst);
                    idle.interrupt();
                  }));
    } else {
      Runtime.getRuntime().exit(-128);
    }
  }

  private static DockerShellTestImpl createDSImplementation() {
    DockerShellTestImpl impl = new DockerShellTestImpl();
    return impl;
  }

  public static void unregister(DockerShellTestImpl impl) {
    reg.unregister(impl);
  }

  private static class IdleRunnable implements Runnable {

    @Override
    public void run() {
      while (!Thread.interrupted()) {
        Object obj = new Object();
        synchronized (obj) {
          try {
            obj.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }
}
