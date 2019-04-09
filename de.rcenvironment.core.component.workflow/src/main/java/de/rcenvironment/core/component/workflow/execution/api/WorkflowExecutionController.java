/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Map;

import de.rcenvironment.core.component.execution.api.ExecutionController;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallback;

/**
 * Workflow-specific {@link ExecutionController}.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionController extends ExecutionController, WorkflowExecutionControllerCallback {

    /**
     * @return {@link WorkflowState}
     */
    WorkflowState getState();
    
    /**
     * @return identifier the workflow is stored under in the data management
     */
    Long getDataManagementId();
    
    /**
     * Sets the auth tokens needed to execute the components of the workflow.
     * 
     * @param executionAuthTokens auth tokens to set
     */
    void setComponentExecutionAuthTokens(Map<String, String> executionAuthTokens);
    
}
