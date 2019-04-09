/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Creates instances used when executing a workflow.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface WorkflowExecutionRelatedInstancesFactory {

    /**
     * Creates new instance of {@link ComponentDisconnectWatcher}.
     * 
     * @param wfExeCtx {@link WorkflowExecutionContext} of the workflow the new {@link ComponentDisconnectWatcher} instance is associated
     *        with
     * @param compStatesEntirelyChangedVerifier {@link ComponentStatesChangedEntirelyVerifier} of the workflow the new
     *        {@link ComponentDisconnectWatcher} instance is associated with
     * @return new instance of {@link ComponentDisconnectWatcher}
     */
    ComponentDisconnectWatcher createComponentLostWatcher(WorkflowExecutionContext wfExeCtx,
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

    /**
     * Creates a new {@link NodeRestartWatcher} instance.
     * 
     * @param wfExeCtx the {@link WorkflowExecutionContext}
     * @param compStatesEntirelyChangedVerifier the {@link ComponentStatesChangedEntirelyVerifier} to notify when components become
     *        permanently unavailable
     * @param serviceRegistryAccess the registry to fetch required services from
     * @return the new instance
     */
    NodeRestartWatcher createNodeRestartWatcher(WorkflowExecutionContext wfExeCtx,
        ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier, ServiceRegistryAccess serviceRegistryAccess);
}
