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

package de.uniulm.omi.cloudiator.lance.client;

import static de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus.*;
import static java.lang.Thread.sleep;

import java.util.ArrayList;
import java.util.List;

import de.uniulm.omi.cloudiator.lance.application.component.*;
import de.uniulm.omi.cloudiator.lance.lca.DeploymentException;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerStatus;
import de.uniulm.omi.cloudiator.lance.lca.registry.RegistrationException;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStoreBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.bash.BashBasedHandlerBuilder;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortLinkage;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerType;
import java.util.concurrent.Callable;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.lang.Exception;

//Important: Java-re, docker daemon and Lance(Server) must be installed on the VM and related ports must be opened on the vm

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientRMTest {

    private static ApplicationInstanceId appInstanceId;
    private static ApplicationId applicationId;
    private static String zookeeperComponent;
    private static String zookeeperInportName;
    private static String zookeeperInternalInportName;
    private static String zookeeperOutportName;
    private static ComponentId zookeeperComponentId;
    private static int defaultzookeeperInport;
    private static int defaultZookeeperInternalInport;
    private static String cassandraComponent;
    private static String cassandraInportName;
    private static String cassandraInternalInportName;
    private static String cassandraOutportName;
    private static ComponentId cassandraComponentId;
    private static int defaultcassandraInport;
    private static int defaultCassandraInternalInport;
    private static String kafkaComponent;
    private static String kafkaInportName;
    private static String kafkaInternalInportName;
    private static String kafkaOutportName;
    private static int defaultkafkaInport;
    private static int defaultKafkaInternalInport;
    private static ComponentId kafkaComponentId;
    private static int defaultInternalInport;
    //adjust
    private static String publicIp = "x.x.x.x";
    private static LifecycleClient client;
    private static ComponentInstanceId zookId, cassId,kafkId;

    @BeforeClass
    public static void configureAppContext() {
        //führe installer aus
        appInstanceId =  ApplicationInstanceId.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b");
        applicationId = ApplicationId.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389c");
        zookeeperComponent = "zookeeper";
        zookeeperInportName = "ZOOK_INP";
        zookeeperInternalInportName = "ZOOK_INT_INP";
        zookeeperOutportName = "ZOOK_OUT";
        zookeeperComponentId = ComponentId.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389d");
        defaultzookeeperInport = defaultZookeeperInternalInport = 3888;
        cassandraComponent = "cassandra";
        cassandraInportName = "CASS_INP";
        cassandraInternalInportName = "CASS_INT_INP";
        cassandraOutportName = "CASS_OUT";
        cassandraComponentId = ComponentId.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389e");
        defaultcassandraInport = defaultCassandraInternalInport = 9160;
        kafkaComponent = "kafka";
        kafkaInportName = "KAFK_INP";
        kafkaInternalInportName = "KAFK_INT_INP";
        kafkaOutportName = "KAFK_OUT";
        kafkaComponentId = ComponentId.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389f");
        defaultkafkaInport = defaultKafkaInternalInport = 9092;
        defaultInternalInport = 7;

        System.setProperty("lca.client.config.registry", "etcdregistry");
        //adjust
        System.setProperty("lca.client.config.registry.etcd.hosts", "x.x.x.x:4001");
    }

    /*private DeployableComponent buildDeployableComponent(LifecycleClient client, String compName, ComponentId id, List<InportInfo> inInfs, List<OutportInfo> outInfs, Callable<LifecycleStore> createLifeCycleStore) {
        DeployableComponentBuilder builder = DeployableComponentBuilder.createBuilder(compName, id);

        for(int i=0; i<inInfs.size(); i++)
            builder.addInport(inInfs.get(i).inportName, inInfs.get(i).portType, inInfs.get(i).cardinality, inInfs.get(i).inPort);

        for(int i=0; i<outInfs.size(); i++)
            builder.addOutport(outInfs.get(i).outportName, outInfs.get(i).puHandler, outInfs.get(i).cardinality, outInfs.get(i).min);

        try {
            builder.addLifecycleStore(createLifeCycleStore.call());
        } catch (Exception ex) {
            System.err.println("Server not reachable");
        }
        builder.deploySequentially(true);
        return builder.build();
    }

    class InportInfo {
        public final String inportName;
        public final PortProperties.PortType portType;
        public final int cardinality;
        public final int inPort;

        InportInfo(String inportName, PortProperties.PortType portType, int cardinality, int inPort) {
            this.inportName = inportName;
            this.portType = portType;
            this.cardinality = cardinality;
            this.inPort = inPort;
        }
    }

    class OutportInfo {
        public final String outportName;
        public final PortUpdateHandler puHandler;
        public final int cardinality;
        public final int min;

        OutportInfo(String outportName, PortUpdateHandler puHandler, int cardinality, int min) {
            this.outportName = outportName;
            this.puHandler = puHandler;
            this.cardinality = cardinality;
            this.min = min;
        }
    }

    private LifecycleStore createZookeeperLifecycleStore(){
        LifecycleStoreBuilder store = new LifecycleStoreBuilder();
        // pre-install handler //
        BashBasedHandlerBuilder builder_pre = new BashBasedHandlerBuilder();
        builder_pre.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_pre.addCommand("apt-get -y -q update");
        builder_pre.addCommand("apt-get -y -q upgrade");
        builder_pre.addCommand("export STARTED=\"true\"");
        store.setStartDetector(builder_pre.buildStartDetector());
        // add other commands
        store.setHandler(builder_pre.build(LifecycleHandlerType.PRE_INSTALL), LifecycleHandlerType.PRE_INSTALL);
        // weitere handler? //
        return store.build();
    }

    private LifecycleStore createCassandraLifecycleStore(){
        LifecycleStoreBuilder store = new LifecycleStoreBuilder();
        // pre-install handler //
        BashBasedHandlerBuilder builder_pre = new BashBasedHandlerBuilder();
        builder_pre.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_pre.addCommand("apt-get -y -q update");
        builder_pre.addCommand("apt-get -y -q upgrade");
        builder_pre.addCommand("export STARTED=\"true\"");
        store.setStartDetector(builder_pre.buildStartDetector());
        // add other commands
        store.setHandler(builder_pre.build(LifecycleHandlerType.PRE_INSTALL), LifecycleHandlerType.PRE_INSTALL);
        // weitere handler? //
        return store.build();
    }

    private LifecycleStore createKafkaLifecycleStore(){
        LifecycleStoreBuilder store = new LifecycleStoreBuilder();
        // pre-install handler //
        BashBasedHandlerBuilder builder_pre = new BashBasedHandlerBuilder();
        builder_pre.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
        builder_pre.addCommand("apt-get -y -q update");
        builder_pre.addCommand("apt-get -y -q upgrade");
        builder_pre.addCommand("export STARTED=\"true\"");
        store.setStartDetector(builder_pre.buildStartDetector());
        // add other commands
        store.setHandler(builder_pre.build(LifecycleHandlerType.PRE_INSTALL), LifecycleHandlerType.PRE_INSTALL);
        // weitere handler? //
        return store.build();
    }

    @Test
    public void testAClientGetter() {
        try {
            client = LifecycleClient.getClient(publicIp);
        } catch (RemoteException ex) {
            System.err.println("Server not reachable");
        } catch (NotBoundException ex) {
            System.err.println("Socket not bound");
        }
    }

    @Test
    public void testBRegister() {
        try {
            client.registerApplicationInstance(appInstanceId, applicationId);
            client.registerComponentForApplicationInstance(appInstanceId, zookeeperComponentId);
            client.registerComponentForApplicationInstance(appInstanceId, cassandraComponentId);
            client.registerComponentForApplicationInstance(appInstanceId, kafkaComponentId);
        } catch (RegistrationException ex) {
            System.err.println("Exception during registration");
        }
    }

    @Test
    public void testCZookCompDescriptions() {
        List<InportInfo> inInfs = new ArrayList<>();
        InportInfo inInf = new InportInfo(zookeeperInternalInportName, PortProperties.PortType.INTERNAL_PORT, 2,3888);
        inInfs.add(inInf);
        List<OutportInfo> outInfs = new ArrayList<>();
    OutportInfo outInf =
        new OutportInfo(
            zookeeperOutportName,
            DeploymentHelper.getEmptyPortUpdateHandler(),
            1,
            OutPort.NO_SINKS);
        outInfs.add(outInf);
        buildDeployableComponent(client, zookeeperComponent, zookeeperComponentId, inInfs, outInfs, new Callable<LifecycleStore>() {
            public LifecycleStore call() {
                                       return createZookeeperLifecycleStore();
                                                                              }
        });
    }

    @Test
    public void testDCassCompDescriptions() {
        List<InportInfo> inInfs = new ArrayList<>();
        InportInfo inInf = new InportInfo(cassandraInternalInportName, PortProperties.PortType.INTERNAL_PORT,2,9160);
        inInfs.add(inInf);
        List<OutportInfo> outInfs = new ArrayList<>();
        OutportInfo outInf = new OutportInfo(cassandraOutportName, DeploymentHelper.getEmptyPortUpdateHandler(),1, OutPort.NO_SINKS);
        outInfs.add(outInf);
        buildDeployableComponent(client, cassandraComponent, cassandraComponentId, inInfs, outInfs, new Callable<LifecycleStore>() {
            public LifecycleStore call() {
                                       return createCassandraLifecycleStore();
                                                                              }
        });
    }

    @Test
    public void testEKafkaCompDescriptions() {
        List<InportInfo> inInfs = new ArrayList<>();
        InportInfo inInf = new InportInfo(kafkaInportName, PortProperties.PortType.PUBLIC_PORT,1,9092);
        inInfs.add(inInf);
        List<OutportInfo> outInfs = new ArrayList<>();
        OutportInfo outInf = new OutportInfo(kafkaOutportName, DeploymentHelper.getEmptyPortUpdateHandler(),2, OutPort.NO_SINKS);
        outInfs.add(outInf);
        buildDeployableComponent(client, kafkaComponent, kafkaComponentId, inInfs, outInfs, new Callable<LifecycleStore>() {
            public LifecycleStore call() {
                                       return createKafkaLifecycleStore();
                                                                          }
        });
    }

    public DeploymentContext createZookeperContext(LifecycleClient client) {
        DeploymentContext zookeeper_context = client.initDeploymentContext(applicationId, appInstanceId);
        // saying that we want to use the default port as the actual port number //
        zookeeper_context.setProperty(zookeeperInternalInportName, (Object)defaultZookeeperInternalInport, InPort.class);
        // saying that we wire this outgoing port to the incoming ports of CASSANDRA //
        zookeeper_context.setProperty(zookeeperOutportName, (Object)new PortReference(cassandraComponentId, cassandraInternalInportName, PortLinkage.ALL), OutPort.class);
        return zookeeper_context;
    }

    public DeploymentContext createCassandraContext(LifecycleClient client) {
        DeploymentContext cassandra_context = client.initDeploymentContext(applicationId, appInstanceId);
        // saying that we want to use the default port as the actual port number //
        cassandra_context.setProperty(cassandraInternalInportName, (Object)defaultCassandraInternalInport, InPort.class);
        // saying that we wire this outgoing port to the incoming ports of ZOOKEEPER //
        cassandra_context.setProperty(cassandraOutportName, (Object)new PortReference(zookeeperComponentId, zookeeperInternalInportName, PortLinkage.ALL), OutPort.class);
        return cassandra_context;
    }

    public DeploymentContext createKafkaContext(LifecycleClient client) {
        DeploymentContext kafka_context = client.initDeploymentContext(applicationId, appInstanceId);
        // saying that we want to use the default port as the actual port number //
        kafka_context.setProperty(kafkaInportName, (Object)defaultkafkaInport, InPort.class);
        // saying that we wire this outgoing port to the incoming ports of ZOOKEEPER //
        kafka_context.setProperty(kafkaOutportName, (Object)new PortReference(zookeeperComponentId, zookeeperInternalInportName, PortLinkage.ALL), OutPort.class);
        // saying that we wire this outgoing port to the incoming ports of CASSANDRA //
        kafka_context.setProperty(kafkaOutportName, (Object)new PortReference(cassandraComponentId, cassandraInternalInportName, PortLinkage.ALL), OutPort.class);
        return kafka_context;
    }

    @Test
    public void testFZookeeperDeploymentContext() {
        createZookeperContext(client);
    }

    @Test
    public void testGCassandraDeploymentContext() {
        createCassandraContext(client);
    }

    @Test
    public void testHKafkaDeploymentContext() {
        createKafkaContext(client);
    }

    @Test
    public void testIZookDeploy() {
        try {
            List<InportInfo> inInfs = new ArrayList<>();
            InportInfo inInf = new InportInfo(zookeeperInternalInportName, PortProperties.PortType.INTERNAL_PORT,1,3888);
            inInfs.add(inInf);
            List<OutportInfo> outInfs = new ArrayList<>();
            OutportInfo outInf = new OutportInfo(zookeeperOutportName, DeploymentHelper.getEmptyPortUpdateHandler(),1, OutPort.NO_SINKS);
            outInfs.add(outInf);
            DeployableComponent zookComp = buildDeployableComponent(client, zookeeperComponent, zookeeperComponentId, inInfs, outInfs, new Callable<LifecycleStore>() {
                public LifecycleStore call() {
                    return createZookeeperLifecycleStore();
                }
            });
            DeploymentContext zookContext = createZookeperContext(client);
            zookId = client.deploy(zookContext, zookComp, OperatingSystem.UBUNTU_14_04, ContainerType.DOCKER);
        } catch (DeploymentException ex) {
            System.err.println("Couldn't deploy zookeeper component");
        }
    }

    @Test
    public void testJCassDeploy() {
        try {
            List<InportInfo> inInfs = new ArrayList<>();
            InportInfo inInf = new InportInfo(cassandraInternalInportName, PortProperties.PortType.INTERNAL_PORT,1,9160);
            inInfs.add(inInf);
            List<OutportInfo> outInfs = new ArrayList<>();
            OutportInfo outInf = new OutportInfo(cassandraOutportName, DeploymentHelper.getEmptyPortUpdateHandler(),1, OutPort.NO_SINKS);
            outInfs.add(outInf);
            DeployableComponent cassComp = buildDeployableComponent(client, cassandraComponent, cassandraComponentId, inInfs, outInfs, new Callable<LifecycleStore>() {
                public LifecycleStore call() {
                    return createCassandraLifecycleStore();
                }
            });
            DeploymentContext cassContext = createCassandraContext(client);
            cassId = client.deploy(cassContext, cassComp, OperatingSystem.UBUNTU_14_04, ContainerType.DOCKER);
        } catch (DeploymentException ex) {
            System.err.println("Couldn't deploy cassandra component");
        }
    }

    @Test
    public void testKKafkDeploy() {
        try {
            List<InportInfo> inInfs = new ArrayList<>();
            InportInfo inInf = new InportInfo(kafkaInportName, PortProperties.PortType.PUBLIC_PORT,1,9092);
            inInfs.add(inInf);
            List<OutportInfo> outInfs = new ArrayList<>();
            OutportInfo outInf = new OutportInfo(kafkaOutportName, DeploymentHelper.getEmptyPortUpdateHandler(),2, OutPort.NO_SINKS);
            outInfs.add(outInf);
            DeployableComponent kafkaComp = buildDeployableComponent(client, kafkaComponent, kafkaComponentId, inInfs, outInfs, new Callable<LifecycleStore>() {
                public LifecycleStore call() {
                    return createKafkaLifecycleStore();
                }
            });
            DeploymentContext kafkaContext = createKafkaContext(client);
            kafkId = client.deploy(kafkaContext, kafkaComp, OperatingSystem.UBUNTU_14_04, ContainerType.DOCKER);
        } catch (DeploymentException ex) {
        System.err.println("Couldn't deploy cassandra component");
        }
    }

    @Test
    public void testLComponentStatus() {
        ContainerStatus zookStatus, cassStatus, kafkStatus;
        zookStatus = cassStatus = kafkStatus = UNKNOWN;
        do{
            try{
                zookStatus = client.getComponentContainerStatus(zookId, publicIp);
                cassStatus = client.getComponentContainerStatus(cassId, publicIp);
                kafkStatus = client.getComponentContainerStatus(kafkId, publicIp);
                System.out.println("ZOOKEEPER STATUS:" + zookStatus);
                System.out.println("CASSANDRA STATUS:" + cassStatus);
                System.out.println("KAFKA STATUS:" + kafkStatus);
                sleep(5000);
            } catch(DeploymentException ex) {
                System.err.println("Exception during deployment!");
            } catch(InterruptedException ex) {
                System.err.println("Interrupted!");
            }
        } while(zookStatus != READY || cassStatus != READY || kafkStatus != READY);
    }*/
    @Test
    public void testSuccess() {}
}
