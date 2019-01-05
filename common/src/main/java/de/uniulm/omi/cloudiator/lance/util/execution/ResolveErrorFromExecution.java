package de.uniulm.omi.cloudiator.lance.util.execution;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/** Created by daniel on 10.10.16. */
public class ResolveErrorFromExecution {

  private final Runnable task;
  private final Throwable t;

  public ResolveErrorFromExecution(Runnable task, Throwable t) {
    this.task = task;
    this.t = t;
  }

  public static ResolveErrorFromExecution of(Runnable task, Throwable t) {
    return new ResolveErrorFromExecution(task, t);
  }

  public Throwable error() {
    Throwable tToLog = t;
    if (tToLog == null && task instanceof Future<?>) {
      try {
        if (((Future) task).isDone() && !((Future) task).isCancelled()) {
          ((Future) task).get();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        tToLog = e.getCause();
      }
    }
    return tToLog;
  }
}
