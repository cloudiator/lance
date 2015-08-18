package de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Daniel Seybold on 11.08.2015.
 */
public class PlainShellImpl implements PlainShell {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainShell.class);


    @Override
    public ExecutionResult executeCommand(String command) {
        ExecutionResult executionResult = null;
        Process shellProcess;
        try {
            shellProcess = new ProcessBuilder(command).inheritIO().start();
        } catch (IOException e) {
            LOGGER.error("Error while executing command: " + command,e);
            executionResult = ExecutionResult.systemFailure(e.getLocalizedMessage());
            return executionResult;
        }

        executionResult = this.createExecutionResult(shellProcess);

        return executionResult;
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

    private ExecutionResult createExecutionResult(Process process){

        ExecutionResult executionResult;

        int exitValue = process.exitValue();

        switch (exitValue){
            case 0:
                executionResult = ExecutionResult.success(extractCommandOutput(process),extractErrorOutput(process));
            default:
                executionResult = ExecutionResult.commandFailure(exitValue, extractCommandOutput(process),extractErrorOutput(process));

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
        } catch (IOException e) {
            LOGGER.error("Error while reading process errorstream", e);
            e.printStackTrace();
        }

        output = builder.toString();

        return output;
    }



}
