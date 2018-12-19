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

package de.uniulm.omi.cloudiator.lance.lifecycle.handlers;

import de.uniulm.omi.cloudiator.lance.deployment.Deployment;
import de.uniulm.omi.cloudiator.lance.lifecycle.ExecutionContext;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;

/**
 * may be used to register service instances with a load balancer
 *
 * @author Joerg Domaschka
 */
public interface PostStartHandler extends LifecycleHandler {
  // marker interface
}

final class PostStartDeploymentHandler implements PostStartHandler {

  private static final long serialVersionUID = -8885825960661042962L;
  private final Deployment d;

  PostStartDeploymentHandler(Deployment deploymentParam) {
    d = deploymentParam;
  }

  @Override
  public void execute(ExecutionContext ec) {
    d.execute(LifecycleHandlerType.POST_START, ec);
  }
}
