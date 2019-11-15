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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortHierarchyLevel;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;

public final class BashExportBasedVisitor implements NetworkVisitor, PropertyVisitor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OutPort.class);
	
    private final ShellLikeInterface interfce;
    
    public BashExportBasedVisitor(ShellLikeInterface ifc) {
        interfce = ifc;
    }
    
    private void addEnvironmentVariable(String name, String value) {
        interfce.setEnvironmentVariable(name, value);
    }

    @Override
    public void visitNetworkAddress(String name, String address) {
    	LOGGER.info("exporting network address as environment variable: " + name + " = " + address);
        addEnvironmentVariable(name, address);
    }

    @Override
    public void visitInPort(String fullPortName, String portNr) {
    	LOGGER.info("exporting inPort as environment variable: " + fullPortName + " = " + portNr);
        addEnvironmentVariable(fullPortName, portNr);
    }

    @Override
    public void visitOutPort(String name, String sinkValues) {
        LOGGER.info("exporting out port as environment variable: " + name + " = " + sinkValues);
        addEnvironmentVariable(name, sinkValues);
    }

    @Override
    public void visit(String propertyName, String propertyValue) {
        addEnvironmentVariable(propertyName, propertyValue);
    }
}
