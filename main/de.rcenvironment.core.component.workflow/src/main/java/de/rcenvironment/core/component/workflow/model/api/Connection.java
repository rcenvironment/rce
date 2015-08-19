/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.Serializable;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;

/**
 * Connection class for connecting {@link ComponentDescription}s within a
 * {@link WorkflowDescription}.
 * 
 * @author Roland Gude
 * @author Heinrich Wendel
 */
public class Connection implements Serializable {

    private static final long serialVersionUID = 6019856436149503867L;
    
    /** The source {@link WorkflowNode}. */
    private final WorkflowNode sourceWorkflowNode;
    
    /** The output of the source {@link WorkflowNode}. */
    private final EndpointDescription outputEndpointDescription;
    
    /** The target {@link WorkflowNode}. */
    private final WorkflowNode targetWorkflowNode;
    
    /**  The input of the target  {@link WorkflowNode}. */
    private final EndpointDescription inputEndpointDescription;

    public Connection(WorkflowNode source, EndpointDescription output, WorkflowNode target, EndpointDescription input) {
        this.sourceWorkflowNode = source;
        this.outputEndpointDescription = output;
        this.targetWorkflowNode = target;
        this.inputEndpointDescription = input;
    }
    
    public WorkflowNode getSourceNode() {
        return sourceWorkflowNode;
    }

    public EndpointDescription getOutput() {
        return outputEndpointDescription;
    }

    public WorkflowNode getTargetNode() {
        return targetWorkflowNode;
    }

    public EndpointDescription getInput() {
        return inputEndpointDescription;
    }
    
    @Override
    public boolean equals(Object o) {
        boolean equals = false;
        if (o instanceof Connection) {
            Connection c = (Connection) o;
            if (c.getTargetNode().getIdentifier().equals(targetWorkflowNode.getIdentifier())
                && c.getSourceNode().getIdentifier().equals(sourceWorkflowNode.getIdentifier())
                && inputEndpointDescription.getIdentifier().equals(c.getInput().getIdentifier())
                && outputEndpointDescription.getIdentifier().equals(c.getOutput().getIdentifier())) {
                equals = true;
            }
        }
        return equals;
    }
    
    @Override
    public int hashCode() {
        StringBuilder builder = new StringBuilder();
        builder.append(targetWorkflowNode.getIdentifier());
        builder.append(sourceWorkflowNode.getIdentifier());
        builder.append(inputEndpointDescription.getIdentifier());
        builder.append(outputEndpointDescription.getIdentifier());
        return builder.toString().hashCode();
    }
}
