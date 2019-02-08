package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Todo: Handle sockets inside of Set

/* Executes a script inside the container, that processes the 'ip:port'
vaLues of associated dyngroup components inside the registry */
class DockerDynHandler extends Thread {
  private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerManager.class);

  private final String dynHandlerVal;
  private final String updateScriptFilePath;
  private final DockerConnector client;
  private Map<ComponentInstanceId,String> socketsBefore;
  private Map<ComponentInstanceId,String> socketsAfter;
  private GlobalRegistryAccessor accessor;
  private String containerName;
  private volatile boolean running;

  DockerDynHandler(String containerName, String dynHandlerVal, String updateScriptFilePath, DockerConnector client) {
    this.containerName = containerName;
    this.dynHandlerVal = dynHandlerVal;
    this.updateScriptFilePath = updateScriptFilePath;
    this.client = client;
    socketsBefore = new HashMap<>();
    socketsAfter = new HashMap<>();
    accessor = null;
    this.running = false;
  }

  public void setAccessor(GlobalRegistryAccessor accessor) {
    this.accessor = accessor;
  }

  @Override
  public void run() {
    this.running = true;
    Map<ComponentInstanceId,Map<String,String>> runningDumps = new HashMap<>();
    LOGGER.info(String
        .format("Starting dynamic Handler: %s.", containerName));

    do {
      try {
        if(accessor==null) {
          LOGGER.error("Docker Dyn Handler cannot continue working as it cannot access the registry.");
          return;
        }

        runningDumps = accessor.getRunningDumps();
        socketsAfter = filterHandlerGroupSocks(runningDumps);
        final SocketsDiff socketsDiff = calcSocketsDiff();

        if(socketsDiff.hasDiff()) {
          executeUpdate();
        }

        if(socketsDiff.hasDestroyed()) {
          syncDestruction(socketsDiff.socketsDestroyed);
        }

        socketsBefore = socketsAfter;
      } catch (RegistrationException e) {
        LOGGER.error(String
            .format("Cannot access registry properly in Dyn Handler component."));
      } catch (DockerException e) {
        LOGGER.error(String
            .format("Cannot execute dynamic update inside Dyn Handler container: %s.", containerName));
      }
    } while (running);
  }

  /* If a Dyn Component is about to be destroyed, it waits (thread.wait) for the DynHandler to execute the update
  * accordingly AND syncing the Dyn Component's state to PRE_STOP in the registry before it can continue (i.e.
  * breaking out of thread.wait)*/
  private void syncDestruction(Map<ComponentInstanceId,String> destrSocks) throws RegistrationException {
    for(Map.Entry<ComponentInstanceId,String> sock: destrSocks.entrySet()) {
      LOGGER.warn(String
          .format("Setting Dynamic Component Instance %s Container_Status to a temporally inconsistent state"
              + " for synchronisation purposes.", sock.getKey()));
      accessor.syncDynamicDestructionStatus(sock.getKey());
    }
  }

  private void executeUpdate() throws DockerException {
    final String execString = buildExecInsideContainerString(socketsAfter, updateScriptFilePath);
    LOGGER.info(String
        .format("Starting dynamic update via docker cli command: %s.", execString));
    client.executeSingleDockerCommand(execString);
  }

  private SocketsDiff calcSocketsDiff() {
    Map<ComponentInstanceId, String> socketsKept = new HashMap<>();
    Map<ComponentInstanceId, String> socketsNew = new HashMap<>();
    Map<ComponentInstanceId, String> socketsDestroyed = new HashMap<>();

    for (Map.Entry<ComponentInstanceId, String> socketBefore : socketsBefore.entrySet()) {
      boolean found = false;
      final String beforeKey = socketBefore.getKey().toString();
      final String beforeVal = socketBefore.getValue();
      //todo: use map.get instead of 2nd loop
      for (Map.Entry<ComponentInstanceId, String> socketAfter : socketsAfter.entrySet()) {
        final String afterKey = socketAfter.getKey().toString();
        final String afterVal = socketAfter.getValue();

        if (beforeKey.equals(afterKey) && beforeVal.equals(afterVal)) {
          socketsKept.put(socketBefore.getKey(),socketBefore.getValue());
          found = true;
        }
      }

      if(found==false) {
        socketsDestroyed.put(socketBefore.getKey(),socketBefore.getValue());
      }
    }

    for (Map.Entry<ComponentInstanceId, String> socketAfter : socketsAfter.entrySet()) {
      final String keptSocket = socketsKept.get(socketAfter.getKey());
      if( keptSocket != null && keptSocket.equals(socketAfter.getValue())) {
        continue;
      }
      socketsNew.put(socketAfter.getKey(), socketAfter.getValue());
    }

    SocketsDiff diff = new SocketsDiff();
    diff.socketsKept = socketsKept;
    diff.socketsNew = socketsNew;
    diff.socketsDestroyed = socketsDestroyed;

    return diff;
  }

  private Map<ComponentInstanceId, String> filterHandlerGroupSocks(Map<ComponentInstanceId,Map<String, String>> readyDumps) throws RegistrationException {
    Map<ComponentInstanceId, String> socks = new HashMap<>();
    final String dynGroupKey = LcaRegistryConstants.regEntries.get(Identifiers.DYN_GROUP_KEY);

    for(Map.Entry<ComponentInstanceId,Map<String,String>> compDump: readyDumps.entrySet()) {
      if(compDump.getValue() == null || compDump.getValue().get(dynGroupKey) == null) {
        continue;
      }

      Map<String,String> dumpMap = compDump.getValue();
      String dynGroupVal = dumpMap.get(dynGroupKey);
      if(dynGroupVal.equals(dynHandlerVal)) {
        socks.put(compDump.getKey(), buildSocket(dumpMap, containerName));
      }
    }

    return socks;
  }

  private String buildExecInsideContainerString(
      Map<ComponentInstanceId,String> sockets, String updateScriptFilePath) {

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<ComponentInstanceId,String> entry : sockets.entrySet()) {
      final String s = entry.getValue();
      sb.append(s);
      sb.append(" ");
    }

    final String socketsStr = sb.toString();
    final String commandStr = "exec -ti " + containerName + " " + updateScriptFilePath + " "
        + socketsStr;

    return commandStr;
  }

  private static String buildSocket(Map<String, String> compDump, String cName) throws RegistrationException {
    final String ipVal = PortRegistryTranslator.getHierarchicalHostname(PortRegistryTranslator.PORT_HIERARCHY_0, compDump);
    final String portVal = getPortVal(compDump);
    String socket = "";
    if(portVal.equals("-1")) {
      LOGGER.warn(String
          .format("Found port value -1 for a socket in Dynamic Handler %s. Skipping it"
              + "...", cName));
    } else {
      socket = ipVal + ":" + portVal;
    }

    return socket;
  }

  private static String getPortVal(Map<String, String> compDump) throws RegistrationException {
    String retVal = "";
    for(Map.Entry<String,String> entry: compDump.entrySet()) {
      final String key = entry.getKey();
      if(key.matches("^[^\\s]*PUBLIC[^\\s]+Port$")) {
        retVal = entry.getValue();
      }
      if(key.matches("^[^\\s]*PUBLIC[^\\s]+I[Nn][Pp]$")) {
        retVal = entry.getValue();
      }
    }

    if(retVal.equals("")) {
      throw new RegistrationException("Dynamic Component has no public port set.");
    }
    return retVal;
  }

  //todo: better get this String from PortRegistryTranslator

  public synchronized void setRunning(boolean running) {
    this.running = running;
  }

  public boolean isRunning() {
    return running;
  }

  /* Provides info about the diff in the available sockets */
  private static class SocketsDiff {
    public Map<ComponentInstanceId,String> socketsKept;
    public Map<ComponentInstanceId,String> socketsNew;
    public Map<ComponentInstanceId,String> socketsDestroyed;

    private SocketsDiff() {
      socketsKept = new HashMap<>();
      socketsNew = new HashMap<>();
      socketsDestroyed = new HashMap<>();
    }

    /* Did smth change between two iterations? */
    private boolean hasDiff() {
      if(socketsDestroyed.size()==0 && socketsNew.size()==0) {
        return false;
      }

      return true;
    }

    private boolean hasDestroyed() {
      return (socketsDestroyed.size() > 0) ? true : false;
    }
  }
}
