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

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;

final class DockerOperatingSystemTranslator {

  DockerOperatingSystemTranslator() {}

  private static String translateLinux(OperatingSystem os) {
    switch (os.getType()) {
      case UBUNTU:
        return translateUbuntu(os);
      case SUSE:
      default:
        return translateOther(os);
    }
  }

  private static String translateOther(@SuppressWarnings("unused") OperatingSystem os) {
    throw new IllegalArgumentException("os not supported: ");
  }

  private static String translateWindows(OperatingSystem os) {
    return translateOther(os);
  }

  private static String translateUbuntu(OperatingSystem os) {
    return "ubuntu:" + os.getVersion();
  }

  String translate(OperatingSystem os) {
    switch (os.getFamily()) {
      case LINUX:
        return translateLinux(os);
      case WINDOWS:
        return translateWindows(os);
      case BSD:
      default:
        return translateOther(os);
    }
  }
}
