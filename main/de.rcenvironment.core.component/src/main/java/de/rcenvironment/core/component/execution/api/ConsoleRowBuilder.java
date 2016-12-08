/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.component.execution.impl.ConsoleRowImpl;

/**
 * Builds {@link ConsoleRow} objects.
 * 
 * @author Doreen Seider
 */
public class ConsoleRowBuilder {

    private final int timestampOffset;

    private final ConsoleRowImpl consoleRow;
    
    public ConsoleRowBuilder() {
        this(0);
    }
    
    public ConsoleRowBuilder(int timestampOffset) {
        this.timestampOffset = timestampOffset;
        consoleRow = new ConsoleRowImpl();
    }
    
    
    /**
     * @param workflowExecutionIdentifier execution identifier of the associated workflow
     * @param componentExecutionIdentifier execution identifier of the associated component
     * @return {@link ConsoleRowBuilder} instance for method chaining
     */
    public ConsoleRowBuilder setExecutionIdentifiers(String workflowExecutionIdentifier, String componentExecutionIdentifier) {
        consoleRow.setWorkflowIdentifier(workflowExecutionIdentifier);
        consoleRow.setComponentIdentifier(componentExecutionIdentifier);
        return this;
    }
    
    /**
     * @param workflowInstanceName instance name of the associated workflow
     * @param componentInstanceName instance name of the associated component
     * @return {@link ConsoleRowBuilder} instance for method chaining
     */
    public ConsoleRowBuilder setInstanceNames(String workflowInstanceName, String componentInstanceName) {
        consoleRow.setWorkflowName(workflowInstanceName);
        consoleRow.setComponentName(componentInstanceName);
        return this;
    }
    
    /**
     * @param type ConsoleRow.Type of the {@link ConsoleRow}
     * @return {@link ConsoleRowBuilder} instance for method chaining
     */
    public ConsoleRowBuilder setType(ConsoleRow.Type type) {
        consoleRow.setType(type);
        return this;
    }
    
    /**
     * @param payload payload of the {@link ConsoleRow}
     * @return {@link ConsoleRowBuilder} instance for method chaining
     */
    public ConsoleRowBuilder setPayload(String payload) {
        consoleRow.setPayload(payload);
        return this;
    }
    
    /**
     * @param sequenceNumber sequence number of the {@link ConsoleRow}
     * @return {@link ConsoleRowBuilder} instance for method chaining
     */
    public ConsoleRowBuilder setSequenceNumber(long sequenceNumber) {
        consoleRow.setSequenceNumber(sequenceNumber);
        return this;
    }
    
    /**
     * @param compRun component run the the {@link ConsoleRow} was generated
     * @return {@link ConsoleRowBuilder} instance for method chaining
     */
    public ConsoleRowBuilder setComponentRun(int compRun) {
        consoleRow.setComponentRun(compRun);
        return this;
    }
    
    /**
     * @return {@link ConsoleRow} instance
     */
    public ConsoleRow build() {
        consoleRow.setTimestamp(System.currentTimeMillis() + timestampOffset);
        return consoleRow;
    }
}
