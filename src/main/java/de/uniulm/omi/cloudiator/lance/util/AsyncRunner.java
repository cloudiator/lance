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

package de.uniulm.omi.cloudiator.lance.util;

import java.util.concurrent.FutureTask;

public class AsyncRunner implements Runnable {

    public interface Setter {
        void set();
    }
    
    private final Setter setter;
    
    private AsyncRunner(Setter s) {
    	setter = s;
    }
    
    @Override public void run() { 
    	setter.set(); 
    }
    
    public static<T> Thread createWrappedStateRunner(Setter s, AsyncCallback<T> callback) {
        AsyncRunner r = new AsyncRunner(s);
        FutureTask<T> ft = new FutureTask<>(r, null);
        Thread t = new Thread(ft);
        callback.call(ft);
        return t;
    }
}
