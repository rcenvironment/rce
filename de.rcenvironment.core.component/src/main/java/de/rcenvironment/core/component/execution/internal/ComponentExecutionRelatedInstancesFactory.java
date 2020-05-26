/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.execution.api.BatchingConsoleRowsForwarder;
import de.rcenvironment.core.component.execution.api.Component;

/**
 * Creates instances used when executing a {@link Component}.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionRelatedInstancesFactory {

    /**
     * Creates new instance of {@link ComponentExecutionStorageBridge} and adds them to given {@link ComponentExecutionRelatedInstances}.
     * 
     * @param compExeRelatedInstances {@link ComponentExecutionRelatedInstances} of the component the new
     *        {@link ComponentExecutionStorageBridge} instance is associated with
     * @return new instance of {@link ComponentExecutionStorageBridge} added to given {@link ComponentExecutionRelatedInstances}
     */
    ComponentExecutionStorageBridge createComponentExecutionStorageBridge(ComponentExecutionRelatedInstances compExeRelatedInstances);

    /**
     * Creates new instance of {@link ComponentStateMachine}.
     * 
     * @param compExeRelatedInstances {@link ComponentExecutionRelatedInstances} of the component the new {@link ComponentStateMachine}
     *        instance is associated with
     * @return new instance of {ComponentStateMachine}
     */
    ComponentStateMachine createComponentStateMachine(ComponentExecutionRelatedInstances compExeRelatedInstances);

    /**
     * Creates new instance of {@link ComponentExecutionScheduler}.
     * 
     * @param compExeRelatedInstances {@link ComponentExecutionRelatedInstances} of the component the new
     *        {@link ComponentExecutionScheduler} instance is associated with
     * @return new instance of {@link ComponentExecutionScheduler}
     */
    ComponentExecutionScheduler createComponentExecutionScheduler(ComponentExecutionRelatedInstances compExeRelatedInstances);

    /**
     * Creates new instance of {@link ComponentContextBridge}.
     * 
     * @param compExeRelatedInstances {@link ComponentExecutionRelatedInstances} of the component the new {@link ComponentContextBridge}
     *        instance is associated with
     * @return new instance of {ComponentContextBridge}
     */
    ComponentContextBridge createComponentContextBridge(ComponentExecutionRelatedInstances compExeRelatedInstances);

    /**
     * Creates new instance of {@link TypedDatumToOutputWriter}.
     * 
     * @param compExeRelatedInstances {@link ComponentExecutionRelatedInstances} of the component the new {@link TypedDatumToOutputWriter}
     *        instance is associated with
     * @return new instance of {TypedDatumToOutputWriter}
     */
    TypedDatumToOutputWriter createTypedDatumToOutputWriter(ComponentExecutionRelatedInstances compExeRelatedInstances);

    /**
     * Creates new instance of {@link ConsoleRowsSender}.
     * 
     * @param compExeRelatedInstances {@link ComponentExecutionRelatedInstances} of the component the new {@link ConsoleRowsSender} instance
     *        is associated with
     * @return new instance of {ConsoleRowsSender}
     */
    ConsoleRowsSender createConsoleRowsSender(ComponentExecutionRelatedInstances compExeRelatedInstances);

    /**
     * Creates new instance of {@link BatchingConsoleRowsForwarder}.
     * 
     * @param compExeRelatedInstances {@link ComponentExecutionRelatedInstances} of the component the new
     *        {@link BatchingConsoleRowsForwarder} instance is associated with
     * @return new instance of {BatchingConsoleRowsForwarder
     */
    BatchingConsoleRowsForwarder createBatchingConsoleRowsForwarder(ComponentExecutionRelatedInstances compExeRelatedInstances);

    /**
     * Creates new instance of {@link WorkflowExecutionControllerBridgeDelegator}.
     * 
     * @param compExeRelatedInstances {@link ComponentExecutionRelatedInstances} of the component the new
     *        {@link WorkflowExecutionControllerBridgeDelegator} instance is associated with
     * @return new instance of {WorkflowExecutionControllerBridgeDelegator}
     */
    WorkflowExecutionControllerBridgeDelegator createWorkflowExecutionControllerBridgeDelegator(
        ComponentExecutionRelatedInstances compExeRelatedInstances);

}
