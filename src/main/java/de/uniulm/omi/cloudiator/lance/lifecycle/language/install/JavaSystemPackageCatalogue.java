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

package de.uniulm.omi.cloudiator.lance.lifecycle.language.install;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystemVersion;

public final class JavaSystemPackageCatalogue {

    public static String getJdkPackageName(String version, OperatingSystem os) {
        switch(os.getType()){
        case UBUNTU:
            return getUbuntuInstallCommand(version, os);
        default:
            return null;
            // new IllegalArgumentException("Java installation for operating system " + os + " not supported");
        }
    }
    
    private static String getUbuntuInstallCommand(String version, OperatingSystem os) {
        if(OperatingSystemVersion.getUbuntuVersion(14, 4).equals(os.getVersion())){
            if(isJava7(version)) return "openjdk-7-jdk";
        }
        throw new IllegalArgumentException("Java (" + version + ") installation for operating system " + os + " not supported");
    }
    
    private JavaSystemPackageCatalogue() {
        // no instances //
    }
    
    private static boolean isJava7(String version) {
        return "7".equals(version);
    }

    static SystemApplication getSystemApplication(final String version) {
        if(isJava7(version)) {
            return new SystemApplication() {

                @Override
                public String getPackageName(OperatingSystem os) {
                    return getJdkPackageName(version, os);
                }
            };
        }
        throw new IllegalArgumentException("version " + version + " currently not supported for Java.");
    }
}
