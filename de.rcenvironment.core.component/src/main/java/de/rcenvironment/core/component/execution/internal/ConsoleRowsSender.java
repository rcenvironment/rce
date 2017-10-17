/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.ConsoleRowBuilder;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Sends console rows for certain events and does some related event handling.
 *
 * @author Doreen Seider
 */
public class ConsoleRowsSender {
    
    private final ComponentExecutionRelatedInstances compExeRelatedInstances;
    
    private final Object logMessageLock = new Object();
    
    protected ConsoleRowsSender(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        this.compExeRelatedInstances = compExeRelatedInstances;
    }
    
    protected synchronized void sendLogFileWriteTriggerAsConsoleRow() {
        String payload = StringUtils.escapeAndConcat(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_LOG_FINISHED.name(),
            String.valueOf(compExeRelatedInstances.compExeStorageBridge.getComponentExecutionDataManagementId()),
            String.valueOf(compExeRelatedInstances.compExeRelatedStates.executionCount.get()));
        synchronized (logMessageLock) {
            sendConsoleRow(Type.LIFE_CYCLE_EVENT, payload);
            compExeRelatedInstances.compExeRelatedStates.consoleRowSequenceNumber.set(0);
            compExeRelatedInstances.compExeRelatedStates.compHasSentConsoleRowLogMessages.set(false);
        }
    }

    protected void sendLogMessageAsConsoleRow(Type consoleRowType, String message, int compRun) {
        ConsoleRowBuilder consoleRowBuilder = createConsoleRowBuilder();
        consoleRowBuilder.setType(consoleRowType).setPayload(message).setComponentRun(compRun);
        synchronized (logMessageLock) {
            consoleRowBuilder.setSequenceNumber(compExeRelatedInstances.compExeRelatedStates.consoleRowSequenceNumber.incrementAndGet());
            compExeRelatedInstances.batchingConsoleRowsForwarder.onConsoleRow(consoleRowBuilder.build());
            compExeRelatedInstances.compExeRelatedStates.compHasSentConsoleRowLogMessages.set(true); 
        }    
    }
    
    protected void sendStateAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType type) {
        ConsoleRowBuilder consoleRowBuilder = createConsoleRowBuilder();
        consoleRowBuilder.setPayload(type.name());
        compExeRelatedInstances.batchingConsoleRowsForwarder.onConsoleRow(consoleRowBuilder.build());
    }
    
    protected void sendTimelineEventAsConsoleRow(ConsoleRow.Type consoleRowType, String payload) {
        payload = StringUtils.escapeAndConcat(payload, String.valueOf(compExeRelatedInstances.compExeStorageBridge
                    .getComponentExecutionDataManagementId()));
        sendConsoleRow(consoleRowType, payload);
    }
    
    private void sendConsoleRow(Type consoleRowType, String payload) {
        ConsoleRowBuilder consoleRowBuilder = createConsoleRowBuilder();
        consoleRowBuilder.setType(consoleRowType).setPayload(payload);
        compExeRelatedInstances.batchingConsoleRowsForwarder.onConsoleRow(consoleRowBuilder.build());
    }
    
    private ConsoleRowBuilder createConsoleRowBuilder() {
        ConsoleRowBuilder consoleRowBuilder = new ConsoleRowBuilder(compExeRelatedInstances.timestampOffsetToWorkfowNode);
        consoleRowBuilder.setExecutionIdentifiers(compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier(),
            compExeRelatedInstances.compExeCtx.getExecutionIdentifier())
            .setInstanceNames(compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                compExeRelatedInstances.compExeCtx.getInstanceName())
            .setType(ConsoleRow.Type.LIFE_CYCLE_EVENT);
        return consoleRowBuilder;
    }
    
}
