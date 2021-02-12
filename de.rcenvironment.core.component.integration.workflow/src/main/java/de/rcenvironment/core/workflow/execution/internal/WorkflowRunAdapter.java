/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.internal;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

class WorkflowRunAdapter {

    public static class Builder {

        private String workflowExecutionIdentifier;

        private DistributedNotificationService notificationService;

        private LongSupplier timeService;

        private AsyncTaskService taskService;

        public Builder workflowExecutionIdentifier(final String executionIdentifier) {
            this.workflowExecutionIdentifier = executionIdentifier;
            return this;
        }

        public Builder distributedNotificationService(final DistributedNotificationService service) {
            this.notificationService = service;
            return this;
        }

        public Builder timeService(final LongSupplier service) {
            this.timeService = service;
            return this;
        }

        public Builder taskService(final AsyncTaskService service) {
            this.taskService = service;
            return this;
        }

        public WorkflowRunAdapter buildAndRegisterForWorkflowStateUpdates() throws RemoteOperationException {
            Objects.requireNonNull(this.timeService);
            Objects.requireNonNull(this.taskService);

            final WorkflowRunAdapter product = new WorkflowRunAdapter(timeService, taskService);

            product.timestampOfLastHeartbeat = new AtomicLong(timeService.getAsLong());

            notificationService.subscribe(WorkflowConstants.STATE_NOTIFICATION_ID + workflowExecutionIdentifier,
                new WorkflowStateNotificationHandler(workflowExecutionIdentifier, product::onWorkflowHeartbeat,
                    product::onWorkflowTermination),
                null);

            return product;
        }
    }

    /**
     * The interval in ms in which to check for heartbeats of the running workflow.
     */
    private static final int INTERVAL_BETWEEN_HEARTBEAT_CHECKS_IN_MS = 1000;

    /**
     * If this amount of time (in ms) passes between heartbeats, the workflow is considered to be dead. Must be larger than
     * WorkflowExecutionServiceImpl#ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC, which is set to 6000 at the time of writing
     */
    private static final long MAX_TIME_BETWEEN_HEARTBEATS_IN_MS = 20000;

    private final Semaphore workflowFinished = new Semaphore(0);

    private final LongSupplier timeService;

    private final AsyncTaskService taskService;

    private WorkflowState workflowState;

    private AtomicLong timestampOfLastHeartbeat;

    // We make the constructor protected in order to enforce use of the Builder
    protected WorkflowRunAdapter(final LongSupplier timeService, final AsyncTaskService taskService) {
        this.timeService = timeService;
        this.taskService = taskService;
    }

    public void awaitWorkflowTermination() throws InterruptedException {
        final ScheduledFuture<?> backgroundTask = taskService.scheduleAtFixedInterval("Check for workflow heartbeat", () -> {
            if (timeService.getAsLong() - this.getTimestampOfLastHeartbeat() > MAX_TIME_BETWEEN_HEARTBEATS_IN_MS) {
                LogFactory.getLog(WorkflowRunAdapter.class).debug("Finishing wait for workflow termination due to missing heartbeat");
                workflowState = WorkflowState.FAILED;
                workflowFinished.release();
            }
        }, INTERVAL_BETWEEN_HEARTBEAT_CHECKS_IN_MS);
        workflowFinished.acquire();
        backgroundTask.cancel(false);
    }

    public void onWorkflowTermination(final WorkflowState finalStatus) {
        workflowFinished.release();
        this.workflowState = finalStatus;
    }

    public boolean executionFailed() {
        return WorkflowState.FAILED.equals(this.workflowState);
    }

    private void onWorkflowHeartbeat() {
        timestampOfLastHeartbeat.set(timeService.getAsLong());
    }

    public long getTimestampOfLastHeartbeat() {
        return timestampOfLastHeartbeat.get();
    }
}
