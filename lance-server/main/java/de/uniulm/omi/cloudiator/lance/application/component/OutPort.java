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

package de.uniulm.omi.cloudiator.lance.application.component;

import java.io.Serializable;

import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;

public final class OutPort implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public static final int INFINITE_SINKS = -1;
    public static final int NO_SINKS = -2;
    
    private final PortUpdateHandler handler;
    private final String name;
    private final int cardinality;
    private final int min;
    private final int max;
    
    /**
     * 
     * @param nameParam
     * @param handlerParam
     * @param cardinalityParam
     * @param minSinksParam
     * @param maxSinksParam
     * 
     * @throws NullPointerException if name is null
     */
    OutPort(String nameParam, PortUpdateHandler handlerParam, int cardinalityParam, int minSinksParam, int maxSinksParam) {
        if(nameParam == null) 
            throw new NullPointerException("name has to be set");
        name = nameParam;
        cardinality = cardinalityParam;
        min = minSinksParam;
        max = maxSinksParam;
        handler = handlerParam;
    }
    
    public boolean canHandleInfiniteSinks() {
        return max == INFINITE_SINKS;
    }
    
    public boolean canHandleNoSinks() {
        return min == NO_SINKS;
    }

    public String getName() {
        return name;
    }

    public int getLowerBound() { 
    	return min; 
    }

    public int getUpperBound() { 
    	return max; 
    }
    
    @Override
    public String toString() {
        return name + ": [" + min + "," + max + "]";
    }

    public PortUpdateHandler getUpdateHandler() { 
    	return handler; 
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + cardinality;
        result = prime * result + max;
        result = prime * result + min;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof OutPort)) {
            return false;
        }
        OutPort that = (OutPort) o;
        return name.equals(that.name) && this.max == that.max 
                && this.min == that.min && this.cardinality == that.cardinality;
    }

    public boolean namesMatch(OutPort that) {
        if(this == that) {
            return true;
        }
        if(that == null || that.name == null) {
            return false;
        }
        return this.name.equals(that.name);
    }
}
