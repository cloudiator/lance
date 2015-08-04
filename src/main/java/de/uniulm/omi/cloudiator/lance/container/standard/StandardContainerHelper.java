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

package de.uniulm.omi.cloudiator.lance.container.standard;

import java.util.Arrays;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

final class StandardContainerHelper {

	private StandardContainerHelper() {
		// no instances of this class //
	}

	static void checkForCreationParameters(Object[] o){
		//if(o == null || o.length == 0 || o.length > 1 || !(o[0] instanceof OperatingSystem)) throw new IllegalArgumentException(Arrays.toString(o));
		if(o == null || o.length > 0) throw new IllegalArgumentException(Arrays.toString(o));
		// return (OperatingSystem) o[0];
		return;
	}
	
	static LifecycleStore checkForInitParameters(Object[] o){
		if(o == null || o.length == 0 || o.length > 1 || !(o[0] instanceof LifecycleStore)) throw new IllegalArgumentException(Arrays.toString(o));
		return (LifecycleStore) o[0];
	}
}
