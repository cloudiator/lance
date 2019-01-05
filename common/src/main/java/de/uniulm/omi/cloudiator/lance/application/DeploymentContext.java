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

package de.uniulm.omi.cloudiator.lance.application;

import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import java.io.Serializable;
import java.util.HashMap;

public final class DeploymentContext implements Serializable {

  private static final long serialVersionUID = 4924998330675206170L;
  private final ApplicationId myAppId;
  private final ApplicationInstanceId myInstanceId;
  private final LcaRegistry registry;

  private final HashMap<String, Object> properties = new HashMap<>();
  private final HashMap<String, Class<?>> propertyTypes = new HashMap<>();

  public DeploymentContext(ApplicationId appId, ApplicationInstanceId instanceId, LcaRegistry re) {
    myAppId = appId;
    myInstanceId = instanceId;
    registry = re;
  }

  public void setProperty(String name, Object value, Class<?> type) {
    properties.put(name, value);
    propertyTypes.put(name, type);
  }

  public Object getProperty(String name, Class<?> clazz) {
    Class<?> clazz2 = propertyTypes.get(name);
    if (clazz2 == null) {
      throw new IllegalStateException("unknown property: " + name);
    }
    if (clazz2 != clazz) {
      throw new IllegalArgumentException("types do not match: " + clazz + " vs. " + clazz2);
    }
    return properties.get(name);
  }

  public LcaRegistry getRegistry() {
    return registry;
  }

  public ApplicationInstanceId getApplicationInstanceId() {
    return myInstanceId;
  }

  public ApplicationId getApplicationId() {
    return myAppId;
  }

  @Override
  public String toString() {
    return "Context: " + myAppId + "/" + myInstanceId + ":" + properties;
  }
}
