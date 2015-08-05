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

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.Shell;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.Command;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandResultReference;

public interface DownloadCommand extends Command {

	public static class DownloadCommandFactory {
	
		private final static Set<LifecycleHandlerType> supportedLifecycles;
		
		static {
			supportedLifecycles = EnumSet.of(LifecycleHandlerType.PRE_INSTALL);
		}
		
		public static DownloadCommand create(LifecycleHandlerType inPhase, URI _uri) {
			
			if(supportedLifecycles.contains(inPhase)) {
				return new DownloadCommandImpl(inPhase, _uri);
			}
			throw new IllegalStateException("SystemServiceCommand cannot be executed at Lifecylce Phase " + inPhase);
		}
		
		private DownloadCommandFactory() {
			// no instances so far //
		}
	}
}

class DownloadCommandImpl implements DownloadCommand {
	
	private final LifecycleHandlerType type;
	private final URI uri;
	private final DefaultCommandResultReference result = new DefaultCommandResultReference();
	private final String filename;

	DownloadCommandImpl(LifecycleHandlerType _type, URI _uri) {
		uri = _uri;
		type = _type;
		filename = UUID.randomUUID().toString();
	}
	
	@Override
	public CommandResultReference getResult() {
		return result;
	}

	@Override
	public boolean runsInLifecycle(LifecycleHandlerType _type) {
		return type == _type;
	}

	@Override
	public void execute(ExecutionContext ec) {
		OperatingSystem os = ec.getOperatingSystem();
		if(os.isLinuxOs()) {
			Shell shell = ec.getShell();
			// ExecutionResult exec_result = 
			shell.executeCommand("wget -O " + filename + " " + uri.toString());
			result.setResult(filename);
		} else {
			throw new IllegalStateException("Download is not implemented for Windows and Mac operating systems");
		}
	}
}
