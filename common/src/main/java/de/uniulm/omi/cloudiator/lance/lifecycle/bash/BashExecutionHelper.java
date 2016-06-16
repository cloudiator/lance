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

import java.util.List;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.lifecycle.Shell;

final class BashExecutionHelper {
    
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
    
    static ExecutionResult doExecuteCommand(boolean blocking, String command, Shell shell) throws LifecycleException {
    	ExecutionResult res = null;
        if(blocking) {
            res = shell.executeBlockingCommand(command);
        } 
        res = shell.executeCommand(command);
        if(res.isSuccess()) { 
        	return res;
        }
        throw new LifecycleException("execution of command '" + command + "' failed: " + res);
    }
    
    static void executeCommands(OperatingSystem osParam, ExecutionContext ec, List<String[]> commands) throws LifecycleException {
          if(!osMatches(osParam, ec))
              return;
          
          Shell shell = ec.getShell();
          for(String[] cmd : commands) {
              String res = buildStringFromCommandLine(cmd);
              doExecuteCommand(false, res, shell);
          }
    }
    
    static void executeBlockingCommands(OperatingSystem osParam, ExecutionContext ec, List<String[]> commands) throws LifecycleException {
        if(!osMatches(osParam, ec))
            return;
        
        Shell shell = ec.getShell();
        final int commandSize = commands.size();
        int counter = 0;
        
        for(String[] cmd : commands) {
            String res = buildStringFromCommandLine(cmd);
            counter++;
            doExecuteCommand(counter == commandSize, res, shell);
        }
    }
}
