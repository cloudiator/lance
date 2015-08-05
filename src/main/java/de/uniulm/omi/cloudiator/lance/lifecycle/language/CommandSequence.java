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

package de.uniulm.omi.cloudiator.lance.lifecycle.language;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;

/** 
 * a command sequence is an application- and component-
 * centric view on a lifecycle. In particular, it is 
 * possible to define the installation and configuration 
 * steps as if this was the only application to be run.
 * 
 * (Hopefully, ) this will enable to mix sequences of 
 * multiple components in one lifecycle.
 * 
 * @author Joerg Domaschka
 *
 */
public final class CommandSequence {

	private final List<Command> commands;
	private final String myName;
	
	CommandSequence(String _myName, List<Command> _commands) {
		commands = _commands;
		myName = _myName;
	}

	public boolean hasLifecycleOperations(LifecycleHandlerType type) {
		for(Command c : commands) {
			if(c.runsInLifecycle(type)) return true;
		}
		return false;
	}

	public void executeCommandsForLifecycle(LifecycleHandlerType type, ExecutionContext ec) {
		for(Command c : commands) {
			if(c.runsInLifecycle(type)) {
				c.execute(ec);
			}
		}
	}
}
