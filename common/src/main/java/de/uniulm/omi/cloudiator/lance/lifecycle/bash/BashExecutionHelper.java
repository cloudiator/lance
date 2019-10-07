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

package de.uniulm.omi.cloudiator.lance.lifecycle.bash;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import java.util.ArrayList;
import java.util.List;

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import de.uniulm.omi.cloudiator.lance.lifecycle.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BashExecutionHelper {
  private final static Logger LOGGER = LoggerFactory.getLogger(BashExecutionHelper.class);

    private BashExecutionHelper() {
        // no instances of this class //
    }
    
    private static boolean osMatches(OperatingSystem osParam, ExecutionContext ec) {
        return osParam.equals(ec.getOperatingSystem());
    }
    
    private static String buildStringFromCommandLine(String[] cmd) {
        String res = "";
        for(String s : cmd) { 
            res = res + " " + s;
        }
        return res;
    }
    
    static ExecutionResult doExecuteCommand(boolean blocking, String command, Shell shell) {
        if(blocking) {
            return shell.executeBlockingCommand(command);
        } 
        return shell.executeCommand(command); 
    }
    
    static void executeCommands(OperatingSystem osParam, ExecutionContext ec, List<String[]> commands) throws LifecycleException {
      final List<String> errCommandsList = new ArrayList<>();
      if(!osMatches(osParam, ec))
          return;

      Shell shell = ec.getShell();
      for(String[] cmd : commands) {
          String res = buildStringFromCommandLine(cmd);
          ExecutionResult result = doExecuteCommand(false, res, shell);
          processExecResult(result, res, errCommandsList);
      }

      if(errCommandsList.size() > 0) {
        throw new LifecycleException(String.join("\n", errCommandsList));
      }
    }
    
    static void executeBlockingCommands(OperatingSystem osParam, ExecutionContext ec, List<String[]> commands) throws LifecycleException {
        final List<String> errCommandsList = new ArrayList<>();
        if(!osMatches(osParam, ec)) {
          LOGGER.error(String.format("Cannot execute commands %s as operatingsystems %s and %s do not match", commands.toString(), osParam, ec.getOperatingSystem()));
          return;
        }

        Shell shell = ec.getShell();
        final int commandSize = commands.size();
        int counter = 0;
        
        for(String[] cmd : commands) {
            String res = buildStringFromCommandLine(cmd);
            counter++;
            ExecutionResult result = doExecuteCommand(counter == commandSize, res, shell);
            processExecResult(result, res, errCommandsList);
        }

        if(errCommandsList.size() > 0) {
          throw new LifecycleException(String.join("\n", errCommandsList));
        }
    }

    private static void processExecResult(ExecutionResult result, String commandString, List<String> errCommandsList) {
      if(result.isSuccess() || errCommandsList == null) {
        return;
      }

      errCommandsList.add(commandString);
    }
}
