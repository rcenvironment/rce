/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

/**
 * Exception class representing workflow execution failures.
 * 
 * @author Robert Mischke
 */
public class WorkflowExecutionException extends Exception {

    private static final long serialVersionUID = -7534090344001317930L;

    public WorkflowExecutionException(String message) {
        super(message);
    }
    
    public WorkflowExecutionException(String message, Exception e) {
        super(message, e);
    }

    public WorkflowExecutionException(Exception e) {
        super(e);
    }

}
