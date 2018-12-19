package de.uniulm.omi.cloudiator.lance.lca.containers.dummy;

import de.uniulm.omi.cloudiator.lance.lca.container.ComponentInstanceId;
import de.uniulm.omi.cloudiator.lance.lca.container.ContainerException;
import de.uniulm.omi.cloudiator.lance.lca.container.port.DownstreamAddress;
import de.uniulm.omi.cloudiator.lance.lca.container.port.PortDiff;
import de.uniulm.omi.cloudiator.lance.lifecycle.HandlerType;
import de.uniulm.omi.cloudiator.lance.lifecycle.LifecycleActionInterceptor;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.DetectorType;
import de.uniulm.omi.cloudiator.lance.lifecycles.CoreElements;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DummyInterceptor implements LifecycleActionInterceptor {

  private static final HandlerType portUpdateType = new HandlerType() {};
  private final List<HandlerType> called = new LinkedList<>();
  private final Map<HandlerType, Integer> countingHandlerCalls = new HashMap<>();
  private final List<String> invocations = new LinkedList<>();
  private final Map<HandlerType, List<Integer>> failureMap = new HashMap<>();
  private volatile HandlerType ongoingPreparation = null;

  @Override
  public void prepare(HandlerType type) throws ContainerException {
    if (ongoingPreparation != null) {
      throw new IllegalStateException("preparation is ongoing: " + ongoingPreparation);
    }
    if (countingHandlerCalls.containsKey(type)) {
      throw new IllegalStateException("handler called twice: " + type);
    }
    ongoingPreparation = type;
    countingHandlerCalls.put(type, 1);
    invocations.add("prepare_" + type);
    called.add(type);
    checkForFailure(type);
  }

  @Override
  public void postprocess(HandlerType type) throws ContainerException {
    if (!countingHandlerCalls.containsKey(type)) {
      throw new IllegalStateException("prepare handler not called: " + type);
    }
    if (ongoingPreparation != type) {
      throw new IllegalStateException("prepare handler not called: " + type);
    }
    int i = countingHandlerCalls.get(type);
    countingHandlerCalls.put(type, i + 1);
    ongoingPreparation = null;
    invocations.add("postprocess_" + type);
    checkForFailure(type);
  }

  @Override
  public ComponentInstanceId getComponentInstanceId() {
    invocations.add("getComponentInstanceId");
    return CoreElements.componentInstanceId;
  }

  @Override
  public void postprocessPortUpdate(PortDiff<DownstreamAddress> diff) throws ContainerException {
    if (!countingHandlerCalls.containsKey(portUpdateType)) {
      throw new IllegalStateException("prepare port update handler not called");
    }
    if (ongoingPreparation != portUpdateType) {
      throw new IllegalStateException("prepare port update handler not called");
    }
    int i = countingHandlerCalls.get(portUpdateType);
    countingHandlerCalls.put(portUpdateType, i + 1);
    ongoingPreparation = null;
    invocations.add("postprocessPortUpdate_" + diff);
    checkForFailure(portUpdateType);
  }

  @Override
  public void preprocessPortUpdate(PortDiff<DownstreamAddress> diff) throws ContainerException {
    if (ongoingPreparation != null) {
      throw new IllegalStateException("preparation is ongoing: " + ongoingPreparation);
    }
    if (countingHandlerCalls.containsKey(portUpdateType)) {
      throw new IllegalStateException("handler called twice: " + portUpdateType);
    }
    ongoingPreparation = portUpdateType;
    countingHandlerCalls.put(portUpdateType, 1);
    invocations.add("preprocessPortUpdate_" + diff);
    called.add(portUpdateType);
    checkForFailure(portUpdateType);
  }

  @Override
  public void postprocessDetector(DetectorType type) throws ContainerException {
    if (!countingHandlerCalls.containsKey(type)) {
      throw new IllegalStateException(
          "prepare detector handler not called: " + type + ": " + called);
    }
    if (ongoingPreparation != type) {
      throw new IllegalStateException("prepare detector not called: " + type);
    }
    int i = countingHandlerCalls.get(type);
    countingHandlerCalls.put(type, i + 1);
    ongoingPreparation = null;
    invocations.add("postprocessDetector_" + type);
    checkForFailure(type);
  }

  @Override
  public void preprocessDetector(DetectorType type) throws ContainerException {
    if (ongoingPreparation != null) {
      throw new IllegalStateException("preparation is ongoing: " + ongoingPreparation);
    }
    int number = 0;
    if (countingHandlerCalls.containsKey(type)) {
      countingHandlerCalls.get(type);
    }
    ongoingPreparation = type;
    countingHandlerCalls.put(type, number + 1);
    invocations.add("preprocessDetector_" + type);
    called.add(type);
    checkForFailure(type);
  }

  public int handlerCalls() {
    int sum = 0;
    for (Entry<HandlerType, Integer> e : countingHandlerCalls.entrySet()) {
      if (e.getValue() != null) {
        sum = sum + e.getValue();
      } else {
        throw new IllegalStateException();
      }
    }
    return sum;
  }

  public List<HandlerType> invokedHandlers() {
    return new ArrayList<>(called);
  }

  public DummyInterceptor failsAt(HandlerType t, boolean preprocess, int iteration) {
    if (iteration < 0) throw new IllegalArgumentException();
    int count = iteration * 2 + (preprocess ? 1 : 2);
    addToFailure(t, count);
    return this;
  }

  private void checkForFailure(HandlerType t) throws ContainerException {
    Integer i = countingHandlerCalls.get(t);
    if (i == null) {
      throw new IllegalStateException();
    }
    List<Integer> j = failureMap.get(t);
    if (j == null) return;
    if (j.contains(i)) {
      throw new ContainerException();
    }
  }

  private void addToFailure(HandlerType t, int count) {
    List<Integer> l = failureMap.get(t);
    if (l == null) {
      l = new ArrayList<>();
      failureMap.put(t, l);
    }
    l.add(count);
  }
}
