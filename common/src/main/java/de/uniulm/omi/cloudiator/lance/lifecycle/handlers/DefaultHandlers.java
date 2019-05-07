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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StartDetector;

public final class DefaultHandlers implements LifecycleHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHandlers.class);
    private static final long serialVersionUID = -5958986200429759370L;

    static Logger getLogger() {
    	return LOGGER; 
    }

    public static final InitHandler DEFAULT_INIT_HANDLER = new InitHandler() {

        @Override public void execute(ExecutionContext ec) {
            // throw new UnsupportedOperationException();
            getLogger().info("DEFAULT InitHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    }; 
    
    
    public static final InstallHandler DEFAULT_INSTALL_HANDLER = new InstallHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT InstallHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };
    
    public static final PostInstallHandler DEFAULT_POST_INSTALL_HANDLER = new PostInstallHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PostInstallHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };
    

    public static final PostStartHandler DEFAULT_POST_START_HANDLER = new PostStartHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PostStartHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };
    
    public static final PostStopHandler DEFAULT_POST_STOP_HANDLER = new PostStopHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PostStopHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };
    
    public static final PreInstallHandler DEFAULT_PRE_INSTALL_HANDLER = new PreInstallHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PreInstallHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };
    
    public static final PreStartHandler DEFAULT_PRE_START_HANDLER = new PreStartHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PreStartHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };    

    public static final PreStopHandler DEFAULT_PRE_STOP_HANDLER = new PreStopHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT PreStopHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };
    
    public static final StartHandler DEFAULT_START_HANDLER = new StartHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT StartHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };
    
    public static final StopHandler DEFAULT_STOP_HANDLER = new StopHandler() {

        @Override public void execute(ExecutionContext ec) {
            getLogger().info("DEFAULT StopHandler doing nothing");
        }

      @Override
      public boolean isEmpty() {
        return true;
      }
    };
    
    public static final StartDetector DEFAULT_START_DETECTOR = new StartDetector() {

      @Override
      public boolean isEmpty() {
        return true;
      }

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

  @Override
  public boolean isEmpty() {
    return true;
  }
}
