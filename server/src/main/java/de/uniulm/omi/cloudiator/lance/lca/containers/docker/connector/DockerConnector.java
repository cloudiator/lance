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

import java.util.Map;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.DockerShell;

public interface DockerConnector {

    // public final String LIFECYCLE_DIRECTORY = "/lifecycle";
    
    DockerShell startContainer(ComponentInstanceId myId) throws DockerException;

    void pullImage(String target) throws DockerException;

    /**
     * 
     * @param key should be the id to use including the registry 
     * @param os will be used as a tag in both cases
     * @return the name of the container
     * @throws DockerException 
     */
    String createSnapshotImage(ComponentInstanceId containerId, String key) throws DockerException;

    String createContainer(String image, ComponentInstanceId myId, Map<Integer, Integer> portsToSet) throws DockerException;

    String findImage(String target) throws DockerException ;

    String getContainerIp(ComponentInstanceId myId) throws DockerException;

    int getPortMapping(ComponentInstanceId myId, Integer portNumber) throws DockerException;

    DockerShell getSideShell(ComponentInstanceId myId) throws DockerException;

	void stopContainer(ComponentInstanceId myId) throws DockerException;

	void pushImage(String target) throws DockerException;;
}
