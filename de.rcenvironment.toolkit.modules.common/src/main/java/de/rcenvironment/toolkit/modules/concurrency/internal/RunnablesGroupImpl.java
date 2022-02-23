/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.internal;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Default implementation of {@link RunnablesGroup}. This current implementation simply delegates to {@link CallablesGroupImpl}.
 * 
 * @author Robert Mischke
 */
public final class RunnablesGroupImpl extends CallablesGroupImpl<RuntimeException> implements RunnablesGroup {

    // frequently instantiated, so use a static logger to avoid overhead
    private static final Log sharedLog = LogFactory.getLog(CallablesGroupImpl.class);

    public RunnablesGroupImpl(AsyncTaskService asyncTaskService) {
        super(asyncTaskService);
    }

    @Override
    public void add(final Runnable task) {
        add(new Callable<RuntimeException>() {

            @Override
            @TaskDescription("Internal RunnablesGroup task delegate")
            public RuntimeException call() throws Exception {
                // CHECKSTYLE:DISABLE (IllegalCatch) - Throwables should not slip through, e.g. for unit assertion tests
                try {
                    task.run();
                    return null;
                } catch (Throwable e) {
                    return wrapIfNecessary(e);
                }
                // CHECKSTYLE:ENABLE (IllegalCatch)
            }

            private RuntimeException wrapIfNecessary(Throwable e) {
                if (e instanceof RuntimeException) {
                    sharedLog.debug("Caught asynchronous exception", e);
                    return (RuntimeException) e;
                } else {
                    sharedLog.error("Non-RTE throwable caught:", e);
                    return new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public List<RuntimeException> executeParallel() {
        return super.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                sharedLog.error("Uncaught exception in RunnablesGroup", e);
            }
        });
    }
}
