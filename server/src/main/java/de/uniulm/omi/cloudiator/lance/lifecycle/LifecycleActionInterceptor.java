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

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;

public interface LifecycleActionInterceptor {

    void prepare(HandlerType type) throws ContainerException;

    void postprocess(HandlerType type);

	ComponentInstanceId getComponentId();

	void postprocessPortUpdate(PortDiff<DownstreamAddress> diff);

	void preprocessPortUpdate(PortDiff<DownstreamAddress> diff) throws ContainerException;

	void postprocessDetector(DetectorType type);

	void preprocessDetector(DetectorType type) throws ContainerException;
}
