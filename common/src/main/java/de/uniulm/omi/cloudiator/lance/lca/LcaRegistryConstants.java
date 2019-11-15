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

package de.uniulm.omi.cloudiator.lance.lca;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.uniulm.omi.cloudiator.lance.lca.LcaRegistryConstants.Identifiers.*;

public final class LcaRegistryConstants {

    public enum Identifiers { CONTAINER_STATUS, COMPONENT_INSTANCE_STATUS,
      PUBLIC_IP, CLOUD_IP, CONTAINER_IP, PUBLIC_PORT, CLOUD_PORT, CONTAINER_PORT,
      DYN_GROUP_KEY, DYN_HANDLER_KEY, INSTANCE_NR, FUNCTION_HANDLER};

    public static final Map<Identifiers, String> regEntries;
    static {
      Map<Identifiers , String> aMap = new HashMap<>();
      aMap.put(CONTAINER_STATUS, "Container_Status");
      aMap.put(COMPONENT_INSTANCE_STATUS, "Component_Instance_Status");
      aMap.put(PUBLIC_IP, "HOST_PUBLIC_IP");
      aMap.put(CLOUD_IP, "HOST_CLOUD_IP");
      aMap.put(CONTAINER_IP, "HOST_CONTAINER_IP");
      aMap.put(PUBLIC_PORT, "HOST_PUBLIC_PORT");
      aMap.put(CLOUD_PORT, "HOST_CLOUD_PORT");
      aMap.put(CONTAINER_PORT, "HOST_CONTAINER_PORT");
      aMap.put(DYN_GROUP_KEY, "dynamicgroup");
      aMap.put(DYN_HANDLER_KEY, "dynamichandler");
      aMap.put(INSTANCE_NR, "Instance_Number");
      aMap.put(FUNCTION_HANDLER, "FUNCTION_HANDLER");
      regEntries = Collections.unmodifiableMap(aMap);
    }

    /*public static final String CLOUD_PROVIDER_ID = "Cloud_Provider_Id";
    public static final String HOST_INTERNAL_IP = "Host_Internal_Ip";
    public static final String LOCAL_IP = "Local_Ip";*/

    private LcaRegistryConstants () {
        // 
    }
}
