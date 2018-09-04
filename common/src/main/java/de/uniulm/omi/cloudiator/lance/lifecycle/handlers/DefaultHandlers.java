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

package de.uniulm.omi.cloudiator.lance.lifecycle.handlers;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StartDetector;

public final class DefaultHandlers implements LifecycleHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHandlers.class);
    private static final long serialVersionUID = -9123613719962377068L;

    static Logger getLogger() { 
    	return LOGGER; 
    }

    public static final InitHandler DEFAULT_INIT_HANDLER = new InitHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            // throw new UnsupportedOperationException();
            getLogger().info("DEFAULT InitHandler doing nothing");
        }
    }; 
    
    
    public static final InstallHandler DEFAULT_INSTALL_HANDLER = new InstallHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT InstallHandler doing nothing");
        }
    };
    
    public static final PostInstallHandler DEFAULT_POST_INSTALL_HANDLER = new PostInstallHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PostInstallHandler doing nothing");
        }
    };
    

    public static final PostStartHandler DEFAULT_POST_START_HANDLER = new PostStartHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PostStartHandler doing nothing");
        }        
    };
    
    public static final PostStopHandler DEFAULT_POST_STOP_HANDLER = new PostStopHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PostStopHandler doing nothing");
        }
    };
    
    public static final PreInstallHandler DEFAULT_PRE_INSTALL_HANDLER = new PreInstallHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PreInstallHandler doing nothing");
        }
    };
    
    public static final PreStartHandler DEFAULT_PRE_START_HANDLER = new PreStartHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PreStartHandler doing nothing");
        }
    };    

    public static final PreStopHandler DEFAULT_PRE_STOP_HANDLER = new PreStopHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PreStopHandler doing nothing");
        }
    };
    
    public static final StartHandler DEFAULT_START_HANDLER = new StartHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT StartHandler doing nothing");
        }
    };
    
    public static final StopHandler DEFAULT_STOP_HANDLER = new StopHandler() {

        private static final long serialVersionUID = 1L;

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT StopHandler doing nothing");
        }
    };
    
    public static final StartDetector DEFAULT_START_DETECTOR = new StartDetector() {
    	
		@Override
		public DetectorState execute(ExecutionContext ec) {
			getLogger().info("DEFAULT StopHandler doing nothing");
			return DetectorState.DETECTED;
		}
	};
    
    public static final VoidHandler DEFAULT_VOID_HANDLER = new VoidHandler(); 
    
    private DefaultHandlers() {
        // no instances //
    }

    @Override
    public void execute(ExecutionContext ec) throws LifecycleException {
        throw new LifecycleException("Execution not possible in default handler");
    }
}
