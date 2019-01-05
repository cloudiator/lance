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

package de.uniulm.omi.cloudiator.lance.lifecycle;

public final class ExecutionResult {

  private final Status status;
  private final int exitCode;
  private final String stdout;
  private final String stderr;

  public ExecutionResult(
      Status statusParam, int exitCodeParam, String stdoutParam, String stderrParam) {
    status = statusParam;
    exitCode = exitCodeParam;
    stdout = stdoutParam;
    stderr = stderrParam;
  }

  public static ExecutionResult success(String stdout, String stderr) {
    return new ExecutionResult(Status.SUCCESS, 0, stdout, stderr);
  }

  public static ExecutionResult commandFailure(int exitCode, String stdOut, String error) {
    return new ExecutionResult(Status.COMMAND_FAILURE, exitCode, stdOut, error);
  }

  public static ExecutionResult systemFailure(String error) {
    return new ExecutionResult(Status.SYSTEM_FAILURE, -1, "", error);
  }

  public boolean isSuccess() {
    return status == Status.SUCCESS;
  }

  public String getOutput() {
    return stdout;
  }

  @Override
  public String toString() {
    return "[" + exitCode + "-> {" + stdout + "},{" + stderr + "}]";
  }

  enum Status {
    SUCCESS,
    COMMAND_FAILURE,
    SYSTEM_FAILURE,
  }
}
