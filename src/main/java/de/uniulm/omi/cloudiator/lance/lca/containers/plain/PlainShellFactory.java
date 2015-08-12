package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lifecycle.ShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Daniel Seybold on 12.08.2015.
 */
public class PlainShellFactory implements ShellFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainShell.class);

    private final AtomicReference<PlainShellWrapper> reference = new AtomicReference<>();

    @Override
    public PlainShellWrapper createShell() {
        PlainShellWrapper wrapper = this.reference.get();
        if(wrapper == null)
            throw new IllegalStateException("shell not set");
        return wrapper;
    }

    void installDockerShell(PlainShell plainShell) {
        final PlainShellWrapper wrapper = new PlainShellWrapper(plainShell);
        PlainShellWrapper old = reference.getAndSet(wrapper);
        if(old != null) {
            LOGGER.error("ERROR: overriding plain shell with new one. this should never happen.");
        }
    }

    void closeShell() {
        PlainShellWrapper old = reference.getAndSet(null);
        if(old == null) {
            LOGGER.error("ERROR: no plain shell set that can be closed.");
        } else {
            old.plainShell.close();
        }
    }
}
