package de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                System.out.println("Content: " + string);
            });

            String commandOut = this.extractCommandOutput(shellProcess);
            System.out.println(commandOut); //debugging

            String errorOut = this.extractErrorOutput(shellProcess);
            System.out.println(errorOut); //debugging


            //important, wait for or it will run in an deadlock!!, adapt execution result maybe
            int exitValue =shellProcess.waitFor();
            System.out.println("ExitCode: " + exitValue);

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

        String[] splittedCommands = this.splitCommandLines(commandLine);

        List<String> commandList = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(commandLine);
        while (m.find())
            commandList.add(m.group(1)); // Add .replace("\"", "") to remove surrounding quotes.


        //create list with os specific commands
        List<String> result = new ArrayList<String>(this.osShell);

        //add app commands
        result.addAll(commandList);

        return result;

    }

    private String[] splitCommandLines(String commandLine){
        String[] commands = commandLine.split(" ");

        return commands;
    }

    @Override
    public ExecutionResult executeBlockingCommand(String command) {
        //fixme: implement this in a blocking way, check if blocking command are necessary
        return null;
    }


    @Override
    public void close() {
        //fixme: implement this, check what needs to be closed or process killed?
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
