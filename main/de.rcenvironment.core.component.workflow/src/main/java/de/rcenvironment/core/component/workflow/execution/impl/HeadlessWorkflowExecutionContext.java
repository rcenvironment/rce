/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.impl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authentication.SingleUser;
import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.component.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.api.HeadlessWorkflowExecutionService.Dispose;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Encapsulates the specific information for a single workflow execution.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class HeadlessWorkflowExecutionContext {

    private final Log log = LogFactory.getLog(getClass());
    
    private final File workflowFile;

    // informational; not needed for execution - seid_do
    private de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext wfExeContext;
    
    // informational; not needed for execution - misc_ro
    private WorkflowState finalState;

    private final TextOutputReceiver outputReceiver;

    private final SingleConsoleRowsProcessor customConsoleRowsProcessor;

    private final CountDownLatch workflowFinishedLatch;

    private final CountDownLatch consoleOutputFinishedLatch;

    private final List<Closeable> resourcesToCloseOnFinish = new ArrayList<>();
    
    private final User user;

    private HeadlessWorkflowExecutionService.Dispose dispose = Dispose.OnFinished;

    private File logDirectory;

    // needed to guarantee this NotificationSubscriber is not removed by GC and thus, can be accessible from remote. get obsolete if
    // following issue is resolved: https://www.sistec.dlr.de/mantis/view.php?id=8659 -- Jan 2015 seid_do
    private ConsoleRowSubscriber consoleRowSubscriber;
    
    // needed to guarantee this NotificationSubscriber is not removed by GC and thus, can be accessible from remote. get obsolete if
    // following issue is resolved: https://www.sistec.dlr.de/mantis/view.php?id=8659 -- Jan 2015 seid_do
    private WorkflowStateNotificationSubscriber workflowStateChangeListener;
    
    public HeadlessWorkflowExecutionContext(File workflowFile, TextOutputReceiver outputReceiver,
        SingleConsoleRowsProcessor customConsoleRowsProcessor) {
        this.workflowFile = workflowFile;
        this.outputReceiver = outputReceiver;
        this.customConsoleRowsProcessor = customConsoleRowsProcessor;
        final int valitidyInDays = 999;
        this.user = new SingleUser(valitidyInDays);

        // wait for two events: the "end of console output marker", and the workflow reaching a
        // finished state;
        // using two separate latches to ensure a duplicate event cannot count for the other
        // type - misc_ro
        workflowFinishedLatch = new CountDownLatch(1);
        consoleOutputFinishedLatch = new CountDownLatch(1);
    }

    public void setConsoleRowSubscriber(ConsoleRowSubscriber consoleRowSubscriber) {
        this.consoleRowSubscriber = consoleRowSubscriber;
    }
    
    public void setWorkflowStateChangeListener(WorkflowStateNotificationSubscriber workflowStateChangeListener) {
        this.workflowStateChangeListener = workflowStateChangeListener;
    }
    
    public User getUser() {
        return user;
    }
    
    public File getWorkflowFile() {
        return workflowFile;
    }
    
    public void setWorkflowExecutionContext(de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext wfExeCtx) {
        this.wfExeContext = wfExeCtx;
    }

    public de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext getWorkflowExecutionContext() {
        return wfExeContext;
    }

    public HeadlessWorkflowExecutionService.Dispose getDispose() {
        return dispose;
    }
    
    public void setDispose(HeadlessWorkflowExecutionService.Dispose dispose) {
        this.dispose = dispose;
    }

    /**
     * @param string output to add
     */
    public void addOutput(String string) {
        outputReceiver.addOutput(string);
    }

    /**
     * @param consoleRow {@link ConsoleRow} received
     */
    public final void reportConsoleRowReceived(ConsoleRow consoleRow) {
        if (customConsoleRowsProcessor != null) {
            customConsoleRowsProcessor.onConsoleRow(consoleRow);
        }
    }

    /**
     * @param newState new {@link WorkflowState}
     * @param getDisposed <code>true</code> if workflow will be disposed, <code>false</code> otherwise
     */
    public synchronized void reportWorkflowTerminated(WorkflowState newState, boolean getDisposed) {
        if (this.finalState != null) {
            log.warn(String.format("Workflow '%s' (%s) was already marked as %s, new final state: %s (%s)",
                getWorkflowExecutionContext().getInstanceName(), wfExeContext.getExecutionIdentifier(),
                finalState.getDisplayName(), newState.getDisplayName(), getWorkflowFile().getAbsolutePath()));
        }
        this.finalState = newState;
        if (finalState != WorkflowState.FINISHED) {
            addOutput("Workflow did not terminated normally (" + finalState.getDisplayName()
                + "); check log and console output for details");
        }
        log.debug(String.format("Workflow '%s' (%s) has terminated, final state: %s (%s)",
            getWorkflowExecutionContext().getInstanceName(), wfExeContext.getExecutionIdentifier(),
            finalState.getDisplayName(), getWorkflowFile()));
        if (!getDisposed) {
            workflowFinishedLatch.countDown();
        }
    }
    
    /**
     * @param newState new {@link WorkflowState}
     */
    public synchronized void reportWorkflowDisposed(WorkflowState newState) {
        log.debug(String.format("Workflow '%s' (%s) is done, disposed: %s (%s)",
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
    public WorkflowState waitForTermination() throws InterruptedException {
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
    public synchronized void registerResourceToCloseOnFinish(Closeable subscriber) {
        resourcesToCloseOnFinish.add(subscriber);
    }

    public void setLogDirectory(File logDirectory) {
        this.logDirectory = logDirectory;
    }

    public File getLogDirectory() {
        return logDirectory;
    }

    /**
     * Closes resources added via {@link #registerResourceToCloseOnFinish(Closeable)}.
     */
    public void closeResources() {
        for (Closeable subscriber : getResourcesToCloseOnFinish()) {
            try {
                subscriber.close();
            } catch (IOException e) {
                log.warn("Error closing resource after end of workflow", e);
            }
        }
    }

    private synchronized List<Closeable> getResourcesToCloseOnFinish() {
        return new ArrayList<>(resourcesToCloseOnFinish);
    }

}
