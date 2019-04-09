/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.EndpointCharacter;

/**
 * Represents an edge in the workflow graph.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class WorkflowGraphEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ComponentExecutionIdentifier sourceExecutionIdentifier;

    private final String outputIdentifier;

    private final ComponentExecutionIdentifier targetExecutionIdentifier;

    private final String inputIdentifier;

    private final EndpointCharacter outputEndpointCharacter;

    private final EndpointCharacter inputEndpointCharacter;

    public WorkflowGraphEdge(ComponentExecutionIdentifier sourceExecutionIdentifier, String outputIdentifier,
        EndpointCharacter outputEndpointCharacter, ComponentExecutionIdentifier targetExecutionIdentifier, String inputIdentifier,
        EndpointCharacter inputEndpointCharacter) {
        this.sourceExecutionIdentifier = sourceExecutionIdentifier;
        this.outputIdentifier = outputIdentifier;
        this.outputEndpointCharacter = outputEndpointCharacter;
        this.targetExecutionIdentifier = targetExecutionIdentifier;
        this.inputIdentifier = inputIdentifier;
        this.inputEndpointCharacter = inputEndpointCharacter;
    }

    public ComponentExecutionIdentifier getSourceExecutionIdentifier() {
        return sourceExecutionIdentifier;
    }

    public String getOutputIdentifier() {
        return outputIdentifier;
    }

    public ComponentExecutionIdentifier getTargetExecutionIdentifier() {
        return targetExecutionIdentifier;
    }

    public String getInputIdentifier() {
        return inputIdentifier;
    }

    public EndpointCharacter getOutputCharacter() {
        return outputEndpointCharacter;
    }

    /**
     * @return The {@link EndpointCharacter} of the target's input.
     */
    public EndpointCharacter getInputCharacter() {
        return inputEndpointCharacter;
    }

}
