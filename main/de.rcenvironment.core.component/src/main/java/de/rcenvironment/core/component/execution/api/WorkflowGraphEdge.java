/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;

/**
 * Represents an edge in the workflow graph.
 * 
 * @author Doreen Seider
 */
public class WorkflowGraphEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sourceExecutionIdentifier;

    private final String outputIdentifier;

    private final String targetExecutionIdentifier;

    private final String inputIdentifier;

    public WorkflowGraphEdge(String sourceExecutionIdentifier, String outputIdentifier,
        String targetExecutionIdentifier, String inputIdentifier) {
        this.sourceExecutionIdentifier = sourceExecutionIdentifier;
        this.outputIdentifier = outputIdentifier;
        this.targetExecutionIdentifier = targetExecutionIdentifier;
        this.inputIdentifier = inputIdentifier;
    }

    public String getSourceExecutionIdentifier() {
        return sourceExecutionIdentifier;
    }

    public String getOutputIdentifier() {
        return outputIdentifier;
    }

    public String getTargetExecutionIdentifier() {
        return targetExecutionIdentifier;
    }

    public String getInputIdentifier() {
        return inputIdentifier;
    }

}
