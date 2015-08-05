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
	
	Inprogress(Process _proc, BufferedReader _stdOut, BufferedReader _stdErr) {
		proc = _proc;
		stdOut = _stdOut;
		stdErr = _stdErr;
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
	
	private final char BELL_CHAR = (char) 7;
	String readOutUntilBell() {
		return readAvailable(stdOut, BELL_CHAR);
	}

	public String readOutAvailable() {
		return readAvailable(stdOut, -1);
	}
	
	public String readErrAvailable() {
		return readAvailable(stdErr, -1);
	}
	
	private String readAvailable(BufferedReader std, int terminator) {
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
		try { proc.exitValue(); return false; }
		catch(IllegalThreadStateException ex) {
			return true;
		}
	}

	public ExecResult toExecutionResult() {
		if(processStillRunning()) throw new IllegalStateException("process still running; cannot be drained.");
		
		ExecResultBuilder result = new ExecResultBuilder();
		ProcessWrapper.drainStream(result.output, stdOut);
		ProcessWrapper.drainStream(result.error, stdErr);
		return result.build(proc.exitValue());
	}
	

	@Override
	public ExecutionResult executeBlockingCommand(String command) {
		if(! processStillRunning() ) throw new IllegalStateException();
		try {
			doExecuteCommand("exec " + command);
			String stdOut = readOutAvailable();
			String stdErr = readErrAvailable();
			if(processStillRunning()) { return ExecutionResult.success(stdOut, stdErr); }
			else {						return ExecutionResult.systemFailure(stdErr);	 }
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
	
	private int drainAfterExitStatus(String in) {
		String stdOut = in.trim();
		int index = stdOut.lastIndexOf("\n");
		// at least one element is required for number //
		if(index >= stdOut.length() -1) return -1;
		// it may well be < 0 when the command did not 
		// print anything 
		String toparse = stdOut.substring(index + 1);
		return Integer.parseInt(toparse);
	}

	private static final String EXIT_CODE = "echo \"$?\"";
	static final String BELL_COMMAND = "echo -e \"\\a\""; 
	
	@Override
	public ExecutionResult executeCommand(String command) {
		if(! processStillRunning() ) throw new IllegalStateException();
		try {
			doExecuteCommand(command + "; " + EXIT_CODE + " ; " + BELL_COMMAND);
			String stdOut = readOutUntilBell();
			String stdErr = readErrAvailable();
			int exit = drainAfterExitStatus(stdOut);
			return exit == 0 ? ExecutionResult.success(stdOut, stdErr) : ExecutionResult.commandFailure(exit, stdOut, stdErr);
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
