/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.internal;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.workflow.execution.internal.SynchronousWorkflowExecutionServiceImpl;

// We disable Sonar for this class declaration, as it only complains about there not being any tests. This is, however, by design,
// as this class implements the subclass-and-stub-pattern
class SynchronousWorkflowExecutionServiceImplUnderTest extends SynchronousWorkflowExecutionServiceImpl { // NOSONAR

    private long walltime;
    
    private Thread thread;
    
    private boolean workflowExecutionSucceeded;

    private Runnable heartbeatChecker;

    @Override
    protected long getWalltime() {
        return this.walltime;
    }

    public void advanceWalltime(int milliseconds) {
        this.walltime += milliseconds;
    }
    
    public void captureHeartbeatChecker(Runnable heartbeatChecker) {
        this.heartbeatChecker = heartbeatChecker;
    }
    
    public void executeHeartbeatChecker() {
        this.heartbeatChecker.run();
    }
    
    public void startWorkflowExecutionAsynchronously(WorkflowExecutionContext contextToExecute) {
        this.thread = new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    workflowExecutionSucceeded = executeWorkflow(contextToExecute);
                } catch (ComponentException e) {
                    // Not expected
                }
            }
        });
        this.thread.start();
    }
    
    public boolean awaitWorkflowTermination() {
        try {
            this.thread.join(100000);
        } catch (InterruptedException e) {
            return false;
        }
        return this.workflowExecutionSucceeded;
    }
}
