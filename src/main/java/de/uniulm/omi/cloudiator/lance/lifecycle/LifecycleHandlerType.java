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

package de.uniulm.omi.cloudiator.lance.lifecycle;

import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.InitHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.InstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostStartHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostStopHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreStartHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreStopHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StartHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StopHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.VoidHandler;
import de.uniulm.omi.cloudiator.lance.util.state.State;

/**
 * even though being an Enum, this class class is not serialisable  
 * 
 * @author Joerg Domaschka
 *
 */
public enum LifecycleHandlerType implements State {

    NEW(VoidHandler.class, VoidHandler.FACTORY),
    /**
     * invoked when the Lifecycle controller starts; 
     * may be used for validating system environment;
     */ 
    INIT(InitHandler.class, InitHandler.FACTORY),
    
    /** 
     * may be used to get service binaries, e.g. 
     * by downloading 
     */
    PRE_INSTALL(PreInstallHandler.class, PreInstallHandler.FACTORY),
    /**
     * may be used to unzip and install service binaries
     * 
     */
    INSTALL(InstallHandler.class, InstallHandler.FACTORY),
    /**
     * may be used to adapt configuration files
     * according to environment
     */
    POST_INSTALL(PostInstallHandler.class, PostInstallHandler.FACTORY),
    
    /**
     * may be used for checking that required operating system
     * files are available, like files, disk space, and port 
     */
    PRE_START(PreStartHandler.class, PreStartHandler.FACTORY),
    /**
     * may be used for checking that required operating system
     * files are available, like files, disk space, and port 
     */
    START(StartHandler.class, StartHandler.FACTORY),
    /**
     * may be used to register service instances with a load balancer
     */
    POST_START(PostStartHandler.class, PostStartHandler.FACTORY),
    
    /**
     * may be used to unregister service instance at the load balancer
     */
    PRE_STOP(PreStopHandler.class, PreStopHandler.FACTORY),
    /**
     * may be used to add manual stop logic
     */
    STOP(StopHandler.class, StopHandler.FACTORY),
    /**
     * may be used to release external resources
     */
    POST_STOP(PostStopHandler.class, PostStopHandler.FACTORY),
    ;
    
    private final Class<? extends LifecycleHandler> handlerType;
    private final LifecycleHandlerFactory<? extends LifecycleHandler> factory;
    
    LifecycleHandlerType(Class<? extends LifecycleHandler> handlerTypeParam,
                            LifecycleHandlerFactory<? extends LifecycleHandler> factoryParam) {
        handlerType = handlerTypeParam;
        factory = factoryParam;
    }

    public Class<? extends LifecycleHandler> getTypeClass() {
        return handlerType;
    }
    
    public LifecycleHandlerFactory<? extends LifecycleHandler> getFactory() {
        return factory;
    }
    
    public <T extends LifecycleHandler> T getDefaultImplementation() {
        LifecycleHandler h = factory.getDefault();
        @SuppressWarnings("unchecked")
        T t = (T) h;
        return t;
    }
}
