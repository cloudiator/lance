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
import java.util.Arrays;

public final class OperatingSystem implements Serializable {

    public static final OperatingSystem UBUNTU_14_04 = new OperatingSystem(OperatingSystemType.UBUNTU,
                                                            OperatingSystemVersion.getUbuntuVersion(14, 4));
    
    public static final OperatingSystem WINDOWS_7 = new OperatingSystem(OperatingSystemType.WINDOWS,
                                                            OperatingSystemVersion.getWindowsVersion(WindowsVersion.WIN_7));
    private static final long serialVersionUID = 3848019659106600145L;

    private final OperatingSystemType type;
    private final OperatingSystemVersion version;
    
    public OperatingSystem(OperatingSystemType typeParam, OperatingSystemVersion versionParam) {
        if(! typeParam.checkVersion(versionParam))
            throw new IllegalArgumentException("unknown version");
        
        type = typeParam;
        version = versionParam;
    }
    
    public OperatingSystemFamily getFamily() {
        return type.getFamily();
    }

    public OperatingSystemType getType() {
        return type;
    }

    public String getVersionAsString() {
        return version.toString();
    }
    
    public OperatingSystemVersion getVersion() {
        return version;
    }
    
    @Override
    public String toString() {
        return type.toString() + ":" + version.toString();
    }
    
    public boolean isLinuxOs() {
        return type.getFamily() == OperatingSystemFamily.LINUX;
    }
    
    public static OperatingSystem fromString(String ostype, String osversion) {
        OperatingSystemType t = OperatingSystemType.fromString(ostype);
        if(t == null) {
            String s = Arrays.toString(OperatingSystemType.values());
            throw new IllegalArgumentException("operating system type " + ostype + " not known. Only the following" + 
                    " types are known: " + s);
        }
        final OperatingSystemVersion version;
        if(osversion == null || osversion.isEmpty()) {
            // if(t.hasDefaultVersion()) 
            // returns null of DefaultVersion is not available
            version = t.getDefaultVersion();
        } else if(t.checkVersionString(osversion)){
                version = new OperatingSystemVersion(osversion);
        } else {
            version = null;
        }
        
        if(version != null) {
            return new OperatingSystem(t, version);
        }
        
        throw new IllegalArgumentException(osversion + " is not a valid version for OperatingSytemType " + t + " or no "
                    + "default version known for this type.");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if(! (o instanceof OperatingSystem)) {
            return false;
        }
        OperatingSystem that = (OperatingSystem) o;
        return this.type.equals(that.type) && this.version.equals(that.version);
    }

    public String getNonBlockingPackageInstallerCommand() {
        return type.getNonBlockingPackageInstallerCommandForVersion(version);
    }
}
