package de.uniulm.omi.cloudiator.lance.client;

import de.uniulm.omi.cloudiator.domain.OperatingSystem;
import de.uniulm.omi.cloudiator.domain.OperatingSystemArchitecture;
import de.uniulm.omi.cloudiator.domain.OperatingSystemFamily;
import de.uniulm.omi.cloudiator.domain.OperatingSystemImpl;
import de.uniulm.omi.cloudiator.domain.OperatingSystemVersions;
import de.uniulm.omi.cloudiator.lance.application.ApplicationId;
import de.uniulm.omi.cloudiator.lance.application.ApplicationInstanceId;
import de.uniulm.omi.cloudiator.lance.application.DeploymentContext;
import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.application.component.InPort;
import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;
import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleHandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStoreBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.bash.BashBasedHandlerBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import de.uniulm.omi.cloudiator.lance.lifecycle.handlers.DefaultHandlers;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

public class TestUtils {


  public enum version { DOCKER, LIFECYCLE };

  public static ApplicationId applicationId;
  public static ApplicationInstanceId appInstanceId;
  public static String zookeeperComponentDocker, zookeeperComponentDocker_lifecycle, kafkaComponentPlain_lifecycle;
  public static String zookeeperInternalInportNameDocker, zookeeperInternalInportNameDocker_lifecycle, kafkaInternalInportNamePlain_lifecycle;
  public static String zookeeperOutportNameDocker, zookeeperOutportNameDocker_lifecycle, kafkaOutportNamePlain_lifecycle;
  public static String imageName, tag;
  public static ComponentId zookeeperComponentIdDocker, zookeeperComponentIdDocker_lifecycle, kafkaComponentIdPlain_lifecycle;
  public static int defaultZookeeperInternalInportDocker, defaultZookeeperInternalInportDocker_lifecycle, defaultKafkaInternalInportPlain_lifecycle;
  public static ComponentInstanceId zookIdDocker, zookIdDocker_lifecycle, kafkaIdPlain_lifecycle;
  // adjust
  public static String publicIp = "x.x.x.x";
  public static LifecycleClient client;

  static {
    applicationId = new ApplicationId();
    appInstanceId = new ApplicationInstanceId();

    Random rand = new Random();
    zookeeperComponentDocker = "zookeeper";
    zookeeperComponentDocker_lifecycle = "zookeeper_lifecycle";
    kafkaComponentPlain_lifecycle = "kafka";
    zookeeperInternalInportNameDocker = "ZOOK_INT_INP";
    zookeeperInternalInportNameDocker_lifecycle = "ZOOK_INT_INP_LIFECYCLE";
    kafkaInternalInportNamePlain_lifecycle = "KAFKA_INT_INP_LIFECYCLE";
    zookeeperOutportNameDocker = "ZOOK_OUT";
    zookeeperOutportNameDocker_lifecycle = "ZOOK_OUT_LIFECYCLE";
    kafkaOutportNamePlain_lifecycle = "KAFKA_OUT_LIFECYCLE";
    imageName = "zookeeper";
    tag = "latest";
    zookeeperComponentIdDocker = new ComponentId();
    zookeeperComponentIdDocker_lifecycle = new ComponentId();
    kafkaComponentIdPlain_lifecycle = new ComponentId();
    defaultZookeeperInternalInportDocker = 3888;
    defaultZookeeperInternalInportDocker_lifecycle = (rand.nextInt(65563) + 1);
    defaultKafkaInternalInportPlain_lifecycle = (rand.nextInt(65563) + 1);

    System.setProperty("lca.client.config.registry", "etcdregistry");
    // adjust
    System.setProperty("lca.client.config.registry.etcd.hosts", "x.x.x.x:4001");
  }

  public static DockerComponent.Builder buildDockerComponentBuilder(
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      String imageFolder,
      String tag) {
    DockerComponent.Builder builder = new DockerComponent.Builder(buildEntireDockerCommands(), imageName);
    builder.name(compName);
    builder.imageFolder(imageFolder);
    builder.tag(tag);
    builder.myId(id);

    for (int i = 0; i < inInfs.size(); i++)
      builder.addInport(
          inInfs.get(i).inportName,
          inInfs.get(i).portType,
          inInfs.get(i).cardinality,
          inInfs.get(i).inPort);


    for (int i = 0; i < outInfs.size(); i++)
      builder.addOutport(
          outInfs.get(i).outportName,
          outInfs.get(i).puHandler,
          outInfs.get(i).cardinality,
          outInfs.get(i).min);

    builder.deploySequentially(true);
    return builder;
  }

  public static List<InportInfo> getInPortInfos(String internalInportName) {
    List<InportInfo> inInfs = new ArrayList<>();
    InportInfo inInf =
        new InportInfo(internalInportName, PortProperties.PortType.INTERNAL_PORT, 2, 3888);
    inInfs.add(inInf);

    return inInfs;
  }

  public static List<OutportInfo> getOutPortInfos(String outPortName) {
    List<OutportInfo> outInfs = new ArrayList<>();
    OutportInfo outInf =
        new OutportInfo(
            outPortName,
            DeploymentHelper.getEmptyPortUpdateHandler(),
            1,
            OutPort.NO_SINKS);
    outInfs.add(outInf);

    return outInfs;
  }

  public static DeploymentContext createContext (String internalInportName, int defaultInternalInport) {
    DeploymentContext context =
        client.initDeploymentContext(applicationId, appInstanceId);
    // saying that we want to use the default port as the actual port number //
    context.setProperty(
        internalInportName ,(Object) defaultInternalInport, InPort.class);

    return context;
  }

