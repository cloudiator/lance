package de.uniulm.omi.cloudiator.lance.util.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by daniel on 07.10.16.
 */
public class LoggingThreadPoolExecutor extends ThreadPoolExecutor {

    final static Logger LOGGER = LoggerFactory.getLogger(LoggingThreadPoolExecutor.class);

    public LoggingThreadPoolExecutor(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            threadFactory);
    }

    @Override protected void afterExecute(Runnable r, Throwable t) {
        LOGGER.debug(String.format("Executed runnable %s. Got throwable %s", r, t));
        super.afterExecute(r, t);
        Throwable tToLog = ResolveErrorFromExecution.of(r, t).error();
        if (tToLog != null) {
            LOGGER.error("Uncaught exception occurred during the execution of task " + r + ".",
                tToLog);
        }
    }
}
