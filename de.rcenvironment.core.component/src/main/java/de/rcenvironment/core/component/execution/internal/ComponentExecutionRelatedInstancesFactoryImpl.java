/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.execution.api.BatchingConsoleRowsForwarder;

/**
 * Default implementation of {@link ComponentExecutionRelatedInstancesFactory}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionRelatedInstancesFactoryImpl implements ComponentExecutionRelatedInstancesFactory {

    @Override
    public ComponentExecutionStorageBridge createComponentExecutionStorageBridge(
        ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return new ComponentExecutionStorageBridge(compExeRelatedInstances);
    }

    @Override
    public ComponentStateMachine createComponentStateMachine(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return new ComponentStateMachine(compExeRelatedInstances);
    }

    @Override
    public ComponentExecutionScheduler createComponentExecutionScheduler(
        ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return new ComponentExecutionScheduler(compExeRelatedInstances);
    }

    @Override
    public ComponentContextBridge createComponentContextBridge(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return new ComponentContextBridge(compExeRelatedInstances);
    }

    @Override
    public TypedDatumToOutputWriter createTypedDatumToOutputWriter(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return new TypedDatumToOutputWriter(compExeRelatedInstances);
    }

    @Override
    public ConsoleRowsSender createConsoleRowsSender(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return new ConsoleRowsSender(compExeRelatedInstances);
    }

    @Override
    public BatchingConsoleRowsForwarder createBatchingConsoleRowsForwarder(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return new BatchingConsoleRowsForwarder(compExeRelatedInstances.wfExeCtrlBridgeDelegator);
    }

    @Override
    public WorkflowExecutionControllerBridgeDelegator createWorkflowExecutionControllerBridgeDelegator(
        ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return new WorkflowExecutionControllerBridgeDelegator(compExeRelatedInstances);
    }

}
