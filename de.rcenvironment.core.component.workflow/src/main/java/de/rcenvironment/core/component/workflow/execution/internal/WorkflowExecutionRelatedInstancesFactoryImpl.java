/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Default implementation of {@link WorkflowExecutionRelatedInstancesFactory}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowExecutionRelatedInstancesFactoryImpl implements WorkflowExecutionRelatedInstancesFactory {

    @Override
    public ComponentDisconnectWatcher createComponentLostWatcher(WorkflowExecutionContext wfExeCtx,
        ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier) {
        return new ComponentDisconnectWatcher(compStatesEntirelyChangedVerifier, wfExeCtx);
    }

    @Override
    public ComponentsConsoleLogFileWriter createComponentConsoleLogFileWriter(WorkflowExecutionStorageBridge wfDataStorageBridge) {
        return new ComponentsConsoleLogFileWriter(wfDataStorageBridge);
    }
    
    @Override
    public ComponentStatesChangedEntirelyVerifier createComponentStatesEntirelyChangedVerifier(int componentCount) {
        return new ComponentStatesChangedEntirelyVerifier(componentCount);
    }

    @Override
    public WorkflowExecutionStorageBridge createWorkflowExecutionStorageBridge(WorkflowExecutionContext wfExeCtx) {
        return new WorkflowExecutionStorageBridge(wfExeCtx);
    }

    @Override
    public WorkflowStateMachine createWorkflowStateMachine(WorkflowStateMachineContext wfStateMachineCtx) {
        return new WorkflowStateMachine(wfStateMachineCtx);
    }

    @Override
    public NodeRestartWatcher createNodeRestartWatcher(WorkflowExecutionContext wfExeCtx,
        ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier, ServiceRegistryAccess serviceRegistryAccess) {
        return new NodeRestartWatcher(compStatesEntirelyChangedVerifier, wfExeCtx, serviceRegistryAccess);
    }

}
