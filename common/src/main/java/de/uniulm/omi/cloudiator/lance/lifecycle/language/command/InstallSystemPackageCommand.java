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

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.Command;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandResultReference;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.install.SystemApplication;

public interface InstallSystemPackageCommand extends Command {

    public static class InstallSystemPackageCommandFactory {
        
        private final static Set<LifecycleHandlerType> supportedLifecycles;
        
        static {
            supportedLifecycles = EnumSet.of(LifecycleHandlerType.INSTALL);
        }
        
        public static InstallSystemPackageCommand create(LifecycleHandlerType inPhase, SystemApplication appParam) {
            if(supportedLifecycles.contains(inPhase)) {
                return new InstallSystemPackageCommandImpl(inPhase, appParam);
            }
            throw new IllegalStateException("SystemServiceCommand cannot be executed at Lifecylce Phase " + inPhase);
        }
        
        private InstallSystemPackageCommandFactory() {
            // no instances so far //
        }
    }
}

class InstallSystemPackageCommandImpl implements InstallSystemPackageCommand {

    private final LifecycleHandlerType type;
    // private final String packageName;
    private final SystemApplication application;
    private final CommandResultReference result = new DefaultCommandResultReference();

    InstallSystemPackageCommandImpl(LifecycleHandlerType typeParam, SystemApplication appParam) {
        application = appParam;
        type = typeParam;
    }
    
    @Override
    public CommandResultReference getResult() {
        return result;
    }

    @Override
    public boolean runsInLifecycle(LifecycleHandlerType typeParam) {
        return type == typeParam;
    }

    @Override
    public void execute(ExecutionContext ec) {
        OperatingSystem os = ec.getOperatingSystem();
        String command = application.getPackageName(os);
        System.err.println("Command getNonBlockingPackageInstallerCommand() not implemented"
            + "in OperatingSystemClass");
        
        // ec.getShell().executeCommand(command);
    }
}
