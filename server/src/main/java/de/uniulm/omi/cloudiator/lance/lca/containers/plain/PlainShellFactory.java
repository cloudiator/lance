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

package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.lca.containers.plain.shell.PlainShell;
import de.uniulm.omi.cloudiator.lance.lifecycle.ShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Daniel Seybold on 12.08.2015.
 */
public class PlainShellFactory implements ShellFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainShell.class);

    private final AtomicReference<PlainShellWrapper> reference = new AtomicReference<>();

    @Override
    public PlainShellWrapper createShell() {
        PlainShellWrapper wrapper = reference.get();
        if(wrapper == null)
            throw new IllegalStateException("plain shell not set");
        return wrapper;
    }

    void installPlainShell(PlainShell plainShell) {
        final PlainShellWrapper wrapper = new PlainShellWrapper(plainShell);
        PlainShellWrapper old = reference.getAndSet(wrapper);
        if(old != null) {
            LOGGER.error("ERROR: overriding plain shell with new one. this should never happen.");
        }
    }

    void closeShell() {
        PlainShellWrapper old = reference.getAndSet(null);
        if(old == null) {
            LOGGER.error("ERROR: no plain shell set that can be closed.");
        } else {
            old.plainShell.close();
        }
    }
}
