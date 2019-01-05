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

package de.uniulm.omi.cloudiator.lance.lifecycle.bash;

import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionResult;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorState;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.StartDetector;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.InstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PostInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreInstallHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.PreStopHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StartHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.StopHandler;
import java.util.ArrayList;
import java.util.List;

public final class BashBasedHandlerBuilder {

  private final List<String[]> commands = new ArrayList<>();
  private volatile OperatingSystem os;

  public BashBasedHandlerBuilder() {
    //
  }

  public PortUpdateHandler buildPortUpdateHandler() {
    return new BashPortUpdateHandler(os, commands);
  }

  public StartDetector buildStartDetector() {
    return new BashStartDetectorHandler(os, commands);
  }

  public LifecycleHandler build(LifecycleHandlerType type) {
    LifecycleHandler retVal = null;
    switch (type) {
      case PRE_INSTALL:
        retVal = new BashPreInstallHandler(os, commands);
        break;
      case INSTALL:
        retVal = new BashInstallHandler(os, commands);
        break;
      case POST_INSTALL:
        retVal = new BashPostInstallHandler(os, commands);
        break;
      case START:
        retVal = new BashStartHandler(os, commands);
        break;
      case PRE_STOP:
        retVal = new BashPreStopHandler(os, commands);
        break;
      case STOP:
        retVal = new BashStopHandler(os, commands);
        break;
      case INIT:
      default:
        throw new UnsupportedOperationException();
    }
    return retVal;
  }

  public BashBasedHandlerBuilder setOperatingSystem(OperatingSystem osParam) {
    os = osParam;
    return this;
  }

  public void addCommand(String... args) {
    commands.add(args);
  }
}

final class BashPreInstallHandler implements PreInstallHandler {

  private static final long serialVersionUID = 7232719469227703730L;
  private final OperatingSystem os;
  private final List<String[]> commands;

  BashPreInstallHandler(OperatingSystem osParam, List<String[]> commandsParam) {
    os = osParam;
    commands = commandsParam;
  }

  @Override
  public void execute(ExecutionContext ec) {
    BashExecutionHelper.executeCommands(os, ec, commands);
  }
}

final class BashInstallHandler implements InstallHandler {

  private static final long serialVersionUID = -2546167412074322307L;
  private final OperatingSystem os;
  private final List<String[]> commands;

  BashInstallHandler(OperatingSystem osParam, List<String[]> commandsParam) {
    os = osParam;
    commands = commandsParam;
  }

  @Override
  public void execute(ExecutionContext ec) {
    BashExecutionHelper.executeCommands(os, ec, commands);
  }
}

final class BashPostInstallHandler implements PostInstallHandler {

  private static final long serialVersionUID = 1169280018441041612L;
  private final OperatingSystem os;
  private final List<String[]> commands;

  BashPostInstallHandler(OperatingSystem osParam, List<String[]> commandsParam) {
    os = osParam;
    commands = commandsParam;
  }

  @Override
  public void execute(ExecutionContext ec) {
    BashExecutionHelper.executeCommands(os, ec, commands);
  }
}

final class BashStartHandler implements StartHandler {

  private static final long serialVersionUID = 1905532285239985777L;
  private final OperatingSystem os;
  private final List<String[]> commands;

  BashStartHandler(OperatingSystem osParam, List<String[]> commandsParam) {
    os = osParam;
    commands = commandsParam;
  }

  @Override
  public void execute(ExecutionContext ec) {
    BashExecutionHelper.executeBlockingCommands(os, ec, commands);
  }
}

final class BashStopHandler implements StopHandler {

  private static final long serialVersionUID = -5705492165282492295L;
  private final OperatingSystem os;
  private final List<String[]> commands;

  BashStopHandler(OperatingSystem osParam, List<String[]> commandsParam) {
    os = osParam;
    commands = commandsParam;
  }

  @Override
  public void execute(ExecutionContext ec) {
    BashExecutionHelper.executeBlockingCommands(os, ec, commands);
  }
}

final class BashPreStopHandler implements PreStopHandler {

  private static final long serialVersionUID = 3138983756438002674L;
  private final OperatingSystem os;
  private final List<String[]> commands;

  BashPreStopHandler(OperatingSystem osParam, List<String[]> commandsParam) {
    os = osParam;
    commands = commandsParam;
  }

  @Override
  public void execute(ExecutionContext ec) {
    BashExecutionHelper.executeBlockingCommands(os, ec, commands);
  }
}

final class BashPortUpdateHandler implements PortUpdateHandler {

  private static final long serialVersionUID = 1095927280515962263L;
  private final OperatingSystem os;
  private final List<String[]> commands;

  BashPortUpdateHandler(OperatingSystem osParam, List<String[]> commandsParam) {
    os = osParam;
    commands = commandsParam;
  }

  @Override
  public void execute(ExecutionContext ec) {
    BashExecutionHelper.executeCommands(os, ec, commands);
  }
}

final class BashStartDetectorHandler implements StartDetector {

  private static final long serialVersionUID = 1123459320914315021L;
  private final OperatingSystem os;
  private final List<String[]> commands;

  BashStartDetectorHandler(OperatingSystem osParam, List<String[]> commandsParam) {
    os = osParam;
    commands = commandsParam;
  }

  @Override
  public DetectorState execute(ExecutionContext ec) {
    BashExecutionHelper.executeCommands(os, ec, commands);
    // TODO: In case, a plain-shell is used: "export STARTED=true" doesn't work out -> refactor
    // PlainShellImpl, so that the variable gets exported into the following shell context
    ExecutionResult result =
        BashExecutionHelper.doExecuteCommand(false, "echo -n \"$STARTED\"", ec.getShell());
    if (result.isSuccess()) {
      if ("true".equals(result.getOutput().trim())) {
        return DetectorState.DETECTED;
      }
      if ("false".equals(result.getOutput().trim())) {
        return DetectorState.NOT_DETECTED;
      }
    }
    return DetectorState.DETECTION_FAILED;
  }
}
