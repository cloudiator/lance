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

package de.uniulm.omi.cloudiator.lance.lca.container.port;

public final class DownstreamAddress{
	
	private final String hostIp; 
	private final Integer port;

	DownstreamAddress(String ip, Integer i) {
		hostIp = ip;
		port = i;
	}

	private static boolean equalObjects(Object o1, Object o2) {
		if(o1 == null && o2 == null) return true;
		if(o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}
	
	boolean hasValidPort() {
		return PortRegistryTranslator.isValidPort(port);
	}

	@Override
	public String toString() {
		return hostIp + ":" + port.toString();
	}

	@Override
	public boolean equals(Object o) {
		if(! (o instanceof DownstreamAddress)) return false;
		DownstreamAddress that = (DownstreamAddress) o;
		return equalObjects(this.hostIp, that.hostIp) && equalObjects(this.port, that.port);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hostIp == null) ? 0 : hostIp.hashCode());
		result = prime * result + ((port == null) ? 0 : port.hashCode());
		return result;
	}
}
