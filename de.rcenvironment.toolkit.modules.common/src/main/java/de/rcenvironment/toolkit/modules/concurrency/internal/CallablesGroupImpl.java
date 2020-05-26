/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadGuard;

/**
 * Default implementation of {@link CallablesGroup}.
 * 
 * @param <T> the result type of method call of the {@link Callable}s
 * 
 * @author Robert Mischke
 */
public class CallablesGroupImpl<T> implements CallablesGroup<T> {

    // frequently instantiated, so use a static logger to avoid overhead
    private static final Log sharedLog = LogFactory.getLog(CallablesGroupImpl.class);

    private List<Callable<T>> tasks = new ArrayList<Callable<T>>();

    private Map<Callable<T>, String> taskIds = new HashMap<>();

    private final AsyncTaskService asyncTaskService;

    public CallablesGroupImpl(AsyncTaskService asyncTaskService) {
        this.asyncTaskService = asyncTaskService;
    }

    @Override
    public void add(Callable<T> task) {
        tasks.add(task);
    }

    @Override
    public void add(Callable<T> task, String taskId) {
        add(task);
        String previousValue = taskIds.put(task, taskId);
        if (previousValue != null) {
            sharedLog.warn("Add the same task instance again, but with a different task id; the new id (" + taskId
                + ") takes precedence over the old id (" + previousValue + ")");
        }
    }

    @Override
    public List<T> executeParallel(AsyncExceptionListener exceptionListener) {
        // this should usually not be called from the GUI thread
        ThreadGuard.checkForForbiddenThread();
        List<Future<T>> futures = new ArrayList<Future<T>>();
        for (Callable<T> task : tasks) {
            futures.add(asyncTaskService.submit(task, taskIds.get(task)));
        }
        List<T> results = new ArrayList<T>();
        // note: this approach matches the order of results to the order of added tasks
        for (Future<T> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException e) {
                results.add(null);
                if (exceptionListener != null) {
                    exceptionListener.onAsyncException(e);
                }
            } catch (ExecutionException e) {
                results.add(null);
                if (exceptionListener != null) {
                    exceptionListener.onAsyncException(e);
                }
            }
        }
        return results;
    }
}
