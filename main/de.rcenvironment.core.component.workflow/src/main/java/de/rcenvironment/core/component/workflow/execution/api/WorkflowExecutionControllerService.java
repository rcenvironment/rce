/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.core.component.execution.api.ExecutionControllerService;

/**
 * Workflow-specific {@link ExecutionControllerService}.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionControllerService extends ExecutionControllerService {

    /**
     * Creates a new {@link WorkflowExecutionController} for the workflow represented be {@link WorkflowExecutionContext}.
     * 
     * @param executionContext {@link WorkflowExecutionContext} of the workflow to execute
     * @param executionAuthTokens the auth tokens which authorizes the {@link WorkflowExecutionController} to execute its components
     * @param calledFromRemote <code>true</code> if method is called remote remote node, <code>false</code> otherwise
     * @return {@link WorkflowExecutionInformation} of the workflow instance
     * @throws WorkflowExecutionException if node is not declared as workflow host
     */
    WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext executionContext,
        Map<String, String> executionAuthTokens, Boolean calledFromRemote) throws WorkflowExecutionException;
    
    /**
     * @param executionId execution identifier of the workflow instance (provided by {@link WorkflowExecutionInformation})
     * @return {@link WorkflowState} of the workflow
     */
    WorkflowState getWorkflowState(String executionId);

    /**
     * @return {@link WorkflowExecutionInformation} objects of all active workflows
     */
    Collection<WorkflowExecutionInformation> getWorkflowExecutionInformations();
    
}
