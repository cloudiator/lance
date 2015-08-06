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

package de.uniulm.omi.cloudiator.lance.lifecycle.language.command;

import java.util.EnumSet;
import java.util.Set;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystemType;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.Shell;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.Command;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandResultReference;

public interface ExecuteOnShellCommand extends Command {

    public static class ExecuteOnShellCommandFactory {
            
        private final static Set<LifecycleHandlerType> supportedLifecycles;
        
        static {
            supportedLifecycles = EnumSet.of(LifecycleHandlerType.INIT,
                                                LifecycleHandlerType.INSTALL,
                                                LifecycleHandlerType.POST_INSTALL,
                                                LifecycleHandlerType.POST_START,
                                                LifecycleHandlerType.POST_STOP,
                                                LifecycleHandlerType.PRE_INSTALL,
                                                LifecycleHandlerType.PRE_START,
                                                LifecycleHandlerType.PRE_STOP,
                                                LifecycleHandlerType.START,
                                                LifecycleHandlerType.STOP);
        }

        public static ExecuteOnShellCommand createRootCommand(LifecycleHandlerType inPhase, CommandResultReference f) {
            if(supportedLifecycles.contains(inPhase)) {
                return new DeferredExecuteOnShellCommand(inPhase, f, true);
            }
            throw new IllegalStateException("SystemServiceCommand cannot be executed at Lifecylce Phase " + inPhase);
        }
        
        public static ExecuteOnShellCommand createUserCommand(LifecycleHandlerType inPhase, CommandResultReference f) {
            if(supportedLifecycles.contains(inPhase)) {
                return new DeferredExecuteOnShellCommand(inPhase, f, false);
            }
            throw new IllegalStateException("SystemServiceCommand cannot be executed at Lifecylce Phase " + inPhase);
        }
        
        private ExecuteOnShellCommandFactory() {
            // no instances so far //
        }
    }
}

final class OnShellCommandResult implements CommandResultReference {

    @Override
    public String getResult(OperatingSystem os, ExecutionContext ec) {
        throw new UnsupportedOperationException();
    }
}

abstract class AbstractExecuteOnShellCommand implements ExecuteOnShellCommand {
    
    protected final boolean useRoot;
    private final CommandResultReference output = new OnShellCommandResult();
    
    AbstractExecuteOnShellCommand(boolean _useRoot) {
        useRoot = _useRoot;
    }
    
    @Override
    public final void execute(ExecutionContext ec) {
        OperatingSystem os = ec.getOperatingSystem();
        Shell shell = ec.getShell();
        if(os.isLinuxOs()) {
            String command = buildLinuxCommand(os);
            shell.executeCommand(command);
        } else {
            throw new IllegalStateException("command line execution not supported for Mac or Windows systems.");
        }
    }

    @Override
    public final CommandResultReference getResult() {
        return output;
    }

    protected abstract String buildLinuxCommand(OperatingSystem os);
}

final class DeferredExecuteOnShellCommand extends AbstractExecuteOnShellCommand {

    private final LifecycleHandlerType type;
    private final CommandResultReference input;
    
    DeferredExecuteOnShellCommand(LifecycleHandlerType _type, CommandResultReference file, boolean _useRoot) {
        super(_useRoot);
        input = file;
        type = _type;
    }

    @Override
    public boolean runsInLifecycle(LifecycleHandlerType handlerType) {
        return type == handlerType;
    }
    
    protected String buildLinuxCommand(OperatingSystem os) {
        String filename = input.getResult(null, null);
        if(filename == null) throw new NullPointerException("no result available");
        if(! filename.startsWith("/")) filename = "./" + filename;
        if(os.getType() == OperatingSystemType.UBUNTU) {
            if(useRoot) return "sudo " + filename;
            return filename;
        }
        return filename;
    }
}
