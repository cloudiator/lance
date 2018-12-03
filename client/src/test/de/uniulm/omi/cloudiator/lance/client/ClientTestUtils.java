package de.uniulm.omi.cloudiator.lance.client;

import de.uniulm.omi.cloudiator.lance.application.component.ComponentId;
import de.uniulm.omi.cloudiator.lance.application.component.DeployableComponent;
import de.uniulm.omi.cloudiator.lance.application.component.DockerComponent;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleStore;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import de.uniulm.omi.cloudiator.lance.util.application.ProvidedPortInfo;
import de.uniulm.omi.cloudiator.lance.util.application.RequiredPortInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

public class ClientTestUtils {

  private ClientTestUtils(){}

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
      ce.printStackTrace();
    }

    return cmdsBuilder.build();
  }

  public static DockerComponent.Builder buildDockerComponentBuilder(
      LifecycleClient client,
      String compName,
      ComponentId id,
      Set<ProvidedPortInfo> providedInfs,
      Set<RequiredPortInfo> requiredInfs,
      String imageFolder, String imageName,
      String tag) {
    DockerComponent.Builder builder = new DockerComponent.Builder(buildEntireDockerCommands(), imageName);
    builder.name(compName);
    builder.imageFolder(imageFolder);
    builder.tag(tag);
    builder.myId(id);

    for (ProvidedPortInfo pInf: providedInfs)
      builder.addInport(
          pInf.getProvidedPortName(),
          pInf.getPortType(),
          pInf.getCardinality(),
          pInf.getProvidedPort());


    for (RequiredPortInfo rInf: requiredInfs)
      builder.addOutport(
          rInf.getRequiredPortName(),
          rInf.getPuHandler(),
          rInf.getCardinality(),
          rInf.getMin());

    builder.deploySequentially(true);
    return builder;
  }

  public static DeployableComponent buildDeployableComponent(
      LifecycleClient client,
      String compName,
      ComponentId id,
      Set<ProvidedPortInfo> providedInfs,
      Set<RequiredPortInfo> requiredInfs,
      Callable<LifecycleStore> createLifeCycleStore) {

    DeployableComponent.Builder builder = DeployableComponent.Builder.createBuilder(compName,id);

    for (ProvidedPortInfo pInf: providedInfs)
      builder.addInport(
          pInf.getProvidedPortName(),
          pInf.getPortType(),
          pInf.getCardinality(),
          pInf.getProvidedPort());


    for (RequiredPortInfo rInf: requiredInfs)
      builder.addOutport(
          rInf.getRequiredPortName(),
          rInf.getPuHandler(),
          rInf.getCardinality(),
          rInf.getMin());

    try {
      builder.addLifecycleStore(createLifeCycleStore.call());
    } catch (Exception ex) {
      System.err.println("Server not reachable");
    }

    builder.deploySequentially(true);
    DeployableComponent comp = builder.build();
    return comp;
  }
}
