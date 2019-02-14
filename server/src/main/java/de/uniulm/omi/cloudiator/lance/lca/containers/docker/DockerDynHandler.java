package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
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
        final SocketsDiff socketsDiff = calcSocketsDiff(runningDumps);

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
        LOGGER.error(String
            .format("Error message: %s.", e.getMessage()));
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

  private SocketsDiff calcSocketsDiff(Map<ComponentInstanceId,Map<String,String>> runningDumps) {
    Map<ComponentInstanceId, String> socketsKept = new HashMap<>();
    Map<ComponentInstanceId, String> socketsNew = new HashMap<>();
    Map<ComponentInstanceId, String> socketsDestroyed = new HashMap<>();
    Map<ComponentInstanceId, String> socketsOrphaned = new HashMap<>();

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
        final ComponentInstanceId cId = socketBefore.getKey();
        /* component with compatible dynamic group and running not a valid
         * socket anymore (e.g. port was dynamically set to -1) -> lorphaned */
        if (runningDumps.get(cId)!=null) {
          socketsOrphaned.put(socketBefore.getKey(), socketBefore.getValue());
          LOGGER.warn("Got orphaned socket %s",socketBefore.getValue());
        } else {
          socketsDestroyed.put(socketBefore.getKey(), socketBefore.getValue());
        }
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
    diff.socketsOrphaned = socketsOrphaned;

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
        final String socket = buildSocket(dumpMap, containerName);
        if (!socket.equals("")) {
          socks.put(compDump.getKey(), buildSocket(dumpMap, containerName));
        }
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
    final String commandStr = "exec -i " + containerName + " " + updateScriptFilePath + " "
        + socketsStr;

    return commandStr;
  }

  //todo: check port values: i.e. convert to int
  private static String buildSocket(Map<String, String> compDump, String cName) throws RegistrationException {
    final String ipVal = PortRegistryTranslator.getHierarchicalHostname(PortRegistryTranslator.PORT_HIERARCHY_1, compDump);
    final String portVal = getPortVal(compDump);

    if(!isValidPort(portVal)) {
      LOGGER.warn(String
        .format("Found port value -1 for a socket in Dynamic Handler %s. Skipping it"
          + "...", cName));
        return "";
    }

    final String socket = ipVal + ":" + portVal;
    return socket;
  }

  //todo: check port values: i.e. convert to int
  private static boolean isValidPort(String port) {
    if(port.equals("-1")) {
      return false;
    }

    return true;
  }


  private static String getPortVal(Map<String, String> compDump) throws RegistrationException {
    String retVal = "";
    for(Map.Entry<String,String> entry: compDump.entrySet()) {
      final String key = entry.getKey();
      if(key.matches("^[^\\s]*PUBLIC[^\\s]+Port$")) {
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

  //debugging
  private static void debugRunningDumps(Map<ComponentInstanceId,Map<String,String>> dumps) {
    if(dumps == null || dumps.size() == 0) {
      LOGGER.debug(String.format("Running dumps is empty!"));
      return;
    }

    for(Map.Entry<ComponentInstanceId,Map<String,String>> entry: dumps.entrySet()) {
      if(entry.getValue() == null) {
        LOGGER.debug(String.format("Got empty map for Component instance: %s", entry.getKey()));
        continue;
      }

      Map<String,String> dumpMap = entry.getValue();

      for(Map.Entry<String,String> strMap: dumpMap.entrySet()) {
        LOGGER.debug(String.format("Found key: %s, value: %s, for running instance %s.", strMap.getKey(),
            strMap.getValue(), entry.getKey()));
      }
    }
  }

  private static void debugDynComparison(String str1, String str2) {
    LOGGER.debug(String.format("Comparing Strings: %s, %s", str1, str2));
    if(str1.equals(str2)) {
      LOGGER.debug(String.format("Strings are euqal!"));
    } else {
      LOGGER.debug(String.format("Strings are not euqal!"));
    }
  }

  private void debugPortParse(String socket) {
    LOGGER.debug(String.format("Found valid socket: %s!", socket));
  }

  /* Provides info about the diff in the available sockets */
  private static class SocketsDiff {
    public Map<ComponentInstanceId,String> socketsKept;
    public Map<ComponentInstanceId,String> socketsNew;
    public Map<ComponentInstanceId,String> socketsDestroyed;
    /* If socket disappeared, but has STATUS!=STOP -> orphaned, could be because PORT has
     * become -1 and socket was hence filtered out in method filterHandlerGroupSocks
     * -> isValidPort */
    public Map<ComponentInstanceId,String> socketsOrphaned;

    private SocketsDiff() {
      socketsKept = new HashMap<>();
      socketsNew = new HashMap<>();
      socketsDestroyed = new HashMap<>();
      socketsOrphaned = new HashMap<>();
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
