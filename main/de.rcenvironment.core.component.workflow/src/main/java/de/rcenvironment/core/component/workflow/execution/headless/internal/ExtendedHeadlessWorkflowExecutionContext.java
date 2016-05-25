/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.headless.api.ConsoleRowSubscriber;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DeletionBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DisposalBehavior;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Encapsulates the specific information for a single workflow execution.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class ExtendedHeadlessWorkflowExecutionContext implements HeadlessWorkflowExecutionContext {

    private final Log log = LogFactory.getLog(getClass());

    private final HeadlessWorkflowExecutionContext headlessWfExeContext;

    private final CountDownLatch workflowFinishedLatch;

    private final CountDownLatch consoleOutputFinishedLatch;

    private final List<Closeable> resourcesToCloseOnFinish = new ArrayList<>();

    private final List<NotificationSubscription> notificationSubscriptionsToUnsubscribeOnFinish = new ArrayList<>();

    // informational; not needed for execution - seid_do
    private WorkflowExecutionContext wfExeContext;

    // informational; not needed for execution - misc_ro
    private WorkflowState finalState;

    public ExtendedHeadlessWorkflowExecutionContext(HeadlessWorkflowExecutionContext headlessWfExeContext) {
        this.headlessWfExeContext = headlessWfExeContext;

        // wait for two events: the "end of console output marker", and the workflow reaching a
        // finished state;
        // using two separate latches to ensure a duplicate event cannot count for the other type -
        // misc_ro
        workflowFinishedLatch = new CountDownLatch(1);
        consoleOutputFinishedLatch = new CountDownLatch(1);
    }

    protected void setWorkflowExecutionContext(WorkflowExecutionContext wfExeCtx) {
        this.wfExeContext = wfExeCtx;
    }

    protected WorkflowExecutionContext getWorkflowExecutionContext() {
        return wfExeContext;
    }

    protected void addOutput(String verboseOutput) {
        addOutput(null, verboseOutput);
    }

    protected void addOutput(String compactOutput, String verboseOutput) {
        if (headlessWfExeContext.getTextOutputReceiver() != null) {
            if (isCompactOutput()) {
                if (compactOutput != null && !compactOutput.isEmpty()) {
                    headlessWfExeContext.getTextOutputReceiver().addOutput(compactOutput);
                }
            } else {
                if (verboseOutput != null && !verboseOutput.isEmpty()) {
                    headlessWfExeContext.getTextOutputReceiver().addOutput(verboseOutput);
                }
            }
        }
    }

    /**
     * @param consoleRow called if new console row was received, e.g. by the
     *        {@link ConsoleRowSubscriber}
     */
    public final void reportConsoleRowReceived(ConsoleRow consoleRow) {
        if (headlessWfExeContext.getSingleConsoleRowReceiver() != null) {
            headlessWfExeContext.getSingleConsoleRowReceiver().onConsoleRow(consoleRow);
        }
    }

    protected synchronized void reportWorkflowNotAliveAnymore(String errorMessage) {
        log.error(StringUtils.format("Final state of workflow '%s' (%s) is %s - %s",
            getWorkflowExecutionContext().getInstanceName(), wfExeContext.getExecutionIdentifier(),
            WorkflowState.UNKNOWN.getDisplayName(),
            errorMessage));
        finalState = WorkflowState.UNKNOWN;
        workflowFinishedLatch.countDown();
        consoleOutputFinishedLatch.countDown();
    }

    /**
     * @param newState new {@link WorkflowState}
     * @param dispose <code>true</code> if workflow will be disposed, <code>false</code> otherwise
     */
    protected synchronized void reportWorkflowTerminated(WorkflowState newState, boolean dispose) {
        if (this.finalState != null) {
            log.warn(StringUtils.format("Workflow '%s' (%s) was already marked as %s, new final state: %s (%s)",
                getWorkflowExecutionContext().getInstanceName(), wfExeContext.getExecutionIdentifier(),
                finalState.getDisplayName(), newState.getDisplayName(), getWorkflowFile().getAbsolutePath()));
        }
        this.finalState = newState;
        if (finalState != WorkflowState.FINISHED) {
            addOutput(StringUtils.format("'%s' terminated abnormally: %s; check log and console output for details",
                getWorkflowFile().getName(), finalState.getDisplayName()));
            
        }
        log.debug(StringUtils.format("Workflow '%s' (%s) has terminated, final state: %s (%s)",
            getWorkflowExecutionContext().getInstanceName(), wfExeContext.getExecutionIdentifier(),
            finalState.getDisplayName(), getWorkflowFile()));
        if (!dispose) {
            workflowFinishedLatch.countDown();
        }
    }

    /**
     * @param newState new {@link WorkflowState}
     */
    protected synchronized void reportWorkflowDisposed(WorkflowState newState) {
        log.debug(StringUtils.format("Workflow '%s' (%s) is done, disposed: %s (%s)",
            getWorkflowExecutionContext().getInstanceName(), wfExeContext.getExecutionIdentifier(),
            newState == WorkflowState.DISPOSED, getWorkflowFile()));
        workflowFinishedLatch.countDown();
    }

    /**
     * Reports that all console output was received.
     */
    public void reportConsoleOutputTerminated() {
        consoleOutputFinishedLatch.countDown();
    }

    /**
     * Awaits termination.
     * 
     * @return {@link WorkflowState} after termination
     * @throws InterruptedException on error
     */
    protected WorkflowState waitForTermination() throws InterruptedException {
        // TODO add timeout and workflow heartbeat checking
        workflowFinishedLatch.await();
        consoleOutputFinishedLatch.await();
        synchronized (this) {
            return finalState;
        }
    }

    /**
     * Registers a resource which must be closed if workflow is terminated.
     * 
     * @param subscriber {@link Closeable} subcriber to register
     */
    protected synchronized void registerResourceToCloseOnFinish(Closeable resource) {
        resourcesToCloseOnFinish.add(resource);
    }

    /**
     * Registers a NS which must be closed if workflow is terminated.
     * 
     * @param subscriber {@link Closeable} subcriber to register
     */
    protected synchronized void registerNotificationSubscriptionsToUnsubscribeOnFinish(NotificationSubscription subscriber) {
        notificationSubscriptionsToUnsubscribeOnFinish.add(subscriber);
    }

    /**
     * Closes resources added via {@link #registerResourceToCloseOnFinish(Closeable)}.
     */
    protected void closeResourcesQuietly() {
        for (Closeable resource : getResourcesToCloseOnFinish()) {
            try {
                resource.close();
            } catch (IOException e) {
                log.warn(StringUtils.format("Error closing resource after end of workflow '%s' (%s) ",
                    wfExeContext.getInstanceName(), wfExeContext.getExecutionIdentifier()), e);
            }
        }
    }

    /**
     * Unsubscribes {@link NotificationSubscriber}s.
     */
    protected void unsubscribeNotificationSubscribersQuietly(DistributedNotificationService notificationService) {
        for (NotificationSubscription subscription : getNotificationSubscribersToUnsubscribeOnFinish()) {
            try {
                notificationService.unsubscribe(subscription.notificationId, subscription.subscriber, subscription.nodeId);
            } catch (RemoteOperationException e) {
                log.warn(StringUtils.format("Failed to unsubscribe %s from notification service (workflow '%s' (%s)",
                    subscription.subscriber.getClass(), wfExeContext.getInstanceName(), wfExeContext.getExecutionIdentifier()), e);
            }
        }
    }

    private synchronized List<Closeable> getResourcesToCloseOnFinish() {
        return new ArrayList<>(resourcesToCloseOnFinish);
    }

    private synchronized List<NotificationSubscription> getNotificationSubscribersToUnsubscribeOnFinish() {
        return new ArrayList<>(notificationSubscriptionsToUnsubscribeOnFinish);
    }

    @Override
    public File getWorkflowFile() {
        return headlessWfExeContext.getWorkflowFile();
    }

    @Override
    public File getPlaceholdersFile() {
        return headlessWfExeContext.getPlaceholdersFile();
    }

    @Override
    public File getLogDirectory() {
        return headlessWfExeContext.getLogDirectory();
    }

    @Override
    public TextOutputReceiver getTextOutputReceiver() {
        return headlessWfExeContext.getTextOutputReceiver();
    }

    @Override
    public SingleConsoleRowsProcessor getSingleConsoleRowReceiver() {
        return headlessWfExeContext.getSingleConsoleRowReceiver();
    }

    @Override
    public DisposalBehavior getDisposalBehavior() {
        return headlessWfExeContext.getDisposalBehavior();
    }

    @Override
    public DeletionBehavior getDeletionBehavior() {
        return headlessWfExeContext.getDeletionBehavior();
    }

    @Override
    public boolean shouldAbortIfWorkflowUpdateRequired() {
        return headlessWfExeContext.shouldAbortIfWorkflowUpdateRequired();
    }

    @Override
    public boolean isCompactOutput() {
        return headlessWfExeContext.isCompactOutput();
    }

    /**
     * Encapsulates information about {@link NotificationSubscriber} subscribed.
     *
     * @author Doreen Seider
     *
     */
    protected class NotificationSubscription {

        protected NotificationSubscriber subscriber;

        protected String notificationId;

        protected NodeIdentifier nodeId;
    }

}
