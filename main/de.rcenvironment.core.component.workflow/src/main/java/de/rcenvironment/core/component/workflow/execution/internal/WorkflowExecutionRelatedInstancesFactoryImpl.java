/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;

/**
 * Default implementation of {@link WorkflowExecutionRelatedInstancesFactory}.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionRelatedInstancesFactoryImpl implements WorkflowExecutionRelatedInstancesFactory {

    @Override
    public ComponentLostWatcher createComponentLostWatcher(WorkflowExecutionContext wfExeCtx,
        ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier) {
        return new ComponentLostWatcher(compStatesEntirelyChangedVerifier, wfExeCtx);
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

}
