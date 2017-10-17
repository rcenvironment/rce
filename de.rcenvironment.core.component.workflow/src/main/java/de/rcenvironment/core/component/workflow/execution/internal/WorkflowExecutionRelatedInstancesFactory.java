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
 * Creates instances used when executing a workflow.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionRelatedInstancesFactory {

    /**
     * Creates new instance of {@link ComponentLostWatcher}.
     * 
     * @param wfExeCtx {@link WorkflowExecutionContext} of the workflow the new {@link ComponentLostWatcher} instance is associated with
     * @param compStatesEntirelyChangedVerifier {@link ComponentStatesChangedEntirelyVerifier} of the workflow the new
     *        {@link ComponentLostWatcher} instance is associated with
     * @return new instance of {@link ComponentLostWatcher}
     */
    ComponentLostWatcher createComponentLostWatcher(WorkflowExecutionContext wfExeCtx,
        ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier);

    /**
     * Creates {@link ComponentsConsoleLogFileWriter} instance, which handles the console log files of all the given components.
     * 
     * @param wfExeStorageBridge {@link WorkflowExecutionStorageBridge} instance related to the components' workflow
     * @return new {@link ComponentsConsoleLogFileWriter} instance
     */
    ComponentsConsoleLogFileWriter createComponentConsoleLogFileWriter(WorkflowExecutionStorageBridge wfExeStorageBridge);

    /**
     * Creates instances of {@link ComponentStatesChangedEntirelyVerifier}.
     * 
     * @param componentCount amount of components in the workflow
     * @return newly created instance of {@link ComponentStatesChangedEntirelyVerifier}
     */
    ComponentStatesChangedEntirelyVerifier createComponentStatesEntirelyChangedVerifier(int componentCount);

    /**
     * Creates new instance of {@link WorkflowExecutionStorageBridge}.
     * 
     * @param wfExeCtx {@link WorkflowExecutionContext} of the workflow the new {@link WorkflowExecutionStorageBridge} instance is
     *        associated with
     * @return new instance of {@link WorkflowExecutionStorageBridge}
     */
    WorkflowExecutionStorageBridge createWorkflowExecutionStorageBridge(WorkflowExecutionContext wfExeCtx);

    /**
     * Creates new instance of {@link WorkflowStateMachine}.
     * 
     * @param wfStateMachineCtx {@link WorkflowStateMachineContext} that belongs to the workflow the new {@link WorkflowStateMachine}
     *        instance is associated with
     * @return new instance of {@link WorkflowStateMachine}
     */
    WorkflowStateMachine createWorkflowStateMachine(WorkflowStateMachineContext wfStateMachineCtx);
}
