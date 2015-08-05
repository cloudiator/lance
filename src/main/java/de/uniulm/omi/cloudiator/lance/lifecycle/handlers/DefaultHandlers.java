package de.uniulm.omi.cloudiator.lance.lifecycle.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;

public final class DefaultHandlers {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultHandlers.class);

	public static final InitHandler DEFAULT_INIT_HANDLER = new InitHandler() {

		@Override public void execute(ExecutionContext ec) {
			// throw new UnsupportedOperationException();
			logger.info("DEFAULT InitHandler doing nothing");
		}
	}; 
	
	
	public static final InstallHandler DEFAULT_INSTALL_HANDLER = new InstallHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT InstallHandler doing nothing");
		}
	};
	
	public static final PostInstallHandler DEFAULT_POST_INSTALL_HANDLER = new PostInstallHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT PostInstallHandler doing nothing");
		}
	};
	

	public static final PostStartHandler DEFAULT_POST_START_HANDLER = new PostStartHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT PostStartHandler doing nothing");
		}		
	};
	
	public static final PostStopHandler DEFAULT_POST_STOP_HANDLER = new PostStopHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT PostStopHandler doing nothing");
		}
	};
	
	public static final PreInstallHandler DEFAULT_PRE_INSTALL_HANDLER = new PreInstallHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT PreInstallHandler doing nothing");
		}
	};
	
	public static final PreStartHandler DEFAULT_PRE_START_HANDLER = new PreStartHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT PreStartHandler doing nothing");
		}
	};	

	public static final PreStopHandler DEFAULT_PRE_STOP_HANDLER = new PreStopHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT PreStopHandler doing nothing");
		}
	};
	
	public static final StartHandler DEFAULT_START_HANDLER = new StartHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT StartHandler doing nothing");
		}
	};
	
	public static final StopHandler DEFAULT_STOP_HANDLER = new StopHandler() {
		@Override public void execute(ExecutionContext ec) {
			logger.info("DEFAULT StopHandler doing nothing");
		}
	};
	
	public static final VoidHandler DEFAULT_VOID_HANDLER = new VoidHandler(); 
	
	private DefaultHandlers() {
		// no instances //
	}
}
