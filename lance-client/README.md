Use of LifecycleClient
-----------------------

### Step I: Get Instance of Client

```
LifecycleClient client = LifecycleClient.getClient();
```

### Step II: Register Application Instance and Components

```
client.registerApplicationInstance(MY_INSTANCE_ID, LSY_APP_ID);
client.registerComponentForApplicationInstance(MY_INSTANCE_ID, ZookeeperComponent.ZOOKEEPER_COMPONENT_ID);
client.registerComponentForApplicationInstance(MY_INSTANCE_ID, CassandraComponent.CASSANDRA_COMPONENT_ID);
client.registerComponentForApplicationInstance(MY_INSTANCE_ID, KafkaComponent.KAFKA_COMPONENT_ID);
```

Note that MY_INSTANCE_ID is of type ApplicationInstanceId and LSY_APP_ID is of type ApplicationId. *Component.X_COMPONENT_ID are of type ComponentId.
The fact that these components are part of the particular application should be stored in some other registry and only be copied to the deployment
registry (via the client) so that the correct branches of components are being created.

### Step III: Create Component Descriptions (if not available)

```
DeployableComponentBuilder builder = DeployableComponentBuilder.createBuilder(CASSANDRA_COMPONENT_NAME, CASSANDRA_COMPONENT_ID);
builder.addInport(CASSANDRA_IN_PORT_NAME, PortType.INTERNAL_PORT, PortProperties.INFINITE_CARDINALITY, CASSANDRA_DEFAULT_INPORT);
builder.addInport(CASSANDRA_INTERNAL_IN_PORT_NAME, PortType.CLUSTER_PORT, PortProperties.INFINITE_CARDINALITY, CASSANDRA_DEFAULT_INTERNAL_INPORT);
builder.addOutport(CASSANDRA_INTERNAL_OUT_PORT_NAME, DeploymentHelper.getEmptyPortUpdateHandler(), PortProperties.INFINITE_CARDINALITY);
builder.addLifecycleStore(createCassandraLifecycleStore());
builder.deploySequentially(true);
return builder.build();
```

this includes creating Lifecycle Handlers:
```
private static LifecycleStore createCassandraLifecycleStore(){
	LifecycleStoreBuilder store = new LifecycleStoreBuilder();
	// pre-install handler //
	BashBasedHandlerBuilder builder = new BashBasedHandlerBuilder();
	builder.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
	builder.addCommand("apt-get -y -q update");
	builder.addCommand("apt-get -y -q upgrade");
	// add other commands 
	store.setHandler(builder.build(LifecycleHandlerType.PRE_INSTALL), LifecycleHandlerType.PRE_INSTALL);
	return store.build();
}
```

### Step IV:  Create Deployment Context (wiring)

```
private static DeploymentContext createCassandraContext(LifecycleClient client) {
		DeploymentContext cassandra_context = client.initDeploymentContext(LSY_APP_ID, MY_INSTANCE_ID);
		// saying that we want to use the default port as the actual port number // 
		cassandra_context.setProperty(CassandraComponent.CASSANDRA_IN_PORT_NAME, CassandraComponent.CASSANDRA_DEFAULT_INPORT, InPort.class);
		// same here // 
		cassandra_context.setProperty(CassandraComponent.CASSANDRA_INTERNAL_IN_PORT_NAME, CassandraComponent.CASSANDRA_DEFAULT_INTERNAL_INPORT, InPort.class);
		// saying that we wire this outgoing port to the incoming ports of CASSANDRA (ourselves) //
		cassandra_context.setProperty(CassandraComponent.CASSANDRA_INTERNAL_OUT_PORT_NAME, new PortReference(CassandraComponent.CASSANDRA_COMPONENT_ID, CassandraComponent.CASSANDRA_INTERNAL_IN_PORT_NAME, PortLinkage.ALL), OutPort.class);
		return cassandra_context;
	}
```

### Step IV:  Create Deployment Context (wiring)

Deploy to a server with IP address SERVER_IP. Note that this matches to the public IP address of the virtual machine. The lifecycle agent 
will listen on port 33034. This needs to be openend in OpenStack. 

```
LifecycleClient client = ...

client.deploy(SERVER_IP, cassandra_context, cassandra, OperatingSystem.UBUNTU_14_04);
```

### Step YX: Shortcommings and Known Limitations

- Hierarchical component models are not supported