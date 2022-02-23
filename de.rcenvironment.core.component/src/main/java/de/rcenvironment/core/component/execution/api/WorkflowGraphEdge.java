/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (inputEndpointCharacter == null) {
            result = prime * result;
        } else {
            result = prime * result + inputEndpointCharacter.hashCode();
        }

        if (inputIdentifier == null) {
            result = prime * result;
        } else {
            result = prime * result + inputIdentifier.hashCode();
        }

        if (outputEndpointCharacter == null) {
            result = prime * result;
        } else {
            result = prime * result + outputEndpointCharacter.hashCode();
        }

        if (outputIdentifier == null) {
            result = prime * result;
        } else {
            result = prime * result + outputIdentifier.hashCode();
        }

        if (sourceExecutionIdentifier == null) {
            result = prime * result;
        } else {
            result = prime * result + sourceExecutionIdentifier.hashCode();
        }

        if (targetExecutionIdentifier == null) {
            result = prime * result;
        } else {
            result = prime * result + targetExecutionIdentifier.hashCode();
        }
        
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WorkflowGraphEdge)) {
            return false;
        }
        WorkflowGraphEdge other = (WorkflowGraphEdge) obj;
        if (inputEndpointCharacter != other.inputEndpointCharacter) {
            return false;
        }
        if (inputIdentifier == null) {
            if (other.inputIdentifier != null) {
                return false;
            }
        } else if (!inputIdentifier.equals(other.inputIdentifier)) {
            return false;
        }
        if (outputEndpointCharacter != other.outputEndpointCharacter) {
            return false;
        }
        if (outputIdentifier == null) {
            if (other.outputIdentifier != null) {
                return false;
            }
        } else if (!outputIdentifier.equals(other.outputIdentifier)) {
            return false;
        }
        if (sourceExecutionIdentifier == null) {
            if (other.sourceExecutionIdentifier != null) {
                return false;
            }
        } else if (!sourceExecutionIdentifier.equals(other.sourceExecutionIdentifier)) {
            return false;
        }
        if (targetExecutionIdentifier == null) {
            if (other.targetExecutionIdentifier != null) {
                return false;
            }
        } else if (!targetExecutionIdentifier.equals(other.targetExecutionIdentifier)) {
            return false;
        }
        return true;
    }
}
