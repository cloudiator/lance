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
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerFactory;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;

/**
 * may be used to register service instances with a load balancer
 * 
 * @author Joerg Domaschka
 */
public interface PostStartHandler extends LifecycleHandler {

    public static final LifecycleHandlerFactory<PostStartHandler> FACTORY = new LifecycleHandlerFactory<PostStartHandler>() {
        @Override public final PostStartHandler getDefault() { return DefaultHandlers.DEFAULT_POST_START_HANDLER; }

        @Override
        public PostStartHandler getDeploymentHandler(Deployment d) {
            return new PostStartDeploymentHandler(d);
        } 
    };
}

final class PostStartDeploymentHandler implements PostStartHandler {
    
    private final Deployment d;
    
    PostStartDeploymentHandler(Deployment _d) {
        d = _d;
    }

    @Override
    public void execute(ExecutionContext ec) {
        d.execute(LifecycleHandlerType.POST_START, ec);
    }
}
