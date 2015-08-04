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

package de.uniulm.omi.cloudiator.lance.container.spec.os;

public abstract class OSVersionFormat {

	public static final OSVersionFormat UBUNTU_VERSION_FORMAT = new OSVersionFormat() {
		
		@Override public boolean hasCorrectFormat(String format) {
			if(format == null || format.length() != 5) return false;
			return format.matches("\\d\\d\\.\\d\\d");
		}
	};
	
	public static final OSVersionFormat WINDOWS_VERSION_FORMAT = new OSVersionFormat() {
		
		private final String[] versions = {"95", "98", "2000", "ME", "Vista", "7", "8", "8.1" };
		
		@Override public boolean hasCorrectFormat(String format) {
			for(String v : versions) {
				if(v.equalsIgnoreCase(format)) return true;
			}
			return false;
		}
	};
	
	public static final OSVersionFormat UNKNOWN_VERSION_FORMAT = new OSVersionFormat() {
		
		@Override public boolean hasCorrectFormat(String format) {
			return false;
		}
	};
	
	public abstract boolean hasCorrectFormat(String format);
	
	private OSVersionFormat() {
		
	}
}
