/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.spi;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;


/**
 * Callback for {@link WorkflowState} changes.
 * 
 * @author Doreen Seider
 */
public interface WorkflowStateChangeListener {

    /**
     * Called on new {@link WorkflowState} (includes newly created workflow).
     * 
     * @param workflowIdentifier identifier of affected workflow
     * @param newState new {@link WorkflowState} or <code>null</code> if workflow is newly created.
     */
    void onNewWorkflowState(String workflowIdentifier, WorkflowState newState);
}
