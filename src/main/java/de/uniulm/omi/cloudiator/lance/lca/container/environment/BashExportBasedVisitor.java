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

package de.uniulm.omi.cloudiator.lance.lca.container.environment;

import java.util.List;

import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortHierarchyLevel;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;

public final class BashExportBasedVisitor implements NetworkVisitor, PropertyVisitor {

    private final ShellLikeInterface interfce;
    
    public BashExportBasedVisitor(ShellLikeInterface ifc) {
        interfce = ifc;
    }
    
    public void addEnvironmentVariable(String name, String value) {
        ExecutionResult result = interfce.executeCommand("export " + name + "=" + value);
        if(result.isSuccess()) {
            interfce.executeCommand("echo export " + name + "=" + value);
            return;
        }
        throw new IllegalStateException("could not set environment variables: " + name + "=" + value);
    }

    @Override
    public void visitNetworkAddress(PortHierarchyLevel level, String address) {
        addEnvironmentVariable(level.getName().toUpperCase() + "_IP", address);
    }

    @Override
    public void visitInPort(String portName, PortHierarchyLevel level, Integer portNr) {
        addEnvironmentVariable(level.getName().toUpperCase() + "_" + portName.toUpperCase(), portNr.toString());
    }

    @Override
    public void visitOutPort(String portName, PortHierarchyLevel level, List<DownstreamAddress> sinks) {
        String value = "";
        for(DownstreamAddress element : sinks) {
            if(!value.isEmpty()) {
            	value = value + ","; 
            }
            value = value + element.toString();
        }
        addEnvironmentVariable(level.getName().toUpperCase() + "_" + portName, value);
    }

    @Override
    public void visit(String propertyName, String propertyValue) {
        addEnvironmentVariable(propertyName, propertyValue);
    }
}
