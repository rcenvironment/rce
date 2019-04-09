/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.spi;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;


/**
 * Callback for {@link WorkflowState} changes, which only considers one single workflow.
 * 
 * @author Doreen Seider
 */
public interface SingleWorkflowStateChangeListener {

    /**
     * Called on new {@link WorkflowState} (includes newly created workflow).
     * 
     * @param newWorkflowState new {@link WorkflowState} or <code>null</code> if workflow is newly created.
     */
    void onWorkflowStateChanged(WorkflowState newWorkflowState);
    
    /**
     * Call if receiving {@link WorkflowState#IS_ALIVE} states stopped for workflow not yet terminated.
     * 
     * @param errorMessage message explaining the error, which can be used within a log message
     */
    void onWorkflowNotAliveAnymore(String errorMessage);
    
}
