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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.DownloadCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.DownloadCommand.DownloadCommandFactory;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.ExecuteOnShellCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.ExecuteOnShellCommand.ExecuteOnShellCommandFactory;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.InstallSystemPackageCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.InstallSystemPackageCommand.InstallSystemPackageCommandFactory;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.ReplaceFileContentCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.ReplaceFileContentCommand.ReplaceFileContentCommandFactory;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.SetFilePropertiesCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.SetFilePropertiesCommand.SetFilePropertiesCommandFactory;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.SystemServiceCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.command.SystemServiceCommand.SystemServiceCommandFactory;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.install.SystemApplication;

public final class CommandSequenceBuilder {

	private final List<Command> commands = new ArrayList<Command>();
	private final String myName;
	private LifecycleHandlerType phase = LifecycleHandlerType.NEW;
	
	public CommandSequenceBuilder(String sequenceName) {
		myName = sequenceName;
	}
	
	public CommandSequenceBuilder addCommand(Command c) {
		commands.add(c);
		return this;
	}
	
	private CommandResultReference addCommandAndReturnResult(Command c) {
		addCommand(c);
		return c.getResult();
	}
	
	public CommandSequenceBuilder setPhase(LifecycleHandlerType _phase) {
		phase = _phase;
		return this;
	}
	
	public CommandResultReference download(URI uri) {
		DownloadCommand d = DownloadCommandFactory.create(phase, uri);
		return addCommandAndReturnResult(d);
	}

	public CommandResultReference download(String string) {
		URI uri = null;
		try {
			uri = new URI(string);
			return download(uri);
		} catch (URISyntaxException e) {
			throw new CommandException("wrong URL format", e);
		}
	}

	public CommandResultReference executeOnShell(CommandResultReference f) {
		ExecuteOnShellCommand e = ExecuteOnShellCommandFactory.createRootCommand(phase, f);
		return addCommandAndReturnResult(e);
	}

	public CommandResultReference replaceFileContent(CommandResultReference _ref, String pattern, String replacement) {
		ReplaceFileContentCommand c = ReplaceFileContentCommandFactory.create(phase, _ref, pattern, replacement);
		return addCommandAndReturnResult(c);
	}

	public CommandResultReference configureSystemService(String serviceName, String command) {
		SystemServiceCommand c = SystemServiceCommandFactory.create(phase, serviceName, command);
		return addCommandAndReturnResult(c);
	}

	public CommandSequence build() {
		return new CommandSequence(myName, new ArrayList<Command>(commands));
	}

	public CommandResultReference installSystemPackage(SystemApplication _app) {
		InstallSystemPackageCommand c = InstallSystemPackageCommandFactory.create(phase, _app);
		return addCommandAndReturnResult(c);
	}

	public CommandResultReference setFileProperties(int access, int users, CommandResultReference f) {
		SetFilePropertiesCommand c = SetFilePropertiesCommandFactory.setAccessRights(phase, access, users, f);		
		return addCommandAndReturnResult(c);
	}
}
