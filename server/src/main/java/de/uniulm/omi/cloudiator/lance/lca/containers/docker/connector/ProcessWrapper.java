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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProcessWrapper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);
    
    private final Process proc;
    private final InputStream stdout;
    private final InputStream stderr; 
    private final OutputStream stdin; 
    
    private ProcessWrapper(Process procParam, OutputStream stdinParam, InputStream stdoutParam, InputStream stderrParam) {
        proc = procParam;
        stdin = new BufferedOutputStream(stdinParam);
        stdout = new BufferedInputStream(stdoutParam);
        stderr = new BufferedInputStream(stderrParam);
    }
    
    private static ProcessWrapper createWrappper(ProcessBuilder pb) {
        try {
            Process proc = pb.start();
            return new ProcessWrapper(proc, proc.getOutputStream(), proc.getInputStream(), proc.getErrorStream());
        } catch(IOException ioe){
            LOGGER.info("could not start external process", ioe); 
            return null;
        }
    }
    
    private static List<String> argsAsDockerList(String ...args) {
        List<String> l = new ArrayList<>();
        l.add("sudo");
        l.add("docker");

        //If a quote appears -> treat all following Strings (incl. the quote) until next quote appears as one arg
        //including the quote
        boolean matchingStart = false;
        String patternStringStart = "^[\"'].*";
        String patternStringEnd = ".*[\"']$";
        Pattern patternStart = Pattern.compile(patternStringStart);
        Pattern patternEnd = Pattern.compile(patternStringEnd);
        StringBuilder builder = new StringBuilder();

      for(String s : args) {
        if (!matchingStart) {
          Matcher matcherStart = patternStart.matcher(s);
          matchingStart = matcherStart.matches();
          //If String without tabs/spaces is quoted
          Matcher matcherEnd = patternEnd.matcher(s);
          boolean matchingEnd = matcherEnd.matches();

          if(matchingStart && !matchingEnd) {
            builder = new StringBuilder();
            builder.append(s + " ");
          } else {
            l.add(s);
            matchingStart = false;
          }
        } else {
          builder.append(s + " ");
          Matcher matcher = patternEnd.matcher(s);
          boolean matchingEnd = matcher.matches();

          if (matchingEnd) {
            matchingStart = false;
            String quotedArg = builder.toString();
            l.add(quotedArg);
          }
        }
      }

      return l;
    }
    
    public static Inprogress progressingDockerCommand(String ... args) throws DockerException {
        ProcessWrapper pw = createProcessWrapper(argsAsDockerList(args));
        Inprogress prog = new Inprogress(pw.proc, createReader(pw.stdout), createReader(pw.stderr));
        
        try {
            prog.doExecuteCommand(Inprogress.BELL_COMMAND);
            if(prog.processStillRunning()) {
                String drowned = prog.readOutUntilBell();
                drowned = drowned + "///" + prog.readErrAvailable();
                LOGGER.info("created log running command: '" + drowned + "'");
            }
        } catch (IOException ioe) {
            LOGGER.warn("cannot start progressing command.", ioe);
            prog.close();
        }
        
        return prog;
    }

    public static ExecResult singleCommand(String ... args) throws DockerException {
        return doExecuteSingleCommand(Arrays.asList(args));
    }
    
    public static ExecResult singleDockerCommand(String ... args) throws DockerException {
        return doExecuteSingleCommand(argsAsDockerList(args));
    }
    
    private static ExecResult doExecuteSingleCommand(List<String> args) throws DockerException {
        ProcessWrapper pw = createProcessWrapper(args);
        ExecResultBuilder result = new ExecResultBuilder();
        pw.closeStdIn();
        pw.drainStdOut(result.output);
        pw.drainStdErr(result.error);
        while(true) {
            try { 
                return result.build(pw.proc.waitFor()); 
            } catch(InterruptedException ioe) {
                LOGGER.info("IOException when waiting for external process to terminate", ioe);
            }
        }
    }
    
    private static ProcessWrapper createProcessWrapper(List<String> l) throws DockerException {
        ProcessBuilder pb = new ProcessBuilder(l);
        ProcessWrapper pw = ProcessWrapper.createWrappper(pb);
        if(pw == null) {
            throw new DockerException("cannot instantiate process wrapper");
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

            try { 
                line = reader.readLine(); 
            } catch(IOException ioe) {
                LOGGER.info("could not fully drain process stream", ioe);
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
            LOGGER.info("IOException when closing process wrapper input", ioe);
        }
    }
}
