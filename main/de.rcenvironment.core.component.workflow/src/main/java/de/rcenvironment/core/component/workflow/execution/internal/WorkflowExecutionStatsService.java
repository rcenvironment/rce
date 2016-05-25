/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;

/**
 * Records stats related to workflow execution.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionStatsService {
    
    /**
     * Records stats at the time a workflow is started.
     * 
     * @param wfExeCtx context of the workflow execution
     */
    void addStatsAtWorkflowStart(WorkflowExecutionContext wfExeCtx);
    
    /**
     * Records stats at the time a workflow is terminated.
     * 
     * @param wfExeCtx context of the workflow execution
     * @param finalWorkflowState final state of the workflow executed
     */
    void addStatsAtWorkflowTermination(WorkflowExecutionContext wfExeCtx, WorkflowState finalWorkflowState);

}
