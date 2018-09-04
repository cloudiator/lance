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

import java.io.Serializable;

import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortType;

public final class InPort implements Serializable {

    private static final long serialVersionUID = -6375556080278116537L;
    private final String name;
    private final PortType type;
    // private final int cardinality;
    
    InPort(String nameParam, PortType typeParam, @SuppressWarnings("unused") int cardinalityParam) {
        name = nameParam;
        type = typeParam;
        // cardinality = _cardinality;
    }
    
    public boolean isPublic() {
        return type == PortType.PUBLIC_PORT;
    }

    public String getPortName() { 
    	return name; 
    }
}
