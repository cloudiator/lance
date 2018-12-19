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

package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.lifecycle.ShellFactory;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DockerShellFactory implements ShellFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(DockerShell.class);

  private final AtomicReference<DockerShellWrapper> reference = new AtomicReference<>();

  @Override
  public DockerShellWrapper createShell() {
    DockerShellWrapper wrapper = reference.get();
    if (wrapper == null) throw new IllegalStateException("shell not set");
    return wrapper;
  }

  void installDockerShell(DockerShell dshell) {
    final DockerShellWrapper wrapper = new DockerShellWrapper(dshell);
    DockerShellWrapper old = reference.getAndSet(wrapper);
    if (old != null) {
      LOGGER.error("ERROR: overriding docker shell with new one. this should never happen.");
    }
  }

  void closeShell() {
    DockerShellWrapper old = reference.getAndSet(null);
    if (old == null) {
      LOGGER.error("ERROR: no shell set that can be closed. this should never happen.");
    } else {
      old.shell.close();
    }
  }
}
