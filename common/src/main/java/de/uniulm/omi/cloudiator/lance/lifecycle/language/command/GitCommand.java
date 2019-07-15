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

import java.net.URI;
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

public interface GitCommand extends Command {
    
    enum GitSubcommand {
        CLONE,
        ;
    }

    public static class GitCommandFactory {
            
        private final static Set<LifecycleHandlerType> supportedLifecycles;
        
        static {
            supportedLifecycles = EnumSet.of(LifecycleHandlerType.INSTALL);
        }
        
        public static GitCommand create(LifecycleHandlerType inPhase, GitSubcommand command, URI uri) {
            if(supportedLifecycles.contains(inPhase)) {
                return new GitCommandImpl(inPhase, command, uri);
            }
            throw new IllegalStateException("SystemServiceCommand cannot be executed at Lifecylce Phase " + inPhase);
        }
        
        private GitCommandFactory() {
            // no instances so far //
        }
    }
}

final class GitCommandResult implements CommandResultReference {

    @Override
    public String getResult(OperatingSystem os, ExecutionContext ec) {
        throw new UnsupportedOperationException();
    }
}

final class GitCommandImpl implements GitCommand {

    private final LifecycleHandlerType type;
    // private final GitSubcommand command;
    // private final URI uri;
    private final CommandResultReference output = new GitCommandResult();
    
    
    public GitCommandImpl(LifecycleHandlerType inPhase, @SuppressWarnings("unused") GitSubcommand commandParam, @SuppressWarnings("unused") URI uriParam) {
        // command = _command;
        // uri = _uri;
        type = inPhase;
    }

    @Override
    public CommandResultReference getResult() {
        return output;
    }

    @Override
    public boolean runsInLifecycle(LifecycleHandlerType typeParam) {
        return type == typeParam;
    }

    @Override
    public void execute(ExecutionContext ec) {
        throw new UnsupportedOperationException();
    }
}
