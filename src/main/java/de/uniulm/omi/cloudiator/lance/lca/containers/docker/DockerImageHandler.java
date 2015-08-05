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

package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;

final class DockerImageHandler {
	
	private final DockerOperatingSystemTranslator translator;
	private final OperatingSystem os;
	private final DockerConnector client;
	
	private volatile ImageCreationType initSource;
	
	DockerImageHandler(OperatingSystem _os, DockerOperatingSystemTranslator _translator, DockerConnector _client) {
		if(_os == null) throw new NullPointerException("operating system has to be set.");
		
		os = _os;
		translator = _translator;
		client = _client;
	}

	private String buildImageTagName(ImageCreationType type, String componentInstallId) {
		final String key;
		if(type == ImageCreationType.COMPONENT) { 
			String _key = componentInstallId; 
			String ostag = os.toString();
			ostag = ostag.replaceAll(":",  "_");
			key = _key.toLowerCase() + ":" + ostag.toLowerCase();
		}
		else if(type == ImageCreationType.OPERATING_SYSTEM) { key = translator.translate(os); }
		else if(type == ImageCreationType.COMPONENT_INSTANCE) { throw new UnsupportedOperationException(); }
		else { throw new IllegalArgumentException(); }
		
		return key;
	}
	
	private String doGetSingleImage(String key) {
		// TODO: remove this as soon as access to a private registry is set
		if(client.findImage(key) != null) return key;
				
		try { client.pullImage(key); return key; }
		catch(DockerException de) { return null; }
	}

	
	String doPullImages(ComponentInstanceId myId, String componentInstallId) {
		// first step: try to find matching image for configured component
		// currently not implemented; TODO: implement
		
		// second step: try to find matching image for prepared component
		String target = buildImageTagName(ImageCreationType.COMPONENT, componentInstallId);
		String result = doGetSingleImage(target);
		if(result != null) {
			initSource = ImageCreationType.COMPONENT;
			return result; //FIXME: set in component lifecycle stage
		}
		
		// third step
		target = buildImageTagName(ImageCreationType.OPERATING_SYSTEM, null);
		result = doGetSingleImage(target);
		if(result == null) {
			throw new RuntimeException(new ContainerException("cannot pull image: " + myId));
		}
		initSource = ImageCreationType.OPERATING_SYSTEM;
		return target;
	}

	/** here, we may want to run a snapshotting action 
	 * @throws DockerException */
	void runPostInstallAction(ComponentInstanceId myId, String componentInstallId) throws DockerException {
		if(initSource == ImageCreationType.OPERATING_SYSTEM) {
			// we probably will not need this return value
			// let's keep it for debugging purposes, though
			// @SuppressWarnings("unused") String imageSnapshot = 
			client.createImageSnapshot(myId, componentInstallId, os);
		}
	}
	
	static enum ImageCreationType {
		COMPONENT,
		COMPONENT_INSTANCE,
		OPERATING_SYSTEM,
	}

	OperatingSystem getOperatingSystem() { return os; }
}
