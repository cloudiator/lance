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

package de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell;

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystemUtils;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel Seybold on 11.08.2015.
 */
public class PlainShellImpl implements PlainShell {

    static final Logger LOGGER = LoggerFactory.getLogger(PlainShell.class);

    private ProcessBuilder processBuilder = new ProcessBuilder();
    private final OperatingSystem opSys;
    private final List<String> osShell = new ArrayList<>();

    public PlainShellImpl(OperatingSystem operatingSystem) {
    	opSys = operatingSystem;
        //add the os respective shells for execution
      if (operatingSystem.operatingSystemFamily() == OperatingSystemFamily.WINDOWS) {
            this.osShell.add("powershell.exe");
            this.osShell.add("-command");
      } else if (OperatingSystemUtils.isLinux(operatingSystem.operatingSystemFamily())) {
            this.osShell.add("/bin/bash");
            this.osShell.add("-c");
        } else {
            throw new IllegalStateException("Unkown OS family: " + operatingSystem.operatingSystemFamily().name());
        }
    }

    @Override public ExecutionResult executeCommand(String command) {
        ExecutionResult executionResult;
        Process shellProcess;

        List<String> commands = this.buildCommand(command);

        try {
            shellProcess = this.processBuilder.command(commands).start();

            //just for debugging
            List<String> debuglist = this.processBuilder.command();
            debuglist.stream().forEach((string) -> {
                LOGGER.debug("Content: " + string);
            });

            StringBuilder commandOut = new StringBuilder();
            StringBuilder errorOut = new StringBuilder();
            Thread t1 = createDrainThread(commandOut, shellProcess.getInputStream());
            Thread t2 = createDrainThread(errorOut, shellProcess.getErrorStream());
            t1.start();
            t2.start();
            
            while(true) {
            	try {
            		t1.join();
            		t2.join();
            		break;
            	} catch(InterruptedException ie) {
            		 LOGGER.debug("caught interrupted exception: ", ie);
            	}
            }
            
            //important, wait for or it will run in an deadlock!!, adapt execution result maybe
            int exitValue = shellProcess.waitFor();
            LOGGER.debug("ExitCode: " + exitValue);

            executionResult = createExecutionResult(exitValue, commandOut.toString(), errorOut.toString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("Error while executing command: " + command, e);
            executionResult = ExecutionResult.systemFailure(e.getLocalizedMessage());
            return executionResult;
        }
        return executionResult;
    }
    
    private static Thread createDrainThread(StringBuilder out, InputStream stream) {
    	Thread t = new Thread(){
        	public void run() {
        		try {
        			String s = extractCommandOutput(stream);
        			LOGGER.debug(s);
        			out.append(s);
        		} catch(Exception ex) {
        			LOGGER.debug("failed when draining stream", ex);
        		}
        	}
        };
        return t;
    }


    private List<String> buildCommand(String commandLine) {

        List<String> commandList = new ArrayList<>();
        //Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(commandLine);
        //while (m.find())
        //    commandList.add(m.group(1)); // Add .replace("\"", "") to remove surrounding quotes.
        //create list with os specific commands
        commandList.addAll(this.osShell);

        //TODO: check if already wrapped
        commandList.add(commandLine);

        return commandList;
    }

    @Override public ExecutionResult executeBlockingCommand(String command) {

        //FIXME: implement this in a non-blocking way, check if blocking command are necessary
    	// int exitValue = shellProcess.waitFor(); will block forever otherwise.
        LOGGER.warn("Using currently same impl for blocking/nonblocking execution of commands!");
        return this.executeCommand(command);
    }


    @Override public void close() {
        //FIXME: implement this, check what needs to be closed or process killed?
    	// Comment (jd): should be fine. As we run external processes, closing 
    	// the shell should not be needed
        LOGGER.warn("Closing PlainShellImpl needs to be implemented!");
    }

    @Override public void setDirectory(String directory) {
        this.processBuilder.directory(new File(directory));

        LOGGER.info(this.processBuilder.directory().getAbsoluteFile().toString());
    }

    private static ExecutionResult createExecutionResult(int exitValue, String commandOut, String errorOut) {
        ExecutionResult executionResult;
        if (exitValue == 0) {
            executionResult = ExecutionResult.success(commandOut, errorOut);
        } else {
            executionResult = ExecutionResult.commandFailure(exitValue, commandOut, errorOut);
        }
        return executionResult;
    }

    private static String extractCommandOutput(InputStream stream) {
        StringBuilder builder = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream));) {
            String line;            
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                LOGGER.debug(line);
                builder.append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            LOGGER.error("Error while reading process outputstream", e);
            e.printStackTrace();
        }

        LOGGER.debug("captured " + builder.length() + " elements to string builder.");
        return builder.toString();
    }

	@Override
	public void setEnvironmentVariable(String key, String value) {
		processBuilder.environment().put(key, value);
		LOGGER.info("exporting environment variable: " + key + " = " + value);
	}
}
