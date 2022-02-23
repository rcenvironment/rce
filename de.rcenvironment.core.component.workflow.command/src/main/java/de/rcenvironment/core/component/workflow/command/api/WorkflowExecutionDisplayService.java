/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.command.api;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;

/**
 * A service that allows displaying workflow executions. By specifying this capability as a service, we avoid strong dependencies on bundles
 * that are ``higher up'' in the dependency-graph, and allow for other displays of workflow executions later down the line.
 * 
 * @author Alexander Weinert
 */
public interface WorkflowExecutionDisplayService {

    /**
     * Due to dependency issues, this service may be injected even though no GUI is actually running. Thus, this method shall be called
     * prior to displayWorkflowExecution to determine whether the latter method is indeed able to display the workflow execution.
     * 
     * @return True if a GUI is currently running and displayWorkflowExecution(WorkflowExecutionInformation) may be called.
     */
    boolean hasGui();

    /**
     * Displays the given workflow execution to the user. Before using this method, first check whether a GUI is actually present using the
     * method hasGui().
     * 
     * @param wfExecInfo The workflow execution to be displayed. Must not be null.
     */
    void displayWorkflowExecution(WorkflowExecutionInformation wfExecInfo);

}
