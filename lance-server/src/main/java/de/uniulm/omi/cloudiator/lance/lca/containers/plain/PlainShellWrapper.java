package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import de.uniulm.omi.cloudiator.lance.lifecycle.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Daniel Seybold on 11.08.2015.
 */
public class PlainShellWrapper implements Shell {

    private static final Logger LOGGER = LoggerFactory.getLogger(Shell.class);

    final PlainShell plainShell;

    PlainShellWrapper(PlainShell plainShell){
        this.plainShell = plainShell;
    }

    @Override
    public ExecutionResult executeCommand(String command) {
        ExecutionResult result = this.plainShell.executeCommand(command);
        checkResult(command, result);
        return result;
    }

    @Override
    public ExecutionResult executeBlockingCommand(String command){
        ExecutionResult result = this.plainShell.executeBlockingCommand(command);
        checkResult(command, result);
        return result;
    }

    private static void checkResult(String command, ExecutionResult result) {
        if(result.isSuccess()) {
            return;
        }
        LOGGER.warn("unsuccessull command '" + command + "': " + result.toString());
    }
}
