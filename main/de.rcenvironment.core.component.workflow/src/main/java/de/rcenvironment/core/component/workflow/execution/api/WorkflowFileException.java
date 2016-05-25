/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
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
