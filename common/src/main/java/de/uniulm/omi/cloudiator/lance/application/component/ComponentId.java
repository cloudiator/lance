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

package de.uniulm.omi.cloudiator.lance.application.component;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.UUID;

public final class ComponentId implements Serializable {

  // FIXME: set fixed value!
  public static final ComponentId ERROR_ID = new ComponentId();
  public static final ComponentId SYSTEM_ID = new ComponentId();
  private static final long serialVersionUID = 4750738467522891916L;

  private final String id;

  public ComponentId() {
    id = UUID.randomUUID().toString();
  }

  private ComponentId(String str) {
    this.id = str;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ComponentId)) {
      return false; // captures null
    }
    ComponentId that = (ComponentId) obj;
    boolean same = this.id.equals(that.id);
    return same;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return id;
  }

  public static ComponentId fromString(String str) {
    checkNotNull(str, "s is null");
    return new ComponentId(str);
  }
}