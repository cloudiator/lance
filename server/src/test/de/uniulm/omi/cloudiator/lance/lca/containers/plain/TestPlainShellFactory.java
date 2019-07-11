package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShellImpl;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;

final class TestPlainShellFactory implements PlainShellFactory {

	private final Stack<TestPlainShell> shells = new Stack<>();
    private final AtomicReference<PlainShellWrapper> reference = new AtomicReference<>();

	@Override
	public PlainShellWrapper createShell() {
		PlainShellWrapper wrapper = reference.get();
		if(wrapper == null)
			throw new IllegalStateException("plain shell not set");
		return wrapper;
	}

	@Override
	public void closeShell() {
        PlainShellWrapper old = reference.getAndSet(null);
        if(old == null) {
            throw new IllegalStateException("shell was not be set");
        } else {
            old.plainShell.close();
        }
	}

	@Override
	public PlainShell createAndinstallPlainShell(OperatingSystem os) {
		TestPlainShell plainShell = new TestPlainShell(os);
		shells.push(plainShell);
        final PlainShellWrapper wrapper = new PlainShellWrapper(plainShell);
        PlainShellWrapper old = reference.getAndSet(wrapper);
        if(old != null) {
        	throw new IllegalStateException("has never been set");
        }
        return plainShell;
	}

	static class TestPlainShell implements PlainShell {

		private final OperatingSystem os;
		
		public TestPlainShell(OperatingSystem osP) {
			os = osP;
		}

		@Override
		public ExecutionResult executeCommand(String string) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setEnvironmentVariable(String key, String value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public ExecutionResult executeBlockingCommand(String command) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void close() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setDirectory(String directory) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