  public static DockerComponent buildDockerComponent(
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      String tag) {

    DockerComponent.Builder builder = buildDockerComponentBuilder(compName, id, inInfs, outInfs,"", tag);
    DockerComponent comp = builder.build();
    return comp;
  }

  public static DeployableComponent buildDeployableComponent(
      String compName,
      ComponentId id,
      List<InportInfo> inInfs,
      List<OutportInfo> outInfs,
      Callable<LifecycleStore> createLifeCycleStore) {

    DeployableComponent.Builder builder = DeployableComponent.Builder.createBuilder(compName,id);

    for (int i = 0; i < inInfs.size(); i++)
      builder.addInport(
          inInfs.get(i).inportName,
          inInfs.get(i).portType,
          inInfs.get(i).cardinality,
          inInfs.get(i).inPort);

    for (int i = 0; i < outInfs.size(); i++)
      builder.addOutport(
          outInfs.get(i).outportName,
          outInfs.get(i).puHandler,
          outInfs.get(i).cardinality,
          outInfs.get(i).min);

    try {
      builder.addLifecycleStore(createLifeCycleStore.call());
    } catch (Exception ex) {
      System.err.println("Server not reachable");
    }

    builder.deploySequentially(true);
    DeployableComponent comp = builder.build();
    return comp;
  }

  static class InportInfo {
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

  static class OutportInfo {
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

  public static LifecycleStore createDefaultLifecycleStore() {
    LifecycleStoreBuilder store = new LifecycleStoreBuilder();
    // pre-install handler //
    BashBasedHandlerBuilder builder_pre = new BashBasedHandlerBuilder();
    // TODO: Extend possible OSes, e.g. alpine (openjdk:8-jre)
    builder_pre.setOperatingSystem(
        new OperatingSystemImpl(
            OperatingSystemFamily.UBUNTU,
            OperatingSystemArchitecture.AMD64,
            OperatingSystemVersions.of(1604,null)));
    builder_pre.addCommand("apt-get -y -q update");
    builder_pre.addCommand("apt-get -y -q upgrade");
    builder_pre.addCommand("export STARTED=\"true\"");
    store.setStartDetector(builder_pre.buildStartDetector());
    // add other commands
    store.setHandler(
        builder_pre.build(LifecycleHandlerType.PRE_INSTALL), LifecycleHandlerType.PRE_INSTALL);
    // weitere handler? //
    return store.build();
  }

  public static LifecycleStore createKafkaLifecycleStore() {

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
    OperatingSystem os = new OperatingSystemImpl(
        OperatingSystemFamily.UBUNTU,
        OperatingSystemArchitecture.AMD64,
        OperatingSystemVersions.of(1604,null));
    builder_pre_inst.setOperatingSystem(os);
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
    builder_inst.setOperatingSystem(os);
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
    builder_post_inst.setOperatingSystem(os);
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
    builder_start.setOperatingSystem(os);
    //adjust
    builder_start.addCommand("sudo nohup ~/kafka/bin/kafka-server-start.sh ~/kafka/config/server.properties > ~/kafka/kafka.log 2>&1 &");
    BashBasedHandlerBuilder builder_start_det = new BashBasedHandlerBuilder();
    builder_start_det.setOperatingSystem(os);
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

  static EntireDockerCommands buildEntireDockerCommands() {
    Random rand = new Random();
    EntireDockerCommands.Builder cmdsBuilder = new EntireDockerCommands.Builder();
    try {
      Map<Option,List<String>> createOptionMap = new HashMap<>();
      createOptionMap.put(Option.ENVIRONMENT, Arrays.asList("foo=bar","john=doe"));
      String  n = Integer.toString(rand.nextInt(65536) + 1);
      createOptionMap.put(Option.PORT, new ArrayList<>(Arrays.asList(n)));
      createOptionMap.put(Option.RESTART, new ArrayList<>(Arrays.asList("no")));
      createOptionMap.put(Option.INTERACTIVE, new ArrayList<>(Arrays.asList("")));
      List<OsCommand> createOsCommandList = new ArrayList<>();
      createOsCommandList.add(OsCommand.BASH);
      List<String> createArgsList = new ArrayList<>();
      createArgsList.add("--noediting");
      cmdsBuilder.setOptions(Type.CREATE, createOptionMap);
      cmdsBuilder.setCommand(Type.CREATE, createOsCommandList);
      cmdsBuilder.setArgs(Type.CREATE, createArgsList);

      Map<Option,List<String>> startOptionMap = new HashMap<>();
      startOptionMap.put(Option.INTERACTIVE, new ArrayList<>(Arrays.asList("")));
      cmdsBuilder.setOptions(Type.START, startOptionMap);
    } catch (DockerCommandException ce) {
      System.err.println("Error in creating docker commands");
    }

    return cmdsBuilder.build();
  }

  private static void printCommandParts(EntireDockerCommands cmds) {
    try {
      System.out.println(cmds.getSetOptionsString(DockerCommand.Type.CREATE));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.Type.CREATE));
      System.out.println(cmds.getSetArgsString(DockerCommand.Type.CREATE));
      System.out.println("\n");
      System.out.println(cmds.getSetOptionsString(DockerCommand.Type.START));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.Type.START));
      System.out.println(cmds.getSetArgsString(DockerCommand.Type.START));
      System.out.println("\n");
      System.out.println(cmds.getSetOptionsString(DockerCommand.Type.STOP));
      System.out.println(cmds.getSetOsCommandString(DockerCommand.Type.STOP));
      System.out.println(cmds.getSetArgsString(DockerCommand.Type.STOP));
    } catch (DockerCommandException e) {
      System.err.println("Error in printing docker command strings");
    }
  }

}
