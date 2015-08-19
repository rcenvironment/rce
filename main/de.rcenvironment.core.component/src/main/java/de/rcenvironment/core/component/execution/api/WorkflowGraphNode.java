/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Represents a node in the {@link WorkflowGraph}.
 * 
 * @author Doreen Seider
 */
public class WorkflowGraphNode implements Serializable {

    private static final long serialVersionUID = 272922098023592460L;

    private final String executionIdentifier;

    private final Set<String> inputIdentifiers;

    private final Set<String> outputIdentifiers;
    
    private final Map<String, String> endpointNames;
    
    private final boolean isResetSink;

    public WorkflowGraphNode(String nodeId, Set<String> inputIdentifiers, Set<String> outputIdentifiers,
        Map<String, String> endpointNames, boolean isResetSink) {
        this.executionIdentifier = nodeId;
        this.inputIdentifiers = inputIdentifiers;
        this.outputIdentifiers = outputIdentifiers;
        this.endpointNames = endpointNames;
        this.isResetSink = isResetSink;
    }

    public String getExecutionIdentifier() {
        return executionIdentifier;
    }

    public Set<String> getInputIdentifiers() {
        return inputIdentifiers;
    }

    public Set<String> getOutputIdentifiers() {
        return outputIdentifiers;
    }
    
    public boolean isResetSink() {
        return isResetSink;
    }
    
    /**
     * @param endpointIdentifier identifier of endpoint to get the name for
     * @return name of the endpoint or <code>null</code> if identifier not known
     */
    public String getEndpointName(String endpointIdentifier) {
        return endpointNames.get(endpointIdentifier);
    }

}
