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

package de.uniulm.omi.cloudiator.lance.lca.containers.docker;

import java.util.List;
import java.util.Map;

import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.container.standard.ContainerLogic;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.environment.BashExportBasedVisitor;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortRegistryTranslator;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerConnector;
import de.uniulm.omi.cloudiator.lance.lca.containers.docker.connector.DockerException;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleController;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;

public class DockerContainerLogic implements ContainerLogic {
		
	private final ComponentInstanceId myId;
	private final DockerConnector client;
	private final DockerShellFactory shellFactory = new DockerShellFactory();
	
	private final DeployableComponent myComponent;
	private final DeploymentContext deploymentContext;

	private final LifecycleController controller;
	private final GlobalRegistryAccessor registryAccessor;
		
	private final DockerImageHandler imageHandler;
	private final NetworkHandler portHandler;
	
	DockerContainerLogic(ComponentInstanceId id, DockerConnector _client, DockerOperatingSystemTranslator translator, 
							GlobalRegistryAccessor accessor, DeployableComponent comp, DeploymentContext ctx, OperatingSystem _os,
							NetworkHandler network) {
		this(id, _client, _os, translator, ctx, accessor, comp, network);
	}
	
	private  DockerContainerLogic(ComponentInstanceId id, DockerConnector _client, OperatingSystem _os,
								DockerOperatingSystemTranslator _translator, DeploymentContext ctx,
								GlobalRegistryAccessor accessor, DeployableComponent _component, NetworkHandler _network) {
		
		if(_os == null) throw new NullPointerException("operating system has to be set.");
		
		myId = id;
		client = _client;
		imageHandler = new DockerImageHandler(_os, _translator, _client);
		deploymentContext = ctx;
		myComponent = _component;
		controller = new LifecycleController(myComponent.getLifecycleStore(), imageHandler.getOperatingSystem(), shellFactory);
		registryAccessor = accessor;
		
		portHandler = _network;
		try { registryAccessor.init(myId); }
		catch(RegistrationException re) { throw new IllegalStateException("cannot start container", re); }
	}
	
	//@Override public ComponentInstanceId getId() { return myId; }
		
	@Override
	public synchronized void doCreate() throws ContainerException {
		String target = null;
		try { 
			registryAccessor.updateState(myId, LifecycleHandlerType.INIT);
			target = imageHandler.doPullImages(myId, createComponentInstallId());
			portHandler.initPorts(DockerContainerManagerFactory.PORT_HIERARCHY_2, "<unknown>"); 
		} catch(RegistrationException re) {throw new ContainerException("cannot access registry", re);}
		
		Map<Integer,Integer> ports_to_set = portHandler.findPortsToSet(deploymentContext);
		try { @SuppressWarnings("unused") String dockerId = client.createContainer(target, myId, ports_to_set); }
		catch(DockerException de) { throw new ContainerException("cannot create container: " + myId); }
		
		// add dummy values so that other components are aware of this instance, 
		// but can see that it is not ready for use yet.
		portHandler.publishLocalData(myId);
	}
	
	@Override
	public void doInit(LifecycleStore store) throws ContainerException {
		try {
			registryAccessor.updateState(myId, LifecycleHandlerType.PRE_INSTALL);
			final DockerShell _dshell = doStartContainer();
			
			registryAccessor.updateState(myId, LifecycleHandlerType.INSTALL);
			executeInstallation();
			registryAccessor.updateState(myId, LifecycleHandlerType.POST_INSTALL);
			registryAccessor.updateState(myId, LifecycleHandlerType.PRE_START);
			executeConfiguration(_dshell);
			
			executeStart();
		} catch(ContainerException ce) {
			throw ce;
		} catch(Exception ex) {
			throw new ContainerException(ex);
		} finally {
			shellFactory.closeShell();
		}
	}

	@Override
	public void doDestroy() {
		throw new UnsupportedOperationException();
	}
	
	private DockerShell doStartContainer() {
		final DockerShell _dshell;
		try { _dshell = client.startContainer(myId); }
		catch(DockerException de) {
			throw new RuntimeException(new ContainerException("cannot start container: " + myId, de));
		}
		//FIXME: make sure that start detector has been run successfully 
		//FIXME: make sure that stop detectors run periodically //
		shellFactory.installDockerShell(_dshell);
		return _dshell;
	}
	
	private void executeStart() throws DockerException, ContainerException, RegistrationException {
		registryAccessor.updateState(myId, LifecycleHandlerType.PRE_START);
		registryAccessor.updateState(myId, LifecycleHandlerType.START);
		controller.blockingStart();
		
		registryAccessor.updateState(myId, LifecycleHandlerType.POST_START);
		// only that we have started, can the ports be 
		// retrieved and then registered at the registry // 
		portHandler.publishLocalData(myId);
		portHandler.startPortUpdaters();
	}
	
	private String createComponentInstallId() {
		return "dockering." + "component." + myComponent.getComponentId().toString(); 
	}
	
	private void executeInstallation() throws DockerException {
		//TODO: add code for starting from snapshot (skip init and install steps)
		
		controller.blockingInit();
		controller.blockingInstall();
		
		imageHandler.runPostInstallAction(myId, createComponentInstallId());
		registerPortMappings();
	}
	
	private void executeConfiguration(DockerShell _dshell) throws DockerException {
		String containerIp = client.getContainerIp(myId);
		portHandler.updateAddress(DockerContainerManagerFactory.PORT_HIERARCHY_2, containerIp);
		portHandler.pollForNeededConnections();
		
		prepareEnvironment(_dshell);
		// TODO: do we to have make a snapshot after this? // 
		controller.blockingConfigure();
	}

	/** retrieved the actual port numbers and the way docker maps them */
	private void registerPortMappings() {
		// for all ports, get the port mapping //
		List<InPort> in_ports = myComponent.getExposedPorts();
		for(InPort in : in_ports) {
			String name = in.getPortName();
			Integer portNumber = (Integer) deploymentContext.getProperty(name, InPort.class);
			int mapped = client.getPortMapping(myId, portNumber);
			
			portHandler.registerInPort(PortRegistryTranslator.PORT_HIERARCHY_0, name, mapped);
			portHandler.registerInPort(PortRegistryTranslator.PORT_HIERARCHY_1, name, mapped);
			portHandler.registerInPort(DockerContainerManagerFactory.PORT_HIERARCHY_2, name, portNumber);
		}
	}
	
	private void prepareEnvironment(DockerShell _dshell) {
		BashExportBasedVisitor visitor = new BashExportBasedVisitor(_dshell);
		visitor.addEnvironmentVariable("TERM", "dumb");
		portHandler.accept(visitor);
		myComponent.accept(deploymentContext, visitor);
	}
}
