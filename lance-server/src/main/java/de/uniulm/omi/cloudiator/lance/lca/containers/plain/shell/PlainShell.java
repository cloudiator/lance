package de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell;

import de.uniulm.omi.cloudiator.lance.lca.container.environment.ShellLikeInterface;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;

import java.io.File;

/**
 * Created by Daniel Seybold on 11.08.2015.
 */
public interface PlainShell extends ShellLikeInterface {

    public ExecutionResult executeCommand(String command);

    public ExecutionResult executeBlockingCommand(String command);

    public void close();

    public void setDirectory(String directory);

    public void setEnvVar(String key, String value);
}
