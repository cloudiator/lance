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

package de.uniulm.omi.cloudiator.lance.lifecycle;

public final class LifecycleStoreBuilder {

    private final LifecycleHandler[] handlers = new LifecycleHandler[LifecycleHandlerType.values().length];
    
    public LifecycleStoreBuilder() {
        // empty!
    }
    
    public LifecycleStoreBuilder setHandler(LifecycleHandler h, LifecycleHandlerType t) {
        if(h == null) throw new NullPointerException();
        Class<?> superClass = t.getTypeClass();
        Class<?> inheritCla = h.getClass();
        if(! superClass.isAssignableFrom(inheritCla)) {
            throw new IllegalArgumentException("handler types do not match: " + h.getClass() + " vs. " + t.getTypeClass());
        }
        if(t == LifecycleHandlerType.NEW) {
        	throw new IllegalArgumentException("cannot set a handler for 'NEW'. " + "This event is a system event");
        }

        handlers[t.ordinal()] = h;
        return this;
    }
    
    public LifecycleStore build() {
        for(LifecycleHandlerType t : LifecycleHandlerType.values()) {
            if(handlers[t.ordinal()] == null) {
                handlers[t.ordinal()] = t.getDefaultImplementation();
            }
        }
        return new LifecycleStore(handlers);
    }
    
}
