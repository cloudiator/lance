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

package de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerShell;

/** instances of this class are shared among DockerContainerLogics. Therefore, 
 * this class has to be stateless.
 *   
 * @author Joerg Domaschka
 */
final class ProcessBasedConnector implements DockerConnector {

    private static final Logger LOGGER = Logger.getLogger(ProcessBasedConnector.class.getName());
    // never access this field directly except through the connect method
    // private ProcessWrapper dockerClient;
    
    ProcessBasedConnector(@SuppressWarnings("unused") String hostname) {
        // no values to set //
    }
    
    private static String buildContainerName(ComponentInstanceId id) {
        return "dockering__"+ id.toString();
    }
    
    /*
    synchronized ProcessWrapper connect() {
        if(dockerClient != null) return dockerClient;
        dockerClient = new ProcessWrapper();
        return dockerClient;
    }*/
    
    private final static String findTag(String tag, String line) {
        String[] content = line.split("[\\s]++");
        if(tag.equalsIgnoreCase(content[1])) {
            return content[2].trim();
        }
        return null;
    }
    
    @Override
    public String getContainerIp(ComponentInstanceId myId) throws DockerException {
        ExecResult result = ProcessWrapper.singleDockerCommand("inspect", "--format='{{.NetworkSettings.IPAddress}}'", buildContainerName(myId));    
        if(!result.isSuccess()) {return null;}
        BufferedReader reader = new BufferedReader(new StringReader(result.getOutput()));
        try {
            String line = reader.readLine();
            if(line == null) throw new DockerException("could not find result");
            if(reader.readLine() != null) throw new DockerException("too many lines available");
            InetAddress addr = InetAddress.getByName(line.trim());
            return addr.getHostAddress();
        } catch(UnknownHostException he) {
            throw new DockerException("UnknownHostException when creating IP address", he);
        } catch(IOException ioe) {
            throw new DockerException("IOException while reading from string", ioe);
        }
    }
    
    @Override
    public String findImage(String target) throws DockerException {
        String[] split = target.split(":");
        ExecResult result = ProcessWrapper.singleDockerCommand("images", "--no-trunc=true", split[0]);
        if(!result.isSuccess()) {return null;}

        BufferedReader reader = new BufferedReader(new StringReader(result.getOutput()));
        try {
            String line = reader.readLine();
            while(line != null) {
                // skip header // 
                line = reader.readLine();
                if(line == null) break;
                String id = findTag(split[1], line);
                if(id != null) return id;
            }
        } catch(IOException ioe) {
            LOGGER.log(Level.SEVERE, "exception while reading from String", ioe);
        }
        return null;
    }

    @Override
    public void pullImage(String target) throws DockerException {
        ExecResult result = ProcessWrapper.singleDockerCommand("pull", target);
        if(result.isSuccess()) {return;}
        throw new DockerException(result.getError());
    }
    
    private static void createPortArguments(Map<Integer, Integer> inPortsParam, List<String> args) {
        for(Entry<Integer, Integer> entry : inPortsParam.entrySet()) {
            Integer i = entry.getKey();
            Integer j = entry.getValue();
            args.add("-p"); 
            if(j.intValue() < 0 || j.intValue() > 65536) {args.add(i.toString());}
            else {args.add(i.toString() + ":" + j.toString());}
        }
    }
        
    @Override
    public String createContainer(String image, ComponentInstanceId myId, Map<Integer,Integer> inPortsParam) throws DockerException {
        List<String> args = new ArrayList<>();
        args.add("create"); args.add("--name=" + buildContainerName(myId));
        createPortArguments(inPortsParam, args);
        args.add("--restart=no"); args.add("-i");  /*args.add("--tty=true");*/
        args.add(image); args.add("bash"); args.add("--noediting");
        
        ExecResult result = ProcessWrapper.singleDockerCommand(args.toArray(new String[args.size()]));
        if(result.isSuccess()) {
        	return result.getOutput().trim();
        }
        throw new DockerException(result.getError());
    }
    
    @Override
    public DockerShell startContainer(ComponentInstanceId myId) throws DockerException {
        Inprogress pw = ProcessWrapper.progressingDockerCommand("start", "-i", buildContainerName(myId));

        if(pw.processStillRunning()) { return pw; } 
        ExecResult result = pw.toExecutionResult();
        throw new DockerException("cannot start process; return value: " + result.exitCode() + "; " + result.getError());
    }

    @Override
    public String createImageSnapshot(ComponentInstanceId containerId, String key, OperatingSystem os) throws DockerException {
        final String author = "--author=" + "\"Cloudiator LifecylceAgent\"";
        final String message = "--message=" + "\"automatic snapshot after initialisation\"";
        
        ExecResult result = ProcessWrapper.singleDockerCommand("commit", author, message, 
                buildContainerName(containerId),  key);

        if(result.isSuccess()) {return result.getOutput();}
        throw new DockerException(result.getError());
        
    }

    @Override
    public DockerShell getSideShell(ComponentInstanceId myId) throws DockerException {
        Inprogress pw = ProcessWrapper.progressingDockerCommand("exec", "-i", buildContainerName(myId), "bash");
        if(pw.processStillRunning()) { 
        	return pw; 
        } 
        ExecResult result = pw.toExecutionResult();
        throw new DockerException("cannot start process; return value: " + result.exitCode() + "; " + result.getError());
    }

    @Override
    public int getPortMapping(ComponentInstanceId myId, Integer portNumber) throws DockerException {
        ExecResult result = ProcessWrapper.singleDockerCommand("port", buildContainerName(myId), portNumber.toString());    
        if(!result.isSuccess()) {return -1;}
        String line = result.getOutput();
        if(line == null) return -1;
        int idx = line.indexOf(":");
        if(idx == -1) return -1;
        try {
            return Integer.parseInt(line.substring(idx + 1));
        } catch(NumberFormatException nfe) {
            return -1;
        }
    }
}
