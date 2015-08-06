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

import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortLinkage;

public final class PortReference implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final ComponentId compId;
    private final String propertyName;
    // private final PortLinkage linkage;
    
    public PortReference(ComponentId _compId, String _propertyName, @SuppressWarnings("unused") PortLinkage _linkage) {
        compId = _compId;
        propertyName = _propertyName;
       // linkage = _linkage;
    }

    public String getPortName() { return propertyName; }

    public ComponentId getComponentId() { return compId; }

}
