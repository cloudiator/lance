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

package de.uniulm.omi.cloudiator.lance.lifecycle.bash;

import java.util.ArrayList;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.Shell;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.InstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StartHandler;

public final class BashBasedHandlerBuilder {

    private volatile OperatingSystem os;
    private final List<String[]> commands = new ArrayList<>();
    
    public BashBasedHandlerBuilder() {
        // 
    }

    public PortUpdateHandler buildPortUpdateHandler() {
        return new BashPortUpdateHandler(os, commands);
    }
    
    public LifecycleHandler build(LifecycleHandlerType type) {
         switch(type) {
         case PRE_INSTALL:
             return new BashPreInstallHandler(os, commands);
         case INSTALL:
             return new BashInstallHandler(os, commands);
         case POST_INSTALL:
             return new BashPostInstallHandler(os, commands);
         case START:
             return new BashStartHandler(os, commands);
         case INIT:
             throw new UnsupportedOperationException();
         default:
             throw new UnsupportedOperationException();
         }
    }

    public BashBasedHandlerBuilder setOperatingSystem(OperatingSystem osParam) {
        os = osParam;
        return this;
    }

    public void addCommand(String ... args) {
        commands.add(args);
    }
}

class BashPreInstallHandler implements PreInstallHandler {

    private static final long serialVersionUID = 6252852219310892573L;
    private final OperatingSystem os;
    private final List<String[]> commands;
    
    BashPreInstallHandler(OperatingSystem osParam, List<String[]> commandsParam) {
        os = osParam;
        commands = commandsParam;
    }
    
    @Override
    public void execute(ExecutionContext ec) {
        if(! os.equals(ec.getOperatingSystem())) {
        	return;
        }
        Shell shell = ec.getShell();
        for(String[] cmd : commands) {
            String res = "";
            for(String s : cmd) { 
            	res = res + " " + s;
            }
            shell.executeCommand(res);
        }
    }
}

class BashInstallHandler implements InstallHandler {

    private static final long serialVersionUID = 7579233877994910327L;
    private final OperatingSystem os;
    private final List<String[]> commands;
    
    BashInstallHandler(OperatingSystem osParam, List<String[]> commandsParam) {
        os = osParam;
        commands = commandsParam;
    }
    
    @Override
    public void execute(ExecutionContext ec) {
        if(! os.equals(ec.getOperatingSystem())) {
        	return;
        }
        Shell shell = ec.getShell();
        for(String[] cmd : commands) {
            String res = "";
            for(String s : cmd) { 
            	res = res + " " + s;
            }
            shell.executeCommand(res);
        }
    }
}

class BashPostInstallHandler implements PostInstallHandler {

    private static final long serialVersionUID = -5666019177853948866L;
    private final OperatingSystem os;
    private final List<String[]> commands;
    
    BashPostInstallHandler(OperatingSystem osParam, List<String[]> commandsParam) {
        os = osParam;
        commands = commandsParam;
    }
    
    @Override
    public void execute(ExecutionContext ec) {
        if(! os.equals(ec.getOperatingSystem())) {
        	return;
        }
        Shell shell = ec.getShell();
        for(String[] cmd : commands) {
            String res = "";
            for(String s : cmd) { 
            	res = res + " " + s;
            }
            shell.executeCommand(res);
        }
    }
}

class BashStartHandler implements StartHandler {

    private static final long serialVersionUID = -5666019177853948866L;
    private final OperatingSystem os;
    private final List<String[]> commands;
    
    BashStartHandler(OperatingSystem osParam, List<String[]> commandsParam) {
        os = osParam;
        commands = commandsParam;
    }
    
    @Override
    public void execute(ExecutionContext ec) {
        if(! os.equals(ec.getOperatingSystem())) {
        	return;
        }
        Shell shell = ec.getShell();
        final int commandSize = commands.size();
        int counter = 0;
        
        for(String[] cmd : commands) {
            String res = "";
            counter++;
            for(String s : cmd) { 
            	res = res + " " + s;
            }
            if(counter == commandSize) {
                shell.executeBlockingCommand(res);
            } else { 
            	shell.executeCommand(res); 
            }
        }
    }
}

class BashPortUpdateHandler implements PortUpdateHandler {

    private static final long serialVersionUID = -7036692445701185053L;
    
    private final OperatingSystem os;
    private final List<String[]> commands;
    
    BashPortUpdateHandler(OperatingSystem osParam, List<String[]> commandsParam) {
        os = osParam;
        commands = commandsParam;
    }
    
    @Override
    public void execute(ExecutionContext ec) {
        if(! os.equals(ec.getOperatingSystem())) return;
        Shell shell = ec.getShell();
        final int commandSize = commands.size();
        int counter = 0;
        
        for(String[] cmd : commands) {
            String res = "";
            counter++;
            for(String s : cmd) { 
            	res = res + " " + s;
            }
            if(counter == commandSize) {
                shell.executeBlockingCommand(res);
            } else { 
            	shell.executeCommand(res); 
            }
        }
    }
}
