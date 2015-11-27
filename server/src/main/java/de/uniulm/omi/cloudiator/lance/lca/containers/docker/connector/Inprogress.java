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

package de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerShell;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;

class Inprogress implements DockerShell {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerShell.class);
    
    private final Process proc;
    private final BufferedReader stdOut;
    private final BufferedReader stdErr;
    private final BufferedWriter stdIn;
    
    Inprogress(Process procParam, BufferedReader stdOutParam, BufferedReader stdErrParam) {
        proc = procParam;
        stdOut = stdOutParam;
        stdErr = stdErrParam;
        stdIn = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
    }
    
    /*private String readInLine() {
        try {
            return stdOut.readLine();
        } catch(IOException ioe) {
            logger.warn("IOExeception when reading line from external process", ioe);
            return null;
        }
    }*/
    
    private static final char BELL_CHAR = (char) 7;
    String readOutUntilBell() {
        return readAvailable(stdOut, BELL_CHAR);
    }

    public String readOutAvailable() {
        return readAvailable(stdOut, -1);
    }
    
    public String readErrAvailable() {
        return readAvailable(stdErr, -1);
    }
    
    private static String readAvailable(BufferedReader std, int terminator) {
        StringBuilder builder = new StringBuilder();
        try { 
            while((terminator > 0) || std.ready()) {
                int i = std.read();
                if(i == -1 || i == terminator) {
                    break;
                }
                builder.append((char) i);
            }
        } catch(IOException ioe){
            LOGGER.info("reading available bytes terminated due to exception", ioe);
        }
        return builder.toString();
    }

    public boolean processStillRunning() {
        try { 
            proc.exitValue(); 
            return false; 
        } catch(IllegalThreadStateException ex) {
        	// do not print this exception. it is not an error
        	// it depends on the calling method to figure out
        	// if this the process shall be running or not
            // LOGGER.debug("process not terminated", ex);
            return true;
        }
    }

    public ExecResult toExecutionResult() {
        if(processStillRunning()) 
            throw new IllegalStateException("process still running; cannot be drained.");
        
        ExecResultBuilder result = new ExecResultBuilder();
        ProcessWrapper.drainStream(result.output, stdOut);
        ProcessWrapper.drainStream(result.error, stdErr);
        return result.build(proc.exitValue());
    }
    

    @Override
    public ExecutionResult executeBlockingCommand(String command) {
        if(! processStillRunning() ) 
            throw new IllegalStateException();
        try {
            doExecuteCommand("exec " + command);
            String tmpOut = readOutAvailable();
            String tmpErr = readErrAvailable();
            if(processStillRunning()) { 
                return ExecutionResult.success(tmpOut, tmpErr); 
            }
            return ExecutionResult.systemFailure(tmpErr);
        } catch(IOException ioe) {
            LOGGER.warn("problem when reading from external process", ioe);
            return ExecutionResult.systemFailure(ioe.getMessage());
        } catch(Exception t){
            LOGGER.warn("problem when reading from external process", t);
            return ExecutionResult.systemFailure(t.getMessage());
        }
    }
    
    void doExecuteCommand(String command) throws IOException {
        stdIn.write(command, 0, command.length());
        stdIn.newLine();
        stdIn.flush();
    }
    
    private static int drainAfterExitStatus(String in) {
        String tmpOut = in.trim();
        int index = tmpOut.lastIndexOf("\n");
        // at least one element is required for number //
        if(index >= tmpOut.length() -1) {
            return -1;
        }
        // it may well be < 0 when the command did not 
        // print anything 
        String toparse = tmpOut.substring(index + 1);
        return Integer.parseInt(toparse);
    }

    private static final String EXIT_CODE = "echo \"$?\"";
    static final String BELL_COMMAND = "echo -e \"\\a\""; 
    
    @Override
    public ExecutionResult executeCommand(String command) {
        if(! processStillRunning() ) {
            throw new IllegalStateException("shell not available for executing command: " + command);
        }
        try {
            doExecuteCommand(command + "; " + EXIT_CODE + " ; " + BELL_COMMAND);
            String tmpOut = readOutUntilBell();
            String tmpErr = readErrAvailable();
            int exit = drainAfterExitStatus(tmpOut);
            return exit == 0 ? ExecutionResult.success(tmpOut, tmpErr) : ExecutionResult.commandFailure(exit, tmpOut, tmpErr);
        } catch(IOException ioe) {
            LOGGER.warn("problem when reading from external process", ioe);
            return ExecutionResult.systemFailure(ioe.getMessage());
        } catch(Exception t){
            LOGGER.warn("problem when reading from external process", t);
            return ExecutionResult.systemFailure(t.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            stdIn.close();
            stdOut.close();
            stdErr.close();
        } catch(IOException ioe) {
            LOGGER.info("exception when closing InProgress shell", ioe);
        }
    }
}
