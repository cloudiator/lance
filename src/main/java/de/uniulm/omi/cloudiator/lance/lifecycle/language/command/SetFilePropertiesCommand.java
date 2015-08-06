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

public interface SetFilePropertiesCommand extends Command {

    static final class SetFilePropertiesCommandConstants {
        public static final int READ_ACCESS  = 1;
        public static final int WRITE_ACCESS = 2;
        public static final int EXECUTE_ACCESS = 4;
        public static final int ALL_ACCESS = 7;
        
        public static final int FILE_OWNER = 1;
        public static final int FILE_GROUP = 2;
        public static final int FILE_WORLD = 4;
        public static final int FILE_ALL = 7;
        
        private SetFilePropertiesCommandConstants(){
            // no instances allowed so far //
        }
    }

    public static class SetFilePropertiesCommandFactory {
    
        private final static Set<LifecycleHandlerType> supportedLifecycles;
        
        static {
            supportedLifecycles = EnumSet.of(LifecycleHandlerType.INSTALL);
        }
        
        public static SetFilePropertiesCommand setAccessRights(LifecycleHandlerType inPhase, int access, int users, CommandResultReference reference) {
            
            if(supportedLifecycles.contains(inPhase)) {
                if(access > SetFilePropertiesCommandConstants.ALL_ACCESS || access < 0) throw new IllegalArgumentException("" + access);
                if(users > SetFilePropertiesCommandConstants.FILE_ALL || users < 0) throw new IllegalArgumentException("" + access);
                return new SetFilePropertiesCommandImpl (inPhase, access, users, reference);
            }
            throw new IllegalStateException("SystemServiceCommand cannot be executed at Lifecylce Phase " + inPhase);
        }
        
        private SetFilePropertiesCommandFactory() {
            // no instances so far //
        }
    }
}

final class SetFilePropertiesCommandImpl implements SetFilePropertiesCommand {
    
    private final LifecycleHandlerType type;
    protected static final boolean USE_ROOT = true;
    private final CommandResultReference result = new DefaultCommandResultReference();
    private final CommandResultReference input;
    private final int[] props;

    SetFilePropertiesCommandImpl(LifecycleHandlerType inPhase, int access, int users, CommandResultReference reference) {
        input = reference;
        type = inPhase;
        props = new int[]{access, users};
    }

    private final static String getAccessString(int access) {
        return Integer.toString(access);
    }
    
    private static String getUserString(int users, String string) {
        String retVal = "000";
        switch(users) {
        case 7: retVal = string + string + string; break;
        case 6: retVal = "0" + string + string; break;
        case 5: retVal = string + "0" + string; break;
        case 4: retVal = "00" + string; break;
        case 3: retVal = string + string + "0"; break;
        case 2: retVal = "0" + string + "0"; break;
        case 1: retVal = string + "00"; break;
        default:
        }
        return retVal;
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
            String command = buildLinuxCommand(os);
            shell.executeCommand(command);
        } else {
            throw new IllegalStateException("command line execution not supported for Mac or Windows systems.");
        }
    }
    
    protected String buildLinuxCommand(OperatingSystem os) {
        String filename = input.getResult(os, null);
        if(filename == null) throw new NullPointerException("no result available");
        if(! filename.startsWith("/")) filename = "./" + filename;
        String string = getUserString(props[1], getAccessString(props[0]));
        
        String command = "chmod " + string + " " + filename;
        if(os.getType() == OperatingSystemType.UBUNTU) {
            if(USE_ROOT) return "sudo " + command;
            return command;
        }
        return command;
    }

}
