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

package de.uniulm.omi.cloudiator.lance.lca.container;

import java.io.Serializable;
import java.util.UUID;

public final class ComponentInstanceId implements Serializable {

    // FIXME: set fixed value!
    public static final ComponentInstanceId ERROR_ID = new ComponentInstanceId();
    public static final ComponentInstanceId SYSTEM_ID = new ComponentInstanceId();
    private static final long serialVersionUID = 1L;

    private final UUID uuid;
    
    public ComponentInstanceId(){
        uuid = UUID.randomUUID();
    }
    
    private ComponentInstanceId(String s){
        uuid = UUID.fromString(s);
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ComponentInstanceId)) {
            return false; // captures null
        }
        ComponentInstanceId that = (ComponentInstanceId) o;
        return this.uuid.equals(that.uuid);
    }
    
    @Override 
    public int hashCode() {
        return uuid.hashCode();
    }
    
    @Override
    public String toString(){
        return uuid.toString();
    }
    
    public static ComponentInstanceId fromString(String s){
        return new ComponentInstanceId(s);
    }
}
