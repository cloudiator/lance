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

public interface ReplaceFileContentCommand extends Command {

    public static class ReplaceFileContentCommandFactory {
    
        private final static Set<LifecycleHandlerType> supportedLifecycles;
        
        static {
            supportedLifecycles = EnumSet.of(LifecycleHandlerType.INSTALL,
                                                LifecycleHandlerType.POST_INSTALL,
                                                LifecycleHandlerType.POST_START,
                                                LifecycleHandlerType.PRE_START);
        }
        
        public static ReplaceFileContentCommand create(LifecycleHandlerType inPhase, CommandResultReference ref, 
                                                            String pattern, String replacement) {
            
            if(supportedLifecycles.contains(inPhase)) {
                return new ReplaceFileContentImpl(inPhase, ref, pattern, replacement);
            }
            throw new IllegalStateException("ReplaceFileContentCommand cannot be executed at Lifecylce Phase " + inPhase);
        }
        
        private ReplaceFileContentCommandFactory() {
            // no instances (so far)
        }
    }
}

class ReplaceFileContentImpl implements ReplaceFileContentCommand {

    // so far, we only support commands as root //
    private final static boolean useRoot = true;
    private final LifecycleHandlerType type;
    private final CommandResultReference fileref;
    private final String pattern;
    private final String replacement;
    private final CommandResultReference result = new DefaultCommandResultReference();

    ReplaceFileContentImpl(LifecycleHandlerType typeParam, CommandResultReference refParam, String patternParam, String replacementParam) {
        fileref = refParam;
        pattern  = patternParam;
        replacement = replacementParam;
        type = typeParam;
    }
    
    @Override
    public CommandResultReference getResult() {
        return result;
    }

    @Override
    public boolean runsInLifecycle(LifecycleHandlerType handlerType) {
        return type == handlerType;
    }

    @Override
    public void execute(ExecutionContext ec) {
        OperatingSystem os = ec.getOperatingSystem();
        Shell shell = ec.getShell();
        if(os.isLinuxOs()) {
            String command = buildLinuxCommand(os, ec);
            shell.executeCommand(command);
        } else {
            throw new IllegalStateException("command line execution not supported for Mac or Windows systems.");
        }
    }
    
    private String buildLinuxCommand(OperatingSystem os, ExecutionContext ec) {
        String filename = fileref.getResult(os, ec);
        if(filename == null) throw new NullPointerException("no result available");
        if(! filename.startsWith("/")) filename = "./" + filename;
                
        String command = "sed -i -e 's!" + pattern + "!" + replacement + "!g' " + filename;
        if(os.getType() == OperatingSystemType.UBUNTU) {
            if(useRoot) return "sudo " + command;
            return command;
        }
        return command;
    }
}
