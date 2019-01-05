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

import java.io.Serializable;
import java.util.Calendar;

public final class OperatingSystemVersion implements Serializable {

  private static final long serialVersionUID = 1066645702254395280L;
  private final String version;

  public OperatingSystemVersion(String s) {
    version = s;
  }

  /**
   * @param year in 'y' or 'yy' format
   * @param month either 10 or 4
   * @return returns the operating system object
   */
  public static OperatingSystemVersion getUbuntuVersion(int year, int month) {
    if (year > 6
        && year <= (Calendar.getInstance().get(Calendar.YEAR) % 100)
        && (month == 4 || month == 10)) {

      String tmpyear = year + "";
      if (year < 10) {
        tmpyear = "0" + tmpyear;
      }
      String tmpmonth = month + "";
      if (month < 10) {
        tmpmonth = "0" + tmpmonth;
      }

      return new OperatingSystemVersion(tmpyear + "." + tmpmonth);
    }
    throw new IllegalArgumentException(
        "not a valid ubuntu release combination: " + year + "." + month);
  }

  public static OperatingSystemVersion getWindowsVersion(WindowsVersion version) {
    return new OperatingSystemVersion(version.toString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof OperatingSystemVersion)) {
      return false;
    }
    OperatingSystemVersion that = (OperatingSystemVersion) o;
    return this.version.equals(that.version);
  }

  public boolean checkFormatting(OSVersionFormat format) {
    return format.hasCorrectFormat(version);
  }

  @Override
  public String toString() {
    return version;
  }
}
