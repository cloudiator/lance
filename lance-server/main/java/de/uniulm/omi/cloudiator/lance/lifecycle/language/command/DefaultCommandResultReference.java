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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandResultReference;

public final class DefaultCommandResultReference implements CommandResultReference {

    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<String> result = new AtomicReference<>();

    void setResult(String s) {
        if(latch.getCount() != 1L) {
            throw new IllegalStateException();
        }
        result.set(s);
        latch.countDown();
     }
    
    @Override
    public String getResult(OperatingSystem os, ExecutionContext ec) {
        if(latch.getCount() != 0L) {
            throw new IllegalStateException();
        }
        return result.get();
    }
}
