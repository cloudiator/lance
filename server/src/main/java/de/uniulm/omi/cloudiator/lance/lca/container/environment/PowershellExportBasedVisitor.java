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

import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkVisitor;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by Daniel Seybold on 10.09.2015. */
public class PowershellExportBasedVisitor implements NetworkVisitor, PropertyVisitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(PowershellExportBasedVisitor.class);

  private final ShellLikeInterface shellLikeInterface;

  public PowershellExportBasedVisitor(PlainShell plainShell) {
    shellLikeInterface = plainShell;
  }

  public void addEnvironmentVariable(String name, String value) {
    // fixme: replace all doubles quotes to single quotes to be able to execute the powershell
    // command;
    // do not use double quotes for powershell
    // ExecutionResult result =
    // shellLikeInterface.executeCommand("[Environment]::SetEnvironmentVariable(\"" + name + "\",
    // \"" + value + " \", \"User\")");
    ExecutionResult result =
        shellLikeInterface.executeCommand(
            "[Environment]::SetEnvironmentVariable('" + name + "', '" + value + " ', 'User')");
    if (!result.isSuccess()) {
      // shellLikeInterface.executeCommand("[Environment]::SetEnvironmentVariable(\"" + name + "\",
      // \"" + value + " \", \"User\")");
      throw new IllegalStateException(
          "could not set environment variables: "
              + name
              + "="
              + value
              + "\n output: "
              + result.getOutput());
    }
    LOGGER.debug("Successfull set env var: " + name + " = " + value);
  }

  @Override
  public void visitNetworkAddress(String name, String address) {
    addEnvironmentVariable(name, address);
  }

  @Override
  public void visitInPort(String fullPortName, String portNr) {
    addEnvironmentVariable(fullPortName, portNr);
  }

  @Override
  public void visitOutPort(String name, String sinkValues) {
    addEnvironmentVariable(name, sinkValues);
  }

  @Override
  public void visit(String propertyName, String propertyValue) {
    addEnvironmentVariable(propertyName, propertyValue);
  }
}
