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

package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


final class DockerOperatingSystemTranslator {
  private final static Logger LOGGER = LoggerFactory.getLogger(DockerOperatingSystemTranslator.class);

    DockerOperatingSystemTranslator(){
        
    }
    
    String translate(OperatingSystem os) {
      if (OperatingSystemUtils.isLinux(os.operatingSystemFamily())) {
        return translateLinux(os);
      } else if (os.operatingSystemFamily() == OperatingSystemFamily.WINDOWS) {
        return  translateWindows(os);
      } else {
        return translateOther(os);
      }
    }
    
    private static String translateLinux(OperatingSystem os) {
      if (os.operatingSystemFamily() == OperatingSystemFamily.UBUNTU) {
        return translateUbuntu(os);
      }

      LOGGER.info("Except ubuntu, currently only images tagged with 'latest' are used");
      return os.operatingSystemFamily().name().toLowerCase() + ":latest";
    }

    private static String translateUbuntu(OperatingSystem os) {
      final String versionStr = os.operatingSystemVersion().version().toString();

      if (versionStr.length() != 4) {
        throw new IllegalArgumentException(String.format("cannot translate ubuntu os for docker "
            + "as version %s is unknown", versionStr));
      }

      final String year = versionStr.substring(0,1);
      final String month = versionStr.substring(2,3);

      return String.format("%s:%s_%s", os.operatingSystemFamily().name().toLowerCase(),
          year, month);
    }

    private static String translateOther(@SuppressWarnings("unused") OperatingSystem os) {
        throw new IllegalArgumentException("os not supported: ");
    }
    
    private static String translateWindows(OperatingSystem os) {
        return translateOther(os);
    }
}
