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

package de.uniulm.omi.cloudiator.lance.container.spec.os;

public enum OperatingSystemType {
  SUSE(OperatingSystemFamily.LINUX, OSVersionFormat.UNKNOWN_VERSION_FORMAT),
  UBUNTU(OperatingSystemFamily.LINUX, OSVersionFormat.UBUNTU_VERSION_FORMAT) {
    @Override
    public String getNonBlockingPackageInstallerCommandForVersion(OperatingSystemVersion version) {
      return "sudo apt-get install -y";
    }

    @Override
    public OperatingSystemVersion getDefaultVersion() {
      return OperatingSystemVersion.getUbuntuVersion(14, 4);
    }

    @Override
    public boolean hasDefaultVersion() {
      return true;
    }
  },
  ARCH(OperatingSystemFamily.LINUX, OSVersionFormat.UNKNOWN_VERSION_FORMAT),

  WINDOWS(OperatingSystemFamily.WINDOWS, OSVersionFormat.WINDOWS_VERSION_FORMAT),

  MAC(OperatingSystemFamily.BSD, OSVersionFormat.UNKNOWN_VERSION_FORMAT),
  FREE_BSD(OperatingSystemFamily.BSD, OSVersionFormat.UNKNOWN_VERSION_FORMAT),
  OPEN_BSD(OperatingSystemFamily.BSD, OSVersionFormat.UNKNOWN_VERSION_FORMAT),
  ;

  private final OperatingSystemFamily f;
  private final OSVersionFormat format;

  private OperatingSystemType(OperatingSystemFamily familyParam, OSVersionFormat formatParam) {
    f = familyParam;
    format = formatParam;
  }

  /**
   * @param name the symbolic name of the operatig system type
   * @return the OperatingSystemType instance, if <i>name</i> matches the toString() representation
   *     of this OperatingSystemType ignoring case. Otherwise <i>null</i> is returned.
   */
  public static OperatingSystemType fromString(String name) {
    for (OperatingSystemType t : OperatingSystemType.values()) {
      if (t.toString().equalsIgnoreCase(name)) {
        return t;
      }
    }
    return null;
  }

  public boolean checkVersionString(String versionParam) {
    return checkVersion(new OperatingSystemVersion(versionParam));
  }

  public boolean checkVersion(OperatingSystemVersion v) {
    return v.checkFormatting(this.format);
  }

  public OperatingSystemFamily getFamily() {
    return f;
  }

  public boolean hasDefaultVersion() {
    return false;
  }

  /**
   * @return the default version or null if DefaultVersion is not available for this OSType
   *     <p>needs to be overridden by each type.
   */
  public OperatingSystemVersion getDefaultVersion() {
    return null;
  }

  public String getNonBlockingPackageInstallerCommandForVersion(
      @SuppressWarnings("unused") OperatingSystemVersion version) {
    throw new UnsupportedOperationException();
  }
}
