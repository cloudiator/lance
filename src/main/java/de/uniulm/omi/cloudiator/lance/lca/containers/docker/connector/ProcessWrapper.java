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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProcessWrapper {
	
	private final static Logger log = Logger.getLogger(ProcessBasedConnector.class.getName());
	
	private final Process proc;
	private final InputStream stdout;
	private final InputStream stderr; 
	private final OutputStream stdin; 
	
	private ProcessWrapper(Process _proc, OutputStream _stdin, InputStream _stdout, InputStream _stderr) {
		proc = _proc;
		stdin = new BufferedOutputStream(_stdin);
		stdout = new BufferedInputStream(_stdout);
		stderr = new BufferedInputStream(_stderr);
	}
	
	private static ProcessWrapper createWrappper(ProcessBuilder pb) {
		try {
			Process proc = pb.start();
			return new ProcessWrapper(proc, proc.getOutputStream(), proc.getInputStream(), proc.getErrorStream());
		} catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}
	}
	
	private static List<String> argsAsDockerList(String ...args) {
		List<String> l = new ArrayList<String>();
		l.add("sudo");
		l.add("docker");
		for(String s : args) {
			l.add(s);
		}
		return l;
	}
	
	public static Inprogress progressingDockerCommand(String ... args) {
		ProcessWrapper pw = createProcessWrapper(argsAsDockerList(args));
		Inprogress prog = new Inprogress(pw.proc, createReader(pw.stdout), createReader(pw.stderr));
		
		try {
			prog.doExecuteCommand(Inprogress.BELL_COMMAND);
			if(prog.processStillRunning()) {
				String drowned = prog.readOutUntilBell();
				drowned = drowned + "///" + prog.readErrAvailable();
				log.info("created log running command: '" + drowned + "'");
			}
		} catch (IOException ioe) {
			log.log(Level.WARNING, "cannot start progressing command.", ioe);
			prog.close();
		}
		
		return prog;
	}

	public static ExecResult singleCommand(String ... args) {
		return doExecuteSingleCommand(Arrays.asList(args));
	}
	
	public static ExecResult singleDockerCommand(String ... args) {
		return doExecuteSingleCommand(argsAsDockerList(args));
	}
	
	private static ExecResult doExecuteSingleCommand(List<String> args) {
		ProcessWrapper pw = createProcessWrapper(args);
		ExecResultBuilder result = new ExecResultBuilder();
		pw.closeStdIn();
		pw.drainStdOut(result.output);
		pw.drainStdErr(result.error);
		while(true) {
			try { return result.build(pw.proc.waitFor()); }
			catch(InterruptedException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	private static ProcessWrapper createProcessWrapper(List<String> l) {
		ProcessBuilder pb = new ProcessBuilder(l);
		ProcessWrapper pw = ProcessWrapper.createWrappper(pb);
		if(pw == null) {
			throw new RuntimeException("cannot instantiate process wrapper");
		}
		return pw;
	}

	private void drainStdErr(StringBuilder builder) {
		drainStream(builder, createReader(stderr));
	}
	
	private void drainStdOut(StringBuilder builder) {
		drainStream(builder, createReader(stdout));
	}
	
	static void drainStream(StringBuilder builder, BufferedReader reader) {
		String line = "";
		while(line != null) {
			String suffix = builder.length() != 0 ? "\n" : "";
			builder.append(line).append(suffix);

			try { line = reader.readLine(); }
			catch(IOException ioe) {
				ioe.printStackTrace();
				line = null;
			}
		}
	}
	
	private static BufferedReader createReader(InputStream in) {
		return new BufferedReader(new InputStreamReader(in));
	}
	
	private void closeStdIn() {
		try {
			stdin.close();
		} catch(IOException ioe) {
			System.err.print("IOException when closing process wrapper input");
		}
	}
}
