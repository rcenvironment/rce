/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import de.rcenvironment.core.component.execution.api.BatchingConsoleRowsForwarder;
import de.rcenvironment.core.component.execution.internal.ComponentContextBridge;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionRelatedInstances;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionRelatedInstancesFactory;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionScheduler;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionStorageBridge;
import de.rcenvironment.core.component.execution.internal.ComponentStateMachine;
import de.rcenvironment.core.component.execution.internal.ConsoleRowsSender;
import de.rcenvironment.core.component.execution.internal.TypedDatumToOutputWriter;
import de.rcenvironment.core.component.execution.internal.WorkflowExecutionControllerBridgeDelegator;

/**
 * Default stub implementation of {@link ComponentExecutionRelatedInstancesFactory}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionRelatedInstancesFactoryDefaultStub implements ComponentExecutionRelatedInstancesFactory {

    @Override
    public ComponentExecutionStorageBridge createComponentExecutionStorageBridge(
        ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return null;
    }

    @Override
    public ComponentStateMachine createComponentStateMachine(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return null;
    }

    @Override
    public ComponentExecutionScheduler createComponentExecutionScheduler(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return null;
    }

    @Override
    public ComponentContextBridge createComponentContextBridge(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return null;
    }

    @Override
    public TypedDatumToOutputWriter createTypedDatumToOutputWriter(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return null;
    }

    @Override
    public ConsoleRowsSender createConsoleRowsSender(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return null;
    }

    @Override
    public BatchingConsoleRowsForwarder createBatchingConsoleRowsForwarder(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return null;
    }

    @Override
    public WorkflowExecutionControllerBridgeDelegator createWorkflowExecutionControllerBridgeDelegator(
        ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return null;
    }

}
