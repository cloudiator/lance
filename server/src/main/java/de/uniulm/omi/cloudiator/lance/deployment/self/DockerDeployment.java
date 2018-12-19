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

package de.uniulm.omi.cloudiator.lance.deployment.self;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandResultReference;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandSequence;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandSequenceBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.SetFilePropertiesCommand;

public class DockerDeployment {

  private DockerDeployment() {
    // no instances of this class //
  }

  public static CommandSequence create() {
    CommandSequenceBuilder b = new CommandSequenceBuilder("deploy and configure docker");
    b.setPhase(LifecycleHandlerType.PRE_INSTALL);
    CommandResultReference ref = b.download("https://get.docker.com/");

    b.setPhase(LifecycleHandlerType.INSTALL);
    // CommandResultReference f =
    b.setFileProperties(
        SetFilePropertiesCommand.SetFilePropertiesCommandConstants.ALL_ACCESS,
        SetFilePropertiesCommand.SetFilePropertiesCommandConstants.FILE_ALL,
        ref);
    b.executeOnShell(ref);

    b.setPhase(LifecycleHandlerType.POST_INSTALL);
    b.replaceFileContent(
        DockerConfigFileLocation.INSTANCE,
        "^#DOCKER_OPTS.*$",
        "DOCKER_OPTS=\"-H tcp://0.0.0.0:2375 -H unix:///var/run/docker.sock\"");

    b.setPhase(LifecycleHandlerType.START);
    b.configureSystemService("docker", "restart");
    return b.build();
  }
}
