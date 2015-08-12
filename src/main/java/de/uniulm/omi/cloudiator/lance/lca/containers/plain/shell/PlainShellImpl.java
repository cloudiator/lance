package de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by Daniel Seybold on 11.08.2015.
 */
public class PlainShellImpl implements PlainShell {
    //fixme: check if funtionality of InProgress fits better or use the Java ProcessBuilder
    private Process shellProcess;
    private static final Logger LOGGER = LoggerFactory.getLogger(PlainShell.class);


    @Override
    public ExecutionResult executeCommand(String command) {
        ExecutionResult executionResult = null;
        try {
            this.shellProcess = new ProcessBuilder(command).start();
        } catch (IOException e) {
            LOGGER.error("Error while executing command: " + command,e);
            //fixme: create failed executionResult
        }

        //fixme: create successfull execution result

        return executionResult;
    }

    @Override
    public ExecutionResult executeBlockingCommand(String command) {
        //fixme: implement this in a blocking way
        return null;
    }


    @Override
    public void close() {
        //fixme: implement this, check what needs to be closed or process killed?
    }

    /*
    private ExecutionResult checkProcess(){
        int exitValue = this.shellProcess.exitValue();
        ExecutionResult executionResult;
        switch (exitValue){
            case 0: ExecutionResult.success(this.shellProcess.getOutputStream()., this.shellProcess.getErrorStream());
            case -1:;
            default:
        }
        return executionResult;
    }
    */
}
