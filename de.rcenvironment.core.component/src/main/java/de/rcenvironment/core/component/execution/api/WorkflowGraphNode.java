/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Represents a node in the {@link WorkflowGraph}.
 * 
 * @author Doreen Seider
 */
public class WorkflowGraphNode implements Serializable {

    private static final long serialVersionUID = 272922098023592460L;

    private final ComponentExecutionIdentifier executionIdentifier;
    
    // TODO this should be replaced with WorkflowNodeIdentifier, which is currently not possible, since the WorkflowGraph is in the
    // Components package
    private final String wfNodeId;

    private final Set<String> inputIdentifiers;

    private final Set<String> outputIdentifiers;

    private final Map<String, String> endpointNames;

    private final boolean isDriver;
    
    private final boolean isDrivingFaultTolerantLoop;
    
    private final String name;
    
    // TODO this constructor is never used. remove?
    public WorkflowGraphNode(String wfNodeId, ComponentExecutionIdentifier compExeId, Set<String> inputIdentifiers,
        Set<String> outputIdentifiers, Map<String, String> endpointNames, boolean isDriver, boolean isDrivingFaultTolerantLoop) {
        this(wfNodeId, compExeId, inputIdentifiers, outputIdentifiers, endpointNames, isDriver, isDrivingFaultTolerantLoop,
            compExeId.toString());
    }

    public WorkflowGraphNode(String wfNodeId, ComponentExecutionIdentifier compExeId, Set<String> inputIdentifiers,
        Set<String> outputIdentifiers, Map<String, String> endpointNames, boolean isDriver, boolean isDrivingFaultTolerantLoop,
        String name) {
        this.wfNodeId = wfNodeId;
        this.executionIdentifier = compExeId;
        this.inputIdentifiers = inputIdentifiers;
        this.outputIdentifiers = outputIdentifiers;
        this.endpointNames = endpointNames;
        this.isDriver = isDriver;
        this.isDrivingFaultTolerantLoop = isDrivingFaultTolerantLoop;
        this.name = name;
    }

    public ComponentExecutionIdentifier getExecutionIdentifier() {
        return executionIdentifier;
    }
    
    public String getWorkflowNodeIdentifier() {
        return this.wfNodeId;
    }

    public Set<String> getInputIdentifiers() {
        return inputIdentifiers;
    }

    public Set<String> getOutputIdentifiers() {
        return outputIdentifiers;
    }

    public boolean isDriver() {
        return isDriver;
    }
    
    public boolean isDrivingFaultTolerantLoop() {
        return isDrivingFaultTolerantLoop;
    }
    
    /**
     * @param endpointIdentifier identifier of endpoint to get the name for
     * @return name of the endpoint or <code>null</code> if identifier not known
     */
    public String getEndpointName(String endpointIdentifier) {
        return endpointNames.get(endpointIdentifier);
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return StringUtils.format("%s (driver: %b)", getExecutionIdentifier(), isDriver());
    }

}
