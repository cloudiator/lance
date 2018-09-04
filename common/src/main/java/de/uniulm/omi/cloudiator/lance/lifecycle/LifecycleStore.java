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

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StartDetector;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StopDetector;

public class LifecycleStore implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleStore.class);
    private static final long serialVersionUID = 1L;

    private final LifecycleHandler[] handlers;
    private final StartDetector startDetector;
    private final StopDetector stopDetector;
    
    LifecycleStore(LifecycleHandler[] handlersParam, StartDetector startDetectorParam, StopDetector stopDetectorParam) {
        handlers = handlersParam;
        assert handlers.length == LifecycleHandlerType.values().length : "array too small";
        for(LifecycleHandlerType t : LifecycleHandlerType.values()) {
            LifecycleHandler h = getCastHandler(t);
            if(h == null) 
                throw new IllegalStateException("not the correct lifecycle handler");
        }
        startDetector = startDetectorParam;
        stopDetector = stopDetectorParam;
    }
    
    public <T extends LifecycleHandler> T getHandler(LifecycleHandlerType t, Class<T> type) {
        assert t.getTypeClass() == type : "types do not match";
        
        LifecycleHandler h = getCastHandler(t);
        @SuppressWarnings("unchecked")
        T tt = (T) h;
        return tt;
    }
    
    private LifecycleHandler getCastHandler(LifecycleHandlerType t) {
        LifecycleHandler h = handlers[t.ordinal()];
        if(h == null) {
            return null;
        }
        try {
            return t.getTypeClass().cast(h);
        } catch(ClassCastException cce) {
            LOGGER.error("could not retrieve correct handler due to incompatible types", cce);
            return null;
        }
    }

	public StartDetector getStartDetector() {
		return startDetector;
	}

	public StopDetector getStopDetector() {
		return stopDetector;
	}
}
