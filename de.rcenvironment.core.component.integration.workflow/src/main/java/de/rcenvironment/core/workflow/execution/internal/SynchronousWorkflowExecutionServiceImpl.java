/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.workflow.execution.SynchronousWorkflowExecutionService;
import de.rcenvironment.core.workflow.execution.internal.WorkflowRunAdapter.Builder;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

@Component
public class SynchronousWorkflowExecutionServiceImpl implements SynchronousWorkflowExecutionService {
    
    private WorkflowExecutionService workflowExecutionService;
    
    private DistributedNotificationService notificationService;
    
    private AsyncTaskService taskService;
    
    @Override
    public boolean executeWorkflow(final WorkflowExecutionContext workflowExecutionContext) throws ComponentException {
        final WorkflowExecutionInformation handle = startWorkflowExecutionAndGetHandle(workflowExecutionContext);
        return awaitWorkflowTermination(handle);
    }

    private WorkflowExecutionInformation startWorkflowExecutionAndGetHandle(final WorkflowExecutionContext workflowExecutionContext)
        throws ComponentException {

        final WorkflowExecutionInformation handle;
        try {
            handle = workflowExecutionService.startWorkflowExecution(workflowExecutionContext);
        } catch (WorkflowExecutionException | RemoteOperationException e) {
            throw new ComponentException("Unexpected exception thrown during start of workflow execution", e);
        }
        return handle;
    }

    private boolean awaitWorkflowTermination(final WorkflowExecutionInformation handle) throws ComponentException {
        final WorkflowRunAdapter.Builder workflowRunBuilder = createWorkflowRunAdapterBuilder(handle);

        final WorkflowRunAdapter workflowRun;
        try {
            workflowRun = workflowRunBuilder.buildAndRegisterForWorkflowStateUpdates();
        } catch (RemoteOperationException e) {
            throw new ComponentException("Error during registration for workflow state updates", e);
        }

        try {
            workflowRun.awaitWorkflowTermination();
        } catch (InterruptedException e) {
            throw new ComponentException("Interrupted while waiting for workflow to finish", e);
        }

        return !workflowRun.executionFailed();
    }

    protected Builder createWorkflowRunAdapterBuilder(final WorkflowExecutionInformation handle) {
        return new Builder()
            .workflowExecutionIdentifier(handle.getExecutionIdentifier())
            .distributedNotificationService(notificationService)
            .timeService(this::getWalltime)
            .taskService(taskService);
    }
    
    protected long getWalltime() {
        return System.currentTimeMillis();
    }
   
    @Reference
    public void bindWorkflowExecutionService(WorkflowExecutionService service) {
        this.workflowExecutionService = service;
    }

    @Reference
    public void bindDistributedNotificationService(DistributedNotificationService service) {
        this.notificationService = service;
    }
    
    @Reference
    public void bindTaskService(AsyncTaskService service) {
        this.taskService = service;
    }
}
