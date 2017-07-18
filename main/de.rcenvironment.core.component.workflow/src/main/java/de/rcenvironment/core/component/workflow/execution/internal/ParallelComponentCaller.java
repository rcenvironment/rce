/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Helper class to call multiple components in parallel.
 * 
 * @author Doreen Seider
 */
public abstract class ParallelComponentCaller {

    private static final Log LOG = LogFactory.getLog(ParallelComponentCaller.class);
    
    private final String logMessagePart;
    
    private final Set<String> compsToConsider;

    protected ParallelComponentCaller(Set<String> componentsToConsider, WorkflowExecutionContext wfExeCtx) {
        this.compsToConsider = componentsToConsider;
        logMessagePart = WorkflowExecutionUtils.substituteWorkflowNameAndExeId(wfExeCtx);
    }

    protected Throwable callParallelAndWait() {
        CallablesGroup<Throwable> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(Throwable.class);
        for (String executionId : compsToConsider) {
            final String finalExecutionId = executionId;
            callablesGroup.add(new Callable<Throwable>() {

                @Override
                @TaskDescription("Call method of workflow component")
                public Throwable call() throws Exception {
                    try {
                        callSingleComponent(finalExecutionId);
                    } catch (RemoteOperationException | ExecutionControllerException | RuntimeException e) {
                        onErrorInSingleComponentCall(finalExecutionId, e);
                        return e;
                    }
                    return null;
                }
            }, StringUtils.format("Call component ('%s'): %s", finalExecutionId, getMethodToCallAsString()));
        }

        List<Throwable> throwables = callablesGroup.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                // should never happen
            }
        });

        for (Throwable t : throwables) {
            if (t != null) {
                logError(t);
            }
        }

        for (Throwable t : throwables) {
            if (t != null) {
                return new Throwable(StringUtils.format("Failed to %s component(s)", getMethodToCallAsString()), t);
            }
        }
        return null;
    }

    protected abstract void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException;

    protected abstract String getMethodToCallAsString();

    protected void onErrorInSingleComponentCall(String compExeId, Throwable t) {}

    protected void logError(Throwable t) {
        if (t instanceof RemoteOperationException) {
            LOG.error(StringUtils.format("Failed to %s component(s) of %s; cause: %s", 
                getMethodToCallAsString(), logMessagePart, t.toString()));
        } else {
            LOG.error(StringUtils.format("Failed to %s component(s) of %s", getMethodToCallAsString(), logMessagePart, t));
        }
    }
}
