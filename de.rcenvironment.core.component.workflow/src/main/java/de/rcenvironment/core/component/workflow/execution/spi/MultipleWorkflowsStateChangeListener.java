/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.spi;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;


/**
 * Callback for {@link WorkflowState} changes, which considers multiple workflows.
 * 
 * @author Doreen Seider
 */
public interface MultipleWorkflowsStateChangeListener {

    /**
     * Called on new {@link WorkflowState} (includes newly created workflow).
     * 
     * @param wfExecutionId execution identifier of the workflow affected
     * @param newWorkflowState new {@link WorkflowState} or <code>null</code> if workflow is newly created.
     */
    void onWorkflowStateChanged(String wfExecutionId, WorkflowState newWorkflowState);
    
}
