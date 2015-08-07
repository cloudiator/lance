package de.uniulm.omi.cloudiator.lance.lifecycle.handlers;

import de.uniulm.omi.cloudiator.lance.deployment.Deployment;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerFactory;

public final class DefaultFactories {

    public static final LifecycleHandlerFactory<StopHandler> STOP_FACTORY = new LifecycleHandlerFactory<StopHandler>() {
        @Override public final StopHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_STOP_HANDLER; 
        }

        @Override
        public StopHandler getDeploymentHandler(Deployment d) {
            return new StopDeploymentHandler(d);
        } 
    };
    
    public static final LifecycleHandlerFactory<PostStartHandler> POST_START_FACTORY = new LifecycleHandlerFactory<PostStartHandler>() {
        @Override public final PostStartHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_POST_START_HANDLER; 
        }

        @Override
        public PostStartHandler getDeploymentHandler(Deployment d) {
            return new PostStartDeploymentHandler(d);
        } 
    };
    
    public static final LifecycleHandlerFactory<PreStopHandler> PRE_STOP_FACTORY = new LifecycleHandlerFactory<PreStopHandler>() {
        @Override public final PreStopHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_PRE_STOP_HANDLER; 
        }

        @Override
        public PreStopHandler getDeploymentHandler(Deployment d) {
            return new PreStopDeploymentHandler(d);
        } 
    };
    
    public static final LifecycleHandlerFactory<VoidHandler> VOID_FACTORY = new LifecycleHandlerFactory<VoidHandler>() {
        @Override public final VoidHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_VOID_HANDLER; 
        }

        @Override
        public VoidHandler getDeploymentHandler(Deployment d) {
            throw new UnsupportedOperationException("void handler cannot be used for own handler logic");
        } 
    };
    
    public static final LifecycleHandlerFactory<StartHandler> START_FACTORY = new LifecycleHandlerFactory<StartHandler>() {
        @Override public final StartHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_START_HANDLER; 
        }
        
        @Override
        public StartHandler getDeploymentHandler(Deployment d) {
            return new StartDeploymentHandler(d);
        } 
    };
    
    public static final LifecycleHandlerFactory<PostStopHandler> POST_STOP_FACTORY = new LifecycleHandlerFactory<PostStopHandler>() {
        @Override public final PostStopHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_POST_STOP_HANDLER; 
        }

        @Override
        public PostStopHandler getDeploymentHandler(Deployment d) {
            return new PostStopDeploymentHandler(d);
        } 
    };
    
    public static final LifecycleHandlerFactory<PreStartHandler> PRE_START_FACTORY = new LifecycleHandlerFactory<PreStartHandler>() {
        @Override public final PreStartHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_PRE_START_HANDLER; 
        }

        @Override
        public PreStartHandler getDeploymentHandler(Deployment d) {
            return new PreStartDeploymentHandler(d);
        } 
    };
    
    public static final LifecycleHandlerFactory<InstallHandler> INSTALL_FACTORY = new LifecycleHandlerFactory<InstallHandler>() {
        @Override public final InstallHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_INSTALL_HANDLER; 
        } 

        @Override
        public InstallHandler getDeploymentHandler(Deployment d) {
            return new InstallDeploymentHandler(d);
        } 
    };
    
    public static final LifecycleHandlerFactory<PostInstallHandler> POST_INSTALL_FACTORY = new LifecycleHandlerFactory<PostInstallHandler>() {
        @Override public final PostInstallHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_POST_INSTALL_HANDLER; 
        }

        @Override
        public PostInstallHandler getDeploymentHandler(Deployment d) {
            return new PostInstallDeploymentHandler(d);
        } 
    };    

    public static final LifecycleHandlerFactory<PreInstallHandler> PRE_INSTALL_FACTORY = new LifecycleHandlerFactory<PreInstallHandler>() {
        @Override public final PreInstallHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_PRE_INSTALL_HANDLER; 
        }

        @Override
        public PreInstallHandler getDeploymentHandler(Deployment d) {
            return new PreInstallDeploymentHandler(d);
        } 
    };
    
    public static final LifecycleHandlerFactory<InitHandler> INIT_FACTORY = new LifecycleHandlerFactory<InitHandler>() {
        @Override public final InitHandler getDefault() { 
        	return DefaultHandlers.DEFAULT_INIT_HANDLER; 
        }

        @Override
        public InitHandler getDeploymentHandler(Deployment d) {
            return new InitDeploymentHandler(d);
        } 
    };
    
    private DefaultFactories() {
        // no instances of this class //
    }
}
