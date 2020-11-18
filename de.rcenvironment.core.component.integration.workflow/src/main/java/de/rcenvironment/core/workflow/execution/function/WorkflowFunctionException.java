/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.workflow.execution.function;

/**
 * Exception thrown if any error occurs during the construction or execution of a {@link WorkflowFunction}.
 * @author Alexander Weinert
 */
public class WorkflowFunctionException extends Exception {

    private static final long serialVersionUID = 202873833836749730L;

    public WorkflowFunctionException() {
        super();
    }

    public WorkflowFunctionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public WorkflowFunctionException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkflowFunctionException(String message) {
        super(message);
    }

    public WorkflowFunctionException(Throwable cause) {
        super(cause);
    }
}
