/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Map;

import de.rcenvironment.core.component.execution.api.ExecutionController;

/**
 * Workflow-specific {@link ExecutionController}.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionController extends ExecutionController {

    /**
     * @return {@link WorkflowState}
     */
    WorkflowState getState();
    
    /**
     * Sets the auth tokens needed to execute the components of the workflow.
     * 
     * @param executionAuthTokens auth tokens to set
     */
    void setComponentExecutionAuthTokens(Map<String, String> executionAuthTokens);
    
}
