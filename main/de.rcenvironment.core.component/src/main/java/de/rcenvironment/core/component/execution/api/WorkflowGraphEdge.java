/*
 * Copyright (C) 2006-2016 DLR, Germany
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
 * @author Sascha Zur
 */
public class WorkflowGraphEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sourceExecutionIdentifier;

    private final String outputIdentifier;

    private final String targetExecutionIdentifier;

    private final String inputIdentifier;

    private final String outputEndpointType;

    private final String inputEndpointType;

    public WorkflowGraphEdge(String sourceExecutionIdentifier, String outputIdentifier, String outputEndpointType,
        String targetExecutionIdentifier, String inputIdentifier, String inputEndpointType) {
        this.sourceExecutionIdentifier = sourceExecutionIdentifier;
        this.outputIdentifier = outputIdentifier;
        this.outputEndpointType = outputEndpointType;
        this.targetExecutionIdentifier = targetExecutionIdentifier;
        this.inputIdentifier = inputIdentifier;
        this.inputEndpointType = inputEndpointType;
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

    public String getOutputEndpointType() {
        return outputEndpointType;
    }

    public String getInputEndpointType() {
        return inputEndpointType;
    }

}
