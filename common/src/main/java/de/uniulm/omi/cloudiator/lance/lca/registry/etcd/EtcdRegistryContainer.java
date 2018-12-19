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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistryContainer;

public final class EtcdRegistryContainer implements RegistryContainer {

  public static final String LCA_REGISTRY_CONFIG_ETCD_HOSTS_KEY =
      "lca.client.config.registry.etcd.hosts";

  private static final Logger LOGGER = LoggerFactory.getLogger(RegistryContainer.class);

  private final EtcdRegistryImpl impl;

  private EtcdRegistryContainer(EtcdRegistryImpl implParam) {
    impl = implParam;
  }

  @Override
  public LcaRegistry getRegistry() {
    return impl;
  }

  public static EtcdRegistryContainer create() throws RegistrationException {
    LOGGER.info("checking for etcd hosts configuration: " + LCA_REGISTRY_CONFIG_ETCD_HOSTS_KEY);
    String value = System.getProperty(LCA_REGISTRY_CONFIG_ETCD_HOSTS_KEY);

    URI[] uris = doCreate(value);
    if (uris == null) {
      LOGGER.warn(
          "no valid etcd host name found, please provide the following information: <hostname1>:<port1>,<hostname2>:<port2>,...; falling back to localhost.");
      uris = doCreate("localhost:4001");
    }

    return new EtcdRegistryContainer(new EtcdRegistryImpl(uris));
  }

  private static URI[] doCreate(String value) {
    if (value == null) return null;
    String[] split = value.split(",");
    if (split.length == 0) return null;
    List<URI> uris = createUris(split);
    if (uris.isEmpty()) {
      return null;
    }
    return uris.toArray(new URI[uris.size()]);
  }

  private static List<URI> createUris(String[] split) {
    List<URI> uris = new ArrayList<>(split.length);

    for (String s : split) {
      URI uri = createUri(s);
      if (uri == null) continue;
      uris.add(uri);
    }

    return uris;
  }

  private static URI createUri(String host) {
    final int colon = host.indexOf(":");
    if (colon == 0) {
      return null;
    }
    String uri = "http://";
    if (colon == -1) {
      uri = uri + host + ":4001";
    } else if (colon == host.length() - 1) {
      uri = uri + host + "4001";
    } else {
      uri = uri + host;
    }
    try {
      return URI.create(uri);
    } catch (IllegalArgumentException ia) {
      LOGGER.warn("problems creating an URI from etcd parameters, ignoring: " + host, ia);
    }
    return null;
  }
}
