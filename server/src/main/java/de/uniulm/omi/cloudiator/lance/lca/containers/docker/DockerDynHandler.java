package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.util.ArrayList;
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
class DockerDynHandler implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerManager.class);

  private final String dynHandlerVal;
  private final String updateScriptFilePath;
  private final DockerConnector client;
  private List<String> socketsBefore;
  private List<String> socketsAfter;
  private GlobalRegistryAccessor accessor;
  private String containerName;
  private volatile boolean running;

  DockerDynHandler(String containerName, String dynHandlerVal, String updateScriptFilePath, DockerConnector client) {
    this.containerName = containerName;
    this.dynHandlerVal = dynHandlerVal;
    this.updateScriptFilePath = updateScriptFilePath;
    this.client = client;
    socketsBefore = new ArrayList<>();
    socketsAfter = new ArrayList<>();
    accessor = null;
    this.running = false;
  }

  public void setAccessor(GlobalRegistryAccessor accessor) {
    this.accessor = accessor;
  }

  @Override
  public void run() {
    if(accessor==null) {
      LOGGER.error("Docker Dyn Handler cannot start working as it cannot access the registry.");
      return;
    }

    this.running = true;
    List<Map<String,String>> readyDumps = new ArrayList<>();

    do {
      try {
        readyDumps = accessor.getReadyDumps();
        socketsAfter = filterHandlerGroupSocks(readyDumps);
        if(checkSocketsDiff()) {
          final String execString = buildExecInsideContainerString(socketsAfter, updateScriptFilePath);
          LOGGER.info(String
              .format("Starting dynamic update via docker cli command: %s.", execString));
          client.executeSingleDockerCommand(execString);
          socketsBefore = socketsAfter;
        }
      } catch (RegistrationException e) {
        LOGGER.error(String
            .format("Cannot access registry properly in Dyn Handler component."));
      } catch (DockerException e) {
        LOGGER.error(String
            .format("Cannot execute dynamic update inside Dyn Handler container: %s.", containerName));
      }
    } while (running);
  }

  private boolean checkSocketsDiff() {
    Set<String> before = new HashSet<>(socketsBefore);
    Set<String> after = new HashSet<>(socketsAfter);

    if(before.equals(after)) {
      return false;
    }

    return true;
  }

  private List<String> filterHandlerGroupSocks(List<Map<String, String>> readyDumps) throws RegistrationException {
    List<String> socks = new ArrayList<>();
    final String dynGroupKey = LcaRegistryConstants.regEntries.get(Identifiers.DYN_GROUP_KEY);
    for(Map<String,String> compDump: readyDumps) {
      String dynGroupVal = compDump.get(dynGroupKey);
      if(dynGroupVal==dynHandlerVal) {
        socks.add(buildSocket(compDump));
      }
    }

    return socks;
  }

  private String buildExecInsideContainerString(
      List<String> sockets, String updateScriptFilePath) {

    StringBuilder sb = new StringBuilder();
    for (String s : sockets) {
      sb.append(s);
      sb.append(" ");
    }

    final String socketsStr = sb.toString();
    final String commandStr = "exec -ti " + containerName + " " + updateScriptFilePath
        + " " + updateScriptFilePath;

    return commandStr;
  }

  private static String buildSocket(Map<String, String> compDump) throws RegistrationException {
    final String ipVal = PortRegistryTranslator.getHierarchicalHostname(PortRegistryTranslator.PORT_HIERARCHY_0, compDump);
    final String portVal = getPortVal(compDump);
    final String socket = ipVal + ":" + portVal;
    
    return socket;
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
}
