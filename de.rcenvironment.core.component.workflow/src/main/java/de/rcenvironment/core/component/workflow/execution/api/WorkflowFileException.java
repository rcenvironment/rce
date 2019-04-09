/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * Exception class representing workflow file failures.
 * 
 * @author Doreeen Seider
 */
public class WorkflowFileException extends Exception {

    private static final long serialVersionUID = -733143864502026352L;
    
    private final WorkflowDescription parsedWd;

    public WorkflowFileException(String message) {
        super(message);
        this.parsedWd = null;
    }
    
    public WorkflowFileException(String message, Exception e) {
        super(message, e);
        this.parsedWd = null;
    }

    public WorkflowFileException(String message, WorkflowDescription parsedWd) {
        super(message);
        this.parsedWd = parsedWd;
    }
    
    public WorkflowDescription getParsedWorkflowDescription() {
        return parsedWd;
    }

}
