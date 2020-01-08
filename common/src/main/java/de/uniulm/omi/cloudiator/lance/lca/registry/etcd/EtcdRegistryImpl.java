/*
 * Copyright (c) 2014-2015 University of Ulm
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

package de.uniulm.omi.cloudiator.lance.lca.registry.etcd;

import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;

final class EtcdRegistryImpl implements LcaRegistry {

  private static final long serialVersionUID = -7017922645290003357L;
  private static String DESCRIPTION = "Description";
  private static String NAME = "Name";
  private static String MAIN_DESCRIPTION = "main directory for cloudiator's life cylce agent";
  private static String MAIN_NAME = "Life Cycle Agent";

  private static String APP_INSTANCE_DESCRIPTION = "Application Instance Directory with all components of this application instance";
  private static String COMPONENT_DESCRIPTION = "Component Directory with all component instances of this component in this application instance";
  private static String COMPONENT_INSTANCE_DESCRIPTION = "Component Instance Directory with all properties of this component instances";

  private final URI[] uris;
  private transient EtcdClient etcd;

  public EtcdRegistryImpl(URI[] urisParam) throws RegistrationException {
    uris = urisParam;

    final EtcdNettyConfig etcdNettyConfig = new EtcdNettyConfig();
    etcdNettyConfig.setMaxFrameSize(etcdNettyConfig.getMaxFrameSize() * 1000);
    final EtcdNettyClient etcdNettyClient = new EtcdNettyClient(etcdNettyConfig, null, uris);

    etcd = new EtcdClient(etcdNettyClient);
    init();
  }

  private void init() throws RegistrationException {
    String dirName = "/lca";
    createDirectorIfItDoesNotExist(dirName);
    setPropertyInDirectory(dirName, DESCRIPTION, MAIN_DESCRIPTION);
    setPropertyInDirectory(dirName, NAME, MAIN_NAME);
  }

  @Override
  /**
   * @return true if this application instance has been added successfully. false if it was already contained
  in the registry.
   */
  public boolean addApplicationInstance(ApplicationInstanceId instId, ApplicationId appId,
      String name) throws RegistrationException {
    String dirName = generateApplicationInstanceDirectoryName(instId);
    boolean b = createDirectorIfItDoesNotExist(dirName);
    if (b) { // only add properties if this is a new directory //
      setPropertyInDirectory(dirName, DESCRIPTION, APP_INSTANCE_DESCRIPTION);
      setPropertyInDirectory(dirName, NAME, name);
    }
    return b;
  }

  @Override
  public void addComponent(ApplicationInstanceId instId, ComponentId cid, String name)
      throws RegistrationException {
    String dirName = generateComponentDirectory(instId, cid);
    createDirectorIfItDoesNotExist(dirName);
    setPropertyInDirectory(dirName, DESCRIPTION, COMPONENT_DESCRIPTION);
    setPropertyInDirectory(dirName, NAME, name);
  }

  @Override
  public void addComponentInstance(ApplicationInstanceId instId, ComponentId cid,
      ComponentInstanceId cinstId)
      throws RegistrationException {
    String dirName = generateComponentInstanceDirectory(instId, cid, cinstId);
    createDirectorIfItDoesNotExist(dirName);
    setPropertyInDirectory(dirName, DESCRIPTION, COMPONENT_INSTANCE_DESCRIPTION);
  }

  @Override
  public void deleteComponentInstance(ApplicationInstanceId instId, ComponentId cid,
      ComponentInstanceId cinstId)
      throws RegistrationException {
    String dirName = generateComponentInstanceDirectory(instId, cid, cinstId);
    if (!directoryDoesExist(dirName)) {
      throw new RegistrationException(
          "Cannot delete Component instance out of etcd as the corresponding directory does not exist");
    }
    try {
      etcd.deleteDir(dirName).recursive().send();
    } catch (IOException e) {
      throw new RegistrationException(
          "Cannot delete Component instance out of etcd as an IOException occured");
    }
  }

  @Override
  public void addComponentProperty(ApplicationInstanceId instId, ComponentId cid,
      ComponentInstanceId cinstId, String property, Object value) throws RegistrationException {
    String dirName = generateComponentInstanceDirectory(instId, cid, cinstId);
    setPropertyInDirectory(dirName, property, value.toString());
  }

  /* todo: Refactor code that uses this method and make it call the overloaded method as this method isn't entirely consistent
  as it breaks the uniqueness of the hierarchy: appId, cId, cInstanceId */
  @Override
  public void addComponentProperty(ApplicationInstanceId instId, ComponentInstanceId cinstId,
      String property, Object value) throws RegistrationException {
    ComponentId cId = getFirstLevelDir(instId, cinstId);

    if (cId == null) {
      throw new RegistrationException(String
          .format("Cannot add Component Instance Property as there is no parent Component dir."));
    }

    addComponentProperty(instId, cId, cinstId, property, value);
  }

  @Override
  public Map<ComponentInstanceId, Map<String, String>> dumpComponent(ApplicationInstanceId instId,
      ComponentId compId) throws RegistrationException {
    Map<ComponentInstanceId, Map<String, String>> retVal = null;
    String dirName = generateComponentDirectory(instId, compId);
    EtcdKeysResponse ccc = null;
    try {
      ccc = etcd.getDir(dirName).recursive().sorted().send().get();
      retVal = dumpFirstLevelKeys(ccc.node);
      for (Entry<ComponentInstanceId, Map<String, String>> entry : retVal.entrySet()) {
        ComponentInstanceId id = entry.getKey();
        String secondLevelDir = generateComponentInstanceDirectory(instId, compId, id);
        ccc = etcd.getDir(secondLevelDir).recursive().sorted().send().get();
        dumpSecondLevelKeys(ccc.node, entry.getValue());
      }
    } catch (IOException ioe) {
      throw new RegistrationException(ioe);
    } catch (java.util.concurrent.TimeoutException e) {
      throw new RegistrationException(e);
    } catch (EtcdException e) {
      throw new RegistrationException(e);
    }
    return retVal;
  }

  @Override
  public Map<ComponentInstanceId, Map<String, String>> dumpAllAppComponents(
      ApplicationInstanceId instId)
      throws RegistrationException {
    Map<ComponentInstanceId, Map<String, String>> retVal = new HashMap<>();
    List<ComponentId> cIds = readFirstLevelDirs(instId);
    for (ComponentId cId : cIds) {
      Map<ComponentInstanceId, Map<String, String>> cInstDumps = dumpComponent(instId, cId);
      retVal.putAll(cInstDumps);
    }

    return retVal;
  }

  @Override
  public String getComponentProperty(ApplicationInstanceId appInstId, ComponentId compId,
      ComponentInstanceId myId, String property) throws RegistrationException {
    String dirName = generateComponentInstanceDirectory(appInstId, compId, myId);
    return readPropertyFromDirectory(dirName, property);
  }

  @Override
  public boolean applicationInstanceExists(ApplicationInstanceId appInstId)
      throws RegistrationException {
    final String dirName = generateApplicationInstanceDirectoryName(appInstId);
    return directoryDoesExist(dirName);
  }

  @Override
  public boolean applicationComponentExists(ApplicationInstanceId appInstId, ComponentId compId)
      throws RegistrationException {
    String dirName = generateComponentDirectory(appInstId, compId);
    return directoryDoesExist(dirName);
  }

  private ComponentId getFirstLevelDir(ApplicationInstanceId appInstId, ComponentInstanceId compId)
      throws RegistrationException {
    ComponentId retVal = null;
    List<ComponentId> cIds = readFirstLevelDirs(appInstId);
    for (ComponentId cId : cIds) {
      Map<ComponentInstanceId, Map<String, String>> cInstDumps = dumpComponent(appInstId, cId);
      if (cInstDumps.get(compId) != null) {
        retVal = cId;
      }
    }
    return retVal;
  }

  /**
   * @return true if this directory has been created successfully. false if it was already contained
   * in the registry.
   */
  private boolean createDirectorIfItDoesNotExist(String dirName) throws RegistrationException {
    synchronized (EtcdRegistryImpl.class) {
      if (directoryDoesExist(dirName)) {
        return false;
      }

      try {
        // EtcdKeysResponse ccc =
        etcd.putDir(dirName).send().get();
      } catch (IOException ioe) {
        throw new RegistrationException(ioe);
      } catch (java.util.concurrent.TimeoutException e) {
        throw new RegistrationException(e);
      } catch (EtcdException e) {
        throw new RegistrationException(e);
      }
      return true;
    }
  }

  /**
   * @return true if this directory exists. false if it was already contained in the registry.
   */
  private boolean directoryDoesExist(String dirName) throws RegistrationException {
    try {
      EtcdKeysResponse response = etcd.getDir(dirName).send().get();
      return response != null;
    } catch (IOException ioe) {
      throw new RegistrationException(ioe);
    } catch (java.util.concurrent.TimeoutException e) {
      throw new RegistrationException(e);
    } catch (EtcdException e) {
      if (e.errorCode == 100) {
        return false;
      }
      throw new RegistrationException(e);
    }
  }

  private void readObject(java.io.ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    etcd = new EtcdClient(uris);
  }

  private String readPropertyFromDirectory(String dirName, String prop)
      throws RegistrationException {
    try {
      EtcdKeysResponse ccc = etcd.get(dirName + "/" + prop).send().get();
      return ccc.node.value;
    } catch (IOException ioe) {
      throw new RegistrationException(ioe);
    } catch (java.util.concurrent.TimeoutException e) {
      throw new RegistrationException(e);
    } catch (EtcdException e) {
      throw new RegistrationException(e);
    }
  }

  private void setPropertyInDirectory(String dirName, String prop, String value)
      throws RegistrationException {
    try {
      EtcdKeysResponse ccc = etcd.put(dirName + "/" + prop, value.toString()).send().get();
      ccc.toString();
    } catch (IOException ioe) {
      throw new RegistrationException(ioe);
    } catch (java.util.concurrent.TimeoutException e) {
      throw new RegistrationException(e);
    } catch (EtcdException e) {
      throw new RegistrationException(e);
    }
  }

  private final static String generateApplicationInstanceDirectoryName(
      ApplicationInstanceId instId) {
    return "/lca/" + instId.toString();
  }

  private final static String generateComponentDirectory(ApplicationInstanceId instId,
      ComponentId cid) {
    return generateApplicationInstanceDirectoryName(instId) + "/" + cid.toString();
  }

  private final static String generateComponentInstanceDirectory(ApplicationInstanceId instId,
      ComponentId cid, ComponentInstanceId cinstId) {
    return generateComponentDirectory(instId, cid) + "/" + cinstId.toString();
  }

  private static void fillMapWithValue(String key, String value, Map<String, String> map) {
    if (DESCRIPTION.equals(key)) {
      return;
    }
    if (NAME.equals(key)) {
      return;
    }
    map.put(key, value);
  }

  private static Map<ComponentInstanceId, Map<String, String>> dumpFirstLevelKeys(EtcdNode root) {
    final String mainDir = root.key;
    Map<ComponentInstanceId, Map<String, String>> retVal = new HashMap<>();
    final int length = mainDir.length() + 1;
    for (EtcdNode node : root.nodes) {
      if (!node.dir) {
        continue;
      }
      String key = node.key.substring(length);
      String[] split = key.split("/");
      if (split.length == 1) { // component instance element //
        createComponentInstanceIfNotExistantAndFillWithMap(key, retVal);
      } else {
        throw new IllegalStateException("invalid directory structure for key");
        // Map<String,String> map = createComponentInstanceIfNotExistantAndFillWithMap(split[0], retVal);
        // fillMapWithValue(split[1], map);
      }
    }
    return retVal;
  }

  private static List<String> getTailDirString(EtcdNode root) {
    Set<String> strSet = new HashSet<>();
    for (EtcdNode node : root.nodes) {
      if (!node.dir) {
        continue;
      }
      String[] split = node.key.split("/");
      int size = split.length;
      if (size > 0) { // component instance element //
        final String key = split[size - 1];
        strSet.add(key);
      } else {
        throw new IllegalStateException("invalid directory structure for key");
      }
    }

    List<String> retVal = new ArrayList<>();
    retVal.addAll(strSet);
    return retVal;
  }

  private List<ComponentId> readFirstLevelDirs(ApplicationInstanceId appInstId)
      throws RegistrationException {
    List<ComponentId> retVal = new ArrayList<>();
    String dirNameApp = generateApplicationInstanceDirectoryName(appInstId);
    EtcdKeysResponse ccc = null;
    try {
      ccc = etcd.getDir(dirNameApp).recursive().sorted().send().get();
      List<String> retValHead = getTailDirString(ccc.node);

      if (retValHead.size() != 1) {
        throw new RegistrationException("Top dir of ComponentIds must be unique!");
      }

      String dirNameCompIdTop = generateComponentDirectory(appInstId,
          ComponentId.fromString(retValHead.get(0)));

      ccc = etcd.getDir(dirNameCompIdTop).recursive().sorted().send().get();
      List<String> retValTail = getTailDirString(ccc.node);
      retVal = buildFullComponentIds(retValHead, retValTail);

    } catch (IOException ioe) {
      throw new RegistrationException(ioe);
    } catch (java.util.concurrent.TimeoutException e) {
      throw new RegistrationException(e);
    } catch (EtcdException e) {
      throw new RegistrationException(e);
    }
    return retVal;
  }

  private static List<ComponentId> buildFullComponentIds(List<String> retValHead,
      List<String> retValTail)
      throws RegistrationException {
    List<ComponentId> cIds = new ArrayList<>();
    if (retValHead.size() != 1) {
      throw new RegistrationException("Top dir of ComponentIds must be unique!");
    }

    for (String str : retValTail) {
      final String fullCidStr = retValHead.get(0) + "/" + str;
      cIds.add(ComponentId.fromString(fullCidStr));
    }

    return cIds;
  }

  private static void dumpSecondLevelKeys(EtcdNode root, Map<String, String> map) {
    final String mainDir = root.key;
    final int length = mainDir.length() + 1;
    for (EtcdNode node : root.nodes) {
      if (node.dir) {
        throw new IllegalStateException("unexpected to find directories in component instances");
      }
      String key = node.key.substring(length);
      String[] split = key.split("/");
      if (split.length == 1) { // component instance element //
        fillMapWithValue(key, node.value, map);
      } else {
        throw new IllegalStateException("invalid directory structure for key");
        // Map<String,String> map = createComponentInstanceIfNotExistantAndFillWithMap(split[0], retVal);
        // fillMapWithValue(split[1], map);
      }
    }
  }

  private final static Map<String, String> createComponentInstanceIfNotExistantAndFillWithMap(
      String key, Map<ComponentInstanceId, Map<String, String>> retVal) {
    if (DESCRIPTION.equals(key)) {
      return Collections.emptyMap();
    }
    if (NAME.equals(key)) {
      return Collections.emptyMap();
    }
    ComponentInstanceId inst = ComponentInstanceId.fromString(key);
    Map<String, String> map = retVal.get(inst);
    if (map == null) {
      map = new HashMap<>();
      retVal.put(inst, map);
    } else {
      throw new IllegalStateException("unexpected event: map already exists.");
    }
    return map;
  }
    /*
    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        
    } 
    
    private void readObjectNoData() throws ObjectStreamException {
        
    }*/
}
