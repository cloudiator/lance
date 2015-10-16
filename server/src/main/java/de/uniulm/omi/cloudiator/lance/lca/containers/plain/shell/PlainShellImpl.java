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

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Daniel Seybold on 11.08.2015.
 */
public class PlainShellImpl implements PlainShell {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainShell.class);

    private  ProcessBuilder processBuilder = new ProcessBuilder();
    //private ProcessBuilder processBuilder = new ProcessBuilder().inheritIO();

    private final List<String> osShell = new ArrayList<String>();

    public PlainShellImpl(OperatingSystem operatingSystem){

        //fixme: do this in a more generic way
        //add the os respective shells for execution
        if(operatingSystem.equals(OperatingSystem.WINDOWS_7)){

            this.osShell.add("powershell.exe");
            this.osShell.add("-command");

        }else if(operatingSystem.equals(OperatingSystem.UBUNTU_14_04)){

            this.osShell.add("/bin/bash");
            this.osShell.add("-c");

        }else{
            throw new IllegalStateException("Unkown OS: " + operatingSystem.toString());
        }
    }


    @Override
    public ExecutionResult executeCommand(String command) {
        ExecutionResult executionResult = null;
        Process shellProcess;

        List<String>commands = this.buildCommand(command);

        try {

            shellProcess = this.processBuilder.command(commands).start();

            //just for debugging
            List<String> debuglist = this.processBuilder.command();
            debuglist.stream().forEach((string) -> {
                LOGGER.debug("Content: " + string);
                //System.out.println("Content: " + string);
            });

            String commandOut = this.extractCommandOutput(shellProcess);
            //System.out.println(commandOut); //debugging
            LOGGER.debug(commandOut);

            String errorOut = this.extractErrorOutput(shellProcess);
            //System.out.println(errorOut); //debugging
            LOGGER.debug(errorOut);


            //important, wait for or it will run in an deadlock!!, adapt execution result maybe
            int exitValue =shellProcess.waitFor();
            LOGGER.debug("ExitCode: " + exitValue);

            executionResult = this.createExecutionResult(exitValue, commandOut, errorOut);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("Error while executing command: " + command,e);
            executionResult = ExecutionResult.systemFailure(e.getLocalizedMessage());
            return executionResult;
        }


        return executionResult;
    }

    private List<String> buildCommand(String commandLine){
        
        List<String> commandList = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(commandLine);
        while (m.find())
            commandList.add(m.group(1)); // Add .replace("\"", "") to remove surrounding quotes.


        //create list with os specific commands
        List<String> result = new ArrayList<String>(this.osShell);

        //add app commands and wrap them in double quotes, single quotes won't work on Windows
        result.add("\"");
        result.addAll(commandList);
        result.add("\"");

        return result;

    }

    @Override
    public ExecutionResult executeBlockingCommand(String command) {
        //fixme: implement this in a blocking way, check if blocking command are necessary
        LOGGER.warn("Using currently same impl for blocking/nonblocking execution of commands!");
        return this.executeCommand(command);
    }


    @Override
    public void close() {
        //fixme: implement this, check what needs to be closed or process killed?
        LOGGER.warn("Closing PlainShellImpl needs to be implemented!");
    }

    @Override
    public void setDirectory(String directory) {
        this.processBuilder.directory(new File(directory));

        LOGGER.info(this.processBuilder.directory().getAbsoluteFile().toString());
    }

    @Override
    public void setEnvVar(String key, String value) {

    }


    private ExecutionResult createExecutionResult(int exitValue, String commandOut, String errorOut){

        ExecutionResult executionResult;

        if(exitValue == 0){
            executionResult = ExecutionResult.success(commandOut,errorOut);
        }else{
            executionResult = ExecutionResult.commandFailure(exitValue, commandOut,errorOut);
        }

        return executionResult;

    }

    private static String extractCommandOutput(Process process){
        String output;

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        try {
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            //closing
            reader.close();
        } catch (IOException e) {
            LOGGER.error("Error while reading process outputstream", e);
            e.printStackTrace();
        }

        output = builder.toString();




        return output;
    }

    private static String extractErrorOutput(Process process){
        String output;

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        try {
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            //closing
            reader.close();
        } catch (IOException e) {
            LOGGER.error("Error while reading process errorstream", e);
            e.printStackTrace();
        }

        output = builder.toString();

        return output;
    }



}
