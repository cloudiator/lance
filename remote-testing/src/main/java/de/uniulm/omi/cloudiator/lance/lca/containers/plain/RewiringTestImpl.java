/*
 * Copyright (c) 2014-2018 University of Ulm
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

package de.uniulm.omi.cloudiator.lance.lca.containers.plain;

import de.uniulm.omi.cloudiator.lance.lca.EnvContextWrapperRM;
import de.uniulm.omi.cloudiator.lance.lca.LcaRegistry;
import de.uniulm.omi.cloudiator.lance.lca.RewiringTestAgent;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import de.uniulm.omi.cloudiator.lance.lifecycle.*;
import de.uniulm.omi.cloudiator.lance.container.standard.ErrorAwareContainer;
import de.uniulm.omi.cloudiator.lance.lca.container.port.NetworkHandler;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponentBuilder;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.lca.GlobalRegistryAccessor;
import de.uniulm.omi.cloudiator.lance.lifecycle.bash.BashBasedHandlerBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.DefaultHandlers;
import de.uniulm.omi.cloudiator.lance.lifecycles.CoreElementsRewiring;
import de.uniulm.omi.cloudiator.lance.util.application.*;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;

//modified LcAImplementation.java
public class RewiringTestImpl implements RewiringTestAgent {

    private volatile List<FullComponent> fullComponents;
    private volatile CoreElementsRewiring core;

    private static final int DEFAULT_PROPERTIES = 5;
    private static final String INITIAL_LOCAL_ADDRESS = "<unknown>";

    private volatile Map<ComponentInstanceId, Map<String, String>> dumb;

    public RewiringTestImpl() {}

    @Override
    public ApplicationInstanceId testNewTopology(AppArchitecture arch, String publicIp, LcaRegistry reg) throws ContainerException, RemoteException {
        //assertNotNull(CoreElements.context);
        CoreElementsRewiring.arch = arch;
        CoreElementsRewiring.initHostContext(publicIp);
        core = new CoreElementsRewiring(reg);

        try {
            core.setUpRegistry();
        } catch (RegistrationException e) {
            e.printStackTrace();
        }

        init(arch);

        for(FullComponent fullComp: fullComponents) {
            try {
                core.fillRegistry(fullComp.cId);
            } catch (RegistrationException e) {
                e.printStackTrace();
            }
            fullComp.addContainer(createNewContainer(fullComp));
        }

        return arch.getAppInstanceId();
    }

    @Override
    public void testSMTraversingBeforeLccSM() throws ContainerException, RemoteException  {
        for(FullComponent fullComp: fullComponents) {
            assert fullComp != null;
            ErrorAwareContainer<PlainContainerLogic> container = fullComp.container;
            /*
            Possible Transitions in ErrorAwareContainer (flow: Set TransitionActions in StateMachineBuilder and associate the actions with the "this-object". When transitions are fired via
            the "public methods", e.g. public void create(), the TransitionActions call the "non-public methods" of the this objects (via different threads), e.g. void preCreateAction())
            public void create() : ContainerStatus.NEW, ContainerStatus.CREATED
                -Via CreateTransitionAction-
                theContainer.preCreateAction() : //network-stuff
                theContainer.logic.doCreate() : //Directory-Structure
                theContainer.postCreateAction() : //network-stuff
            public void bootstrap() : ContainerStatus.CREATED, ContainerStatus.BOOTSTRAPPED
                -Via BootstrapTransitionAction-
                theContainer.logic.doInit(null) : //
                theContainer.postBootstrapAction() : //network-stuff
            */
            container.create();
            container.awaitCreation();
            //fullComp.networkHandler.startPortUpdaters(fullComp.lcc);
            container.bootstrap();
            container.awaitBootstrap();

            assertRightState(ContainerStatus.BOOTSTRAPPED, container);

            //todo: adjust, if multiple component-instances of same component
            try {
                checkBasicRegistryValue(1, fullComp.cId);
            } catch (RegistrationException e) {
                e.printStackTrace();
            }
            assert dumb != null;

            checkForDumbElements(
                    new String[] {"HOST_CONTAINER_IP", "Instance_Number", "HOST_PUBLIC_IP", "HOST_CLOUD_IP", "Container_Status"},
                    DEFAULT_PROPERTIES, fullComp.cInstId);

            Map<String,String> cids = dumb.get(fullComp.cId);
            assert INITIAL_LOCAL_ADDRESS == cids.get("HOST_CONTAINER_IP");
            assert EnvContextWrapperRM.getPublicIp() == cids.get("HOST_PUBLIC_IP");
            assert EnvContextWrapperRM.getCloudIp() == cids.get("HOST_CLOUD_IP");
            assert "1" == cids.get("Instance_Number");
        }
    }

    @Override
    public void testSMsTraversingBeforeStop()  throws ContainerException, RemoteException  {
        for(FullComponent fullComp: fullComponents) {
            assert fullComp != null;
            ErrorAwareContainer<PlainContainerLogic> container = fullComp.container;
            assertRightState(ContainerStatus.BOOTSTRAPPED, container);
            /*
            Possible Transitions in ErrorAwareContainer (flow: Set TransitionActions in StateMachineBuilder and associate the actions with the "this-object". When transitions are fired via
            the "public methods", e.g. public void create(), the TransitionActions call the "non-public methods" of the this objects (via different threads), e.g. void preCreateAction())
            public void init(LifecycleStore store) : ContainerStatus.BOOTSTRAPPED, ContainerStatus.READY
                -Via InitTransitionAction-
                theContainer.preInitAction() :
                    controller.blockingInit() : LifecycleHandlerType.NEW, LifecycleHandlerType.INIT
                    controller.blockingInstall() : LifecycleHandlerType.INIT, LifecycleHandlerType.PRE_INSTALL, LifecycleHandlerType.PRE_INSTALL, LifecycleHandlerType.INSTALL
                    controller.blockingConfigure() : LifecycleHandlerType.INSTALL, LifecycleHandlerType.POST_INSTALL, LifecycleHandlerType.POST_INSTALL, LifecycleHandlerType.PRE_START
                    controller.blockingStart() : LifecycleHandlerType.PRE_START, LifecycleHandlerType.START, stopDetector.scheduleDetection(hostContext), LifecycleHandlerType.START, LifecycleHandlerType.POST_START
                theContainer.logic.completeInit() : //HERE: TEST WHY SHELL MUST BE CLOSED HERE
                theContainer.postInitAction() : //network-stuff
            */
            container.init(fullComp.comp.getLifecycleStore());
            container.awaitInitialisation();

            assertRightState(ContainerStatus.READY, container);

            //todo: adjust, if multiple component-instances of same component
            try {
                checkBasicRegistryValue(1, fullComp.cId);
            } catch (RegistrationException e) {
                e.printStackTrace();
            }
            assert dumb != null;

            checkForDumbElements(
                    new String[] {"HOST_CONTAINER_IP", "Instance_Number", "HOST_PUBLIC_IP", "HOST_CLOUD_IP", "Container_Status"},
                    DEFAULT_PROPERTIES, fullComp.cInstId);

            Map<String,String> cids = dumb.get(fullComp.cId);
            assert INITIAL_LOCAL_ADDRESS == cids.get("HOST_CONTAINER_IP");
            assert EnvContextWrapperRM.getPublicIp() == cids.get("HOST_PUBLIC_IP");
            assert EnvContextWrapperRM.getCloudIp() == cids.get("HOST_CLOUD_IP");
            assert "1" == cids.get("Instance_Number");
        }
    }

    @Override
    public void testSMsStopTransition() throws ContainerException, RemoteException  {
        for (FullComponent fullComp : fullComponents) {
            assert fullComp != null;

            if(!fullComp.stopIt)
                continue;

            ErrorAwareContainer<PlainContainerLogic> container = fullComp.container;
            assertRightState(ContainerStatus.READY, container);
            /*public void tearDown() : ContainerStatus.READY, ContainerStatus.DESTROYED
             -Via DestroyTransitionAction- //HERE: Check closely
             theContainer.network.stopPortUpdaters() : //HERE: might be important
            theContainer.preDestroyAction() :
                 controller.blockingStop() : stopDetector.clearSchedule(), LifecycleHandlerType.POST_START, LifecycleHandlerType.PRE_STOP, LifecycleHandlerType.PRE_STOP, LifecycleHandlerType.STOP,
                 stopDetector.waitForFinalShutdown(), LifecycleHandlerType.STOP, LifecycleHandlerType.POST_STOP //HERE: Check really closely
             theContainer.logic.doDestroy(forceShutdown) :
            theContainer.setNetworking() : //networking-stuff
            theContainer.network.publishLocalData(theContainer.containerId) : //network-stuff
            */
            container.tearDown();
            container.awaitDestruction();

            assertRightState(ContainerStatus.DESTROYED, container);

            // todo: adjust, if multiple component-instances of same component
            try {
                checkBasicRegistryValue(1, fullComp.cId);
            } catch (RegistrationException e) {
                e.printStackTrace();
            }

            assert dumb != null;

            checkForDumbElements(
              new String[] {
                "HOST_CONTAINER_IP",
                "Instance_Number",
                "HOST_PUBLIC_IP",
                "HOST_CLOUD_IP",
                "Container_Status"
              },
              DEFAULT_PROPERTIES,
              fullComp.cInstId);

            Map<String, String> cids = dumb.get(fullComp.cId);
            assert INITIAL_LOCAL_ADDRESS == cids.get("HOST_CONTAINER_IP");
            assert EnvContextWrapperRM.getPublicIp() == cids.get("HOST_PUBLIC_IP");
            assert EnvContextWrapperRM.getCloudIp() == cids.get("HOST_CLOUD_IP");
            assert "1" == cids.get("Instance_Number");
        }
    }

    @Override
    public void testPortUpdater() throws ContainerException, RemoteException  {
        for (FullComponent fullComp : fullComponents) {
            assert fullComp != null;
            ErrorAwareContainer<PlainContainerLogic> container = fullComp.container;

            LifecycleController lcc = fullComp.lcc;
            List<OutPort>  outports = fullComp.comp.getDownstreamPorts();
            //todo;  adjust, if multiple outports are available
            if(outports.size()!=1)
                continue;

            OutPort outport = outports.get(0);

            /*
            //Call lcc.blockingUpdatePorts(OutPort port, PortUpdateHandler handler, PortDiff<DownstreamAddress> diff) somewhere
            */
            fullComp.networkHandler.startPortUpdaters(lcc);

            // todo: adjust, if multiple component-instances of same component
            try {
                checkBasicRegistryValue(1, fullComp.cId);
            } catch (RegistrationException e) {
                e.printStackTrace();
            }
            //important, check when startPortUpdaters() is called here

            assert dumb != null;

            checkForDumbElements(
                    new String[] {
                            "HOST_CONTAINER_IP",
                            "Instance_Number",
                            "HOST_PUBLIC_IP",
                            "HOST_CLOUD_IP",
                            "Container_Status"
                    },
                    DEFAULT_PROPERTIES,
                    fullComp.cInstId);

            Map<String, String> cids = dumb.get(fullComp.cId);
            assert INITIAL_LOCAL_ADDRESS == cids.get("HOST_CONTAINER_IP");
            assert EnvContextWrapperRM.getPublicIp() == cids.get("HOST_PUBLIC_IP");
            assert EnvContextWrapperRM.getCloudIp() == cids.get("HOST_CLOUD_IP");
            assert "1" == cids.get("Instance_Number");
        }
    }

    private void init(AppArchitecture arch) throws ContainerException {
        dumb = null;
        fullComponents = new ArrayList<FullComponent>(arch.getComponents().size());

        for(ComponentInfo cInfo: arch.getComponents()) {
            DeployableComponentBuilder builder = DeployableComponentBuilder.createBuilder(arch.getApplicationName(), cInfo.getComponentId());

            for(InportInfo inInf: cInfo.getInportInfos())
                builder.addInport(inInf.getInportName(), inInf.getPortType(), inInf.getCardinality(), inInf.getInPort());

            for(OutportInfo outInf: cInfo.getOutportInfos())
                builder.addOutport(outInf.getOutportName(), outInf.getPuHandler(), outInf.getCardinality(), outInf.getMin());

            boolean stopIt;

            switch (cInfo.getComponentName()) {
                case "kafka":
                    builder.addLifecycleStore(createKafkaLifecycleStore());
                    //adjust
                    stopIt = false;
                    break;
                case "cassandra":
                    builder.addLifecycleStore(createCassLifecycleStore());
                    //adjust
                    stopIt = true;
                    break;
                default:
                    throw new ContainerException();
            }

            builder.deploySequentially(true);
            DeployableComponent comp = builder.build();
            GlobalRegistryAccessor accessor = new GlobalRegistryAccessor(core.ctx, comp, cInfo.getComponentInstanceId());
            final LcaRegistry reg = core.ctx.getRegistry();
            NetworkHandler networkHandler = new NetworkHandler(accessor, comp, CoreElementsRewiring.context);
            ExecutionContext exCtx = new ExecutionContext(cInfo.getOs(),null);

            fullComponents.add(new FullComponent(comp, accessor, networkHandler, comp.getComponentId(), cInfo.getComponentInstanceId(), exCtx, stopIt));
        }
    }

    /* copied from PlainContainerManager createNewContainer and adjusted*/
    private ErrorAwareContainer<PlainContainerLogic> createNewContainer(FullComponent fullComp) throws ContainerException {

        PlainShellFactory plainShellFactory = new PlainShellFactoryImpl();

        PlainContainerLogic containerLogic =
                new PlainContainerLogic(fullComp.cInstId, fullComp.comp, core.ctx, fullComp.exCtx.getOperatingSystem(), fullComp.networkHandler,
                        plainShellFactory, core.context);

        ExecutionContext executionContext = new ExecutionContext(fullComp.exCtx.getOperatingSystem(), plainShellFactory);
        LifecycleController lifecycleController =
                new LifecycleController(fullComp.comp.getLifecycleStore(), containerLogic, fullComp.accessor,
                        executionContext, core.context);

        try {
            fullComp.accessor.init(fullComp.cInstId);
        } catch (RegistrationException re) {
            throw new ContainerException("cannot start container, because registry not available",
                    re);
        }

        ErrorAwareContainer<PlainContainerLogic> controller =
                new ErrorAwareContainer<PlainContainerLogic>(fullComp.cInstId, containerLogic, fullComp.networkHandler,
                        lifecycleController, fullComp.accessor);

        //not a really good design
        fullComp.addLifecycleController(lifecycleController);
        return controller;
    }

    private LifecycleStore createKafkaLifecycleStore() {

        LifecycleStoreBuilder store = new LifecycleStoreBuilder();

        /**
         * invoked when the Lifecycle controller starts;
         * may be used for validating system environment;
         */
        //INIT(InitHandler.class, DefaultFactories.INIT_FACTORY),
        //->not supported exception -> shift part of it to pre_install

        /**
         * may be used to get service binaries, e.g.
         * by downloading
         */
        //PRE_INSTALL(PreInstallHandler.class, DefaultFactories.PRE_INSTALL_FACTORY),
        BashBasedHandlerBuilder builder_pre_inst = new BashBasedHandlerBuilder();
        builder_pre_inst.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_pre_inst.addCommand("sudo apt-get -y -q update && sudo apt-get -y -q upgrade");
        //builder_pre_inst.addCommand("sudo dpkg --configure -a");
        builder_pre_inst.addCommand("sudo apt-get -y -q install zookeeperd");
        //adjust, if needed builder_pre_inst.addCommand("sudo apt-get -y -q install default-jre");
        builder_pre_inst.addCommand("sudo /usr/share/zookeeper/bin/zkServer.sh start");
        store.setHandler(builder_pre_inst.build(LifecycleHandlerType.PRE_INSTALL), LifecycleHandlerType.PRE_INSTALL);

        /**
         * may be used to unzip and install service binaries
         *
         */
        //INSTALL(InstallHandler.class, DefaultFactories.INSTALL_FACTORY),
        BashBasedHandlerBuilder builder_inst = new BashBasedHandlerBuilder();
        builder_inst.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_inst.addCommand("mkdir -p /home/ubuntu/Downloads");
        //adjust
        builder_inst.addCommand(
        "wget \"https://archive.apache.org/dist/kafka/1.0.1/kafka_2.11-1.0.1.tgz\" -O /home/ubuntu/Downloads/kafka.tgz");
        builder_inst.addCommand("mkdir -p /home/ubuntu/kafka");
        builder_inst.addCommand("sudo tar -xvzf /home/ubuntu/Downloads/kafka.tgz --strip 1 -C /home/ubuntu/kafka");
        store.setHandler(builder_inst.build(LifecycleHandlerType.INSTALL), LifecycleHandlerType.INSTALL);

        /*
         * may be used to adapt configuration files
         * according to environment
         */
        //POST_INSTALL(PostInstallHandler.class, DefaultFactories.POST_INSTALL_FACTORY),
        BashBasedHandlerBuilder builder_post_inst = new BashBasedHandlerBuilder();
        builder_post_inst.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_post_inst.addCommand(
        "printf \"\\ndelete.topic.enable = true\" >> /home/ubuntu/kafka/config/server.properties");
        builder_post_inst.addCommand("printf \"\\n127.0.1.1 testvm\" | sudo tee -a /etc/hosts");
        store.setHandler(builder_post_inst.build(LifecycleHandlerType.POST_INSTALL), LifecycleHandlerType.POST_INSTALL);

        /**
         * may be used for checking that required operating system
         * files are available, like files, disk space, and port
         */
        //PRE_START(PreStartHandler.class, DefaultFactories.PRE_START_FACTORY),
        /**
         * starts the component instance
         */
        //START(StartHandler.class, DefaultFactories.START_FACTORY),
        BashBasedHandlerBuilder builder_start = new BashBasedHandlerBuilder();
        builder_start.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        //adjust
        builder_start.addCommand("sudo nohup ~/kafka/bin/kafka-server-start.sh ~/kafka/config/server.properties > ~/kafka/kafka.log 2>&1 &");
        BashBasedHandlerBuilder builder_start_det = new BashBasedHandlerBuilder();
        builder_start_det.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        store.setStartDetector(DefaultHandlers.DEFAULT_START_DETECTOR);
        store.setHandler(builder_start.build(LifecycleHandlerType.START), LifecycleHandlerType.START);

        /**
         * may be used to register service instances with a load balancer
         */
        //POST_START(PostStartHandler.class, DefaultFactories.POST_START_FACTORY),
        /**
         * may be used to unregister service instance at the load balancer
         */
        //PRE_STOP(PreStopHandler.class, DefaultFactories.PRE_STOP_FACTORY),
        /**
         * may be used to add manual stop logic
         */
        //STOP(StopHandler.class, DefaultFactories.STOP_FACTORY),
        /**
         * may be used to release external resources
         */
        //POST_STOP(PostStopHandler.class, DefaultFactories.POST_STOP_FACTORY),

        return store.build();
    }

    private LifecycleStore createCassLifecycleStore(){

        LifecycleStoreBuilder store = new LifecycleStoreBuilder();

        /**
         * invoked when the Lifecycle controller starts;
         * may be used for validating system environment;
         */
        //INIT(InitHandler.class, DefaultFactories.INIT_FACTORY),
        //->not supported exception -> shift it to pre_install
        /*BashBasedHandlerBuilder builder_init = new BashBasedHandlerBuilder();
        builder_init.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_init.addCommand("add-apt-repository ppa:webupd8team/java");
        builder_init.addCommand("apt-get -y -q update");
        builder_init.addCommand("apt-get -y -q install oracle-java8-set-default");
        store.setHandler(builder_init.build(LifecycleHandlerType.INIT), LifecycleHandlerType.INIT);*/

        /**
         * may be used to get service binaries, e.g.
         * by downloading
         */
        //PRE_INSTALL(PreInstallHandler.class, DefaultFactories.PRE_INSTALL_FACTORY),
        BashBasedHandlerBuilder builder_pre_inst = new BashBasedHandlerBuilder();
        builder_pre_inst.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_pre_inst.addCommand("sudo apt-get -y -q update && sudo apt-get -y -q upgrade");
        builder_pre_inst.addCommand("sudo add-apt-repository -y ppa:webupd8team/java");
        builder_pre_inst.addCommand("sudo apt-get -y -q update");
        builder_pre_inst.addCommand("echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections");
        builder_pre_inst.addCommand("echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections");
        builder_pre_inst.addCommand("sudo apt-get -y -q install oracle-java8-set-default");
        builder_pre_inst.addCommand("curl -L http://debian.datastax.com/debian/repo_key | sudo apt-key add -");
        builder_pre_inst.addCommand(
        "echo \"deb http://debian.datastax.com/community stable main\" | sudo tee -a /etc/apt/sources.list.d/cassandra.sources.list");
        builder_pre_inst.addCommand("sudo apt-get -y -q update");
        store.setHandler(builder_pre_inst.build(LifecycleHandlerType.PRE_INSTALL), LifecycleHandlerType.PRE_INSTALL);

        /**
         * may be used to unzip and install service binaries
         *
         */
        //INSTALL(InstallHandler.class, DefaultFactories.INSTALL_FACTORY),
        BashBasedHandlerBuilder builder_inst = new BashBasedHandlerBuilder();
        builder_inst.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        //adjust
        //builder_inst.addCommand("sudo apt-get -y -q install dsc22=2.2.3-1 cassandra=2.2.3");
        builder_inst.addCommand("sudo apt-get -y -q install cassandra=3.0.0");
        store.setHandler(builder_inst.build(LifecycleHandlerType.INSTALL), LifecycleHandlerType.INSTALL);

        /*
         * may be used to adapt configuration files
         * according to environment
         */
        //POST_INSTALL(PostInstallHandler.class, DefaultFactories.POST_INSTALL_FACTORY),
        /**
         * may be used for checking that required operating system
         * files are available, like files, disk space, and port
         */
        //PRE_START(PreStartHandler.class, DefaultFactories.PRE_START_FACTORY),
        /**
         * starts the component instance
         */
        //START(StartHandler.class, DefaultFactories.START_FACTORY),
        BashBasedHandlerBuilder builder_start = new BashBasedHandlerBuilder();
        builder_start.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_start.addCommand("sudo chmod 750 /var/run/cassandra");
        builder_start.addCommand("sudo service cassandra start");
        store.setStartDetector(DefaultHandlers.DEFAULT_START_DETECTOR);
        store.setHandler(builder_start.build(LifecycleHandlerType.START), LifecycleHandlerType.START);
        /**
         * may be used to register service instances with a load balancer
         */
        //POST_START(PostStartHandler.class, DefaultFactories.POST_START_FACTORY),
        /**
         * may be used to unregister service instance at the load balancer
         */
        //PRE_STOP(PreStopHandler.class, DefaultFactories.PRE_STOP_FACTORY),
        BashBasedHandlerBuilder builder_pre_stop = new BashBasedHandlerBuilder();
        builder_pre_stop.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_pre_stop.addCommand("sleep 10s");
        store.setHandler(builder_pre_stop.build(LifecycleHandlerType.PRE_STOP), LifecycleHandlerType.PRE_STOP);

        /**
         * may be used to add manual stop logic
         */
        //STOP(StopHandler.class, DefaultFactories.STOP_FACTORY),
        BashBasedHandlerBuilder builder_stop = new BashBasedHandlerBuilder();
        builder_stop.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_stop.addCommand("sudo service cassandra stop");
        store.setHandler(builder_stop.build(LifecycleHandlerType.STOP), LifecycleHandlerType.STOP);

        /**
         * may be used to release external resources
         */
        //POST_STOP(PostStopHandler.class, DefaultFactories.POST_STOP_FACTORY),

        return store.build();
    }

    private void checkForDumbElements(String[] values, int expectedSize, ComponentInstanceId cId) {
        Map<String,String> cids = dumb.get(cId);
        assert cids != null;
        for(String s : values) {
            assert cids.containsKey("unknown key: " + s);
        }
        assert cids.size() == expectedSize;
    }

    private static void assertRightState(ContainerStatus stat, ErrorAwareContainer<PlainContainerLogic> container) {
        for(ContainerStatus status : ContainerStatus.values()) {
            if(status == stat)
                assert status == container.getState();
            else
                assert status != container.getState();
        }
    }

    private void checkBasicRegistryValue(int compInstances, ComponentId cId) throws RegistrationException {
        dumb = core.checkBasicRegistryValues(cId);
        assert dumb.size() == compInstances;
    }

    @Override
    public void stop() throws RemoteException {
        TestBooter.unregister(this);
    }

    @Override
    public void terminate() throws RemoteException {
        TestBooter.unregister(this);
        try {
            core.context.close();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    static class FullComponent {

        FullComponent(DeployableComponent comp, GlobalRegistryAccessor accessor, NetworkHandler networkHandler, ComponentId cId, ComponentInstanceId cInstId, ExecutionContext exCtx, boolean stopIt, ErrorAwareContainer<PlainContainerLogic> container, LifecycleController lcc) {
            this.comp = comp;
            this.accessor = accessor;
            this.networkHandler = networkHandler;
            this.cId = cId;
            this.cInstId = cInstId;
            this.exCtx = exCtx;
            this.stopIt = stopIt;
            this.container = container;
            this.lcc = lcc;
        }

        FullComponent(DeployableComponent comp, GlobalRegistryAccessor accessor, NetworkHandler networkHandler, ComponentId cId, ComponentInstanceId cInstId, ExecutionContext exCtx, boolean stopIt) {
            this(comp, accessor, networkHandler, cId, cInstId, exCtx, stopIt,null, null);
        }

        public void addContainer(ErrorAwareContainer<PlainContainerLogic> container) {
            this.container = container;
        }

        public void addLifecycleController(LifecycleController lcc) {
            this.lcc = lcc;
        }

        public DeployableComponent comp;
        public GlobalRegistryAccessor accessor;
        public NetworkHandler networkHandler;
        public ComponentId cId;
        public ComponentInstanceId cInstId;
        public ExecutionContext exCtx;
        public boolean stopIt;
        public ErrorAwareContainer<PlainContainerLogic> container;
        public LifecycleController lcc;
    }
}
