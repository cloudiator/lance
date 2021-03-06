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
import java.util.ArrayList;
import java.util.List;

/**
 * may be used to unregister service instance at the load balancer
 * 
 * @author Joerg Domaschka
 */
public interface PreStopHandler extends LifecycleHandler {

    // marker interface
}

final class PreStopDeploymentHandler implements PreStopHandler {

    private static final long serialVersionUID = -6199437441592573827L;
    private final Deployment d;
    
    PreStopDeploymentHandler(Deployment deploymentParam) {
        d = deploymentParam;
    }

    @Override
    public void execute(ExecutionContext ec) {
        d.execute(LifecycleHandlerType.PRE_STOP, ec);
    }

    @Override
    public boolean isEmpty() {
      //todo: loop over all lifecycleHanlerTypes and check
      //return d.hasLifecycleOperations(TYPE);
      return true;
    }
}
