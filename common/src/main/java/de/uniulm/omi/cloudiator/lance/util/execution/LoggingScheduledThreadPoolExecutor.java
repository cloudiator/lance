package de.uniulm.omi.cloudiator.lance.util.execution;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by daniel on 07.10.16. */
public class LoggingScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

  static final Logger LOGGER = LoggerFactory.getLogger(LoggingScheduledThreadPoolExecutor.class);

  public LoggingScheduledThreadPoolExecutor(int nThreads, ThreadFactory threadFactory) {
    super(nThreads, threadFactory);
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    Throwable tToLog = ResolveErrorFromExecution.of(r, t).error();
    if (tToLog != null) {
      LOGGER.error("Uncaught exception occurred during the execution of task " + r + ".", tToLog);
    }
  }
}
