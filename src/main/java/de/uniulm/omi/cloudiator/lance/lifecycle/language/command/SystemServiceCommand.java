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

import java.util.EnumSet;
import java.util.Set;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.Command;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandResultReference;

public interface SystemServiceCommand extends Command {

	public static class SystemServiceCommandFactory {
		
		private final static Set<LifecycleHandlerType> supportedLifecycles;
		
		static {
			supportedLifecycles = EnumSet.of(LifecycleHandlerType.START);
		}
		
		public static SystemServiceCommand create(LifecycleHandlerType inPhase, String serviceName, String command) {
			if(supportedLifecycles.contains(inPhase)) {
				return new SystemServiceCommandImpl(inPhase, serviceName, command);
			}
			throw new IllegalStateException("SystemServiceCommand cannot be executed at Lifecylce Phase " + inPhase);
		}
		
		private SystemServiceCommandFactory() {
			// no instances so far //
		}
	}
}

class SystemServiceCommandImpl implements SystemServiceCommand {
	
	private final LifecycleHandlerType type;
	private final String serviceName;
	private final String command;
	private final CommandResultReference result = new DefaultCommandResultReference();

	SystemServiceCommandImpl(LifecycleHandlerType _type, String _serviceName, String _command) {
		serviceName = _serviceName;
		command = _command;
		type = _type;
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
		throw new UnsupportedOperationException();
	}
}
