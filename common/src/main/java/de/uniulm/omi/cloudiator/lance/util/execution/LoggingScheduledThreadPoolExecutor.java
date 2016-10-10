package de.uniulm.omi.cloudiator.lance.util.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Created by daniel on 07.10.16.
 */
public class LoggingScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    final static Logger LOGGER = LoggerFactory.getLogger(LoggingScheduledThreadPoolExecutor.class);

    public LoggingScheduledThreadPoolExecutor(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
    }

    @Override protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        Throwable tToLog = ResolveErrorFromExecution.of(r, t).error();
        if (tToLog != null) {
            LOGGER.error("Uncaught exception occurred during the execution of task " + r + ".",
                tToLog);
        }
    }
}
