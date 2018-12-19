package de.uniulm.omi.cloudiator.lance.util.state;

import java.util.concurrent.Future;

public interface ErrorAwareTransitionState<T> {

  void transitionStarted(T intermediate, Future<?> object);

  void transitionComplete(T to, TransitionException t);
}
