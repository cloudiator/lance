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

package de.uniulm.omi.cloudiator.lance.deployment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.CommandSequence;

public final class Deployment implements Serializable {

	private static final long serialVersionUID = 4353942659952970587L;
	
	private final List<CommandSequence> sequences;
	
	Deployment(ArrayList<CommandSequence> seqs) {
		sequences = seqs;
	}

	public void execute(LifecycleHandlerType handler, ExecutionContext ec) {
		for(CommandSequence cs : sequences) {
			cs.executeCommandsForLifecycle(handler, ec);
		}
	}

	public boolean hasLifecycleOperations(LifecycleHandlerType handler) {
		for(CommandSequence cs : sequences) {
			if(cs.hasLifecycleOperations(handler)) return true;
		}
		return false;
	}

}
