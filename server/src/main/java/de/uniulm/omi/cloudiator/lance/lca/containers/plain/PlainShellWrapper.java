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

package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import de.uniulm.omi.cloudiator.lance.lifecycle.Shell;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Daniel Seybold on 11.08.2015.
 */
public class PlainShellWrapper implements Shell {

    private static final Logger LOGGER = LoggerFactory.getLogger(Shell.class);

    final PlainShell plainShell;

    PlainShellWrapper(PlainShell plainShell){
        this.plainShell = plainShell;
    }

    @Override
    public ExecutionResult executeCommand(String command) {
        ExecutionResult result = this.plainShell.executeCommand(command);
        checkResult(command, result);
        return result;
    }

    @Override
    public ExecutionResult executeBlockingCommand(String command){
        ExecutionResult result = this.plainShell.executeBlockingCommand(command);
        checkResult(command, result);
        return result;
    }

  @Override
  public List<String> getExecCommandsMemory() {
    LOGGER.error("function not implemented yet.");
    return null;
  }

  private static void checkResult(String command, ExecutionResult result) {
        if(result.isSuccess()) {
            return;
        }
        LOGGER.warn("unsuccessull command '" + command + "': " + result.toString());
    }
}
