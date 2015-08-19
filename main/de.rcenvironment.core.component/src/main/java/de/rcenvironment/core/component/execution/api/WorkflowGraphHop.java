/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

/**
 * Represents an hop in a cyclic workflow graph to traverse.
 * 
 * @author Doreen Seider
 */
public class WorkflowGraphHop {
    
    private final String executionIdentifier;
    
    private final String ouputName;
    
    private final String targetExecutionIdentifier;
    
    private final String targetInputName;

    public WorkflowGraphHop(String hopExecutionIdentifier, String hopOuputName, String targetExecutionIdentifier, String targetInputName) {
        this.executionIdentifier = hopExecutionIdentifier;
        this.ouputName = hopOuputName;
        this.targetExecutionIdentifier = targetExecutionIdentifier;
        this.targetInputName = targetInputName;
    }
    
    public String getHopExecutionIdentifier() {
        return executionIdentifier;
    }
    
    public String getHopOuputName() {
        return ouputName;
    }
    
    public String getTargetExecutionIdentifier() {
        return targetExecutionIdentifier;
    }
    
    public String getTargetInputName() {
        return targetInputName;
    }
    
    @Override
    public String toString() {
        return String.format("%s@%s -> %s@%s", ouputName, executionIdentifier, targetInputName, targetExecutionIdentifier);
    }
    
}
