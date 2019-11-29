/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Context information used by {@link WorkflowStateMachine}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowStateMachineContext {

    private final WorkflowExecutionContext workflowExecutionContext;

    private final WorkflowExecutionStorageBridge workflowExecutionStorageBridge;

    private final ComponentStatesChangedEntirelyVerifier componentStatesChangedEntirelyVerifier;

    private final ComponentsConsoleLogFileWriter componentsConsoleLogFileWriter;

    private final ComponentDisconnectWatcher componentLostWatcher;

    private final NodeRestartWatcher nodeRestartWatcher;

    private ServiceRegistryAccess serviceRegistryAccess;

    public WorkflowStateMachineContext(WorkflowExecutionContext wfExeCtx, WorkflowExecutionStorageBridge workflowExecutionStorageBridge,
        ComponentStatesChangedEntirelyVerifier compChangedVerifier, ComponentsConsoleLogFileWriter componentsConsoleLogFileWriter,
        ComponentDisconnectWatcher componentLostWatcher, NodeRestartWatcher nodeRestartWatcher,
        ServiceRegistryAccess serviceRegistryAccess) {
        this.workflowExecutionContext = wfExeCtx;
        this.workflowExecutionStorageBridge = workflowExecutionStorageBridge;
        this.componentStatesChangedEntirelyVerifier = compChangedVerifier;
        this.componentsConsoleLogFileWriter = componentsConsoleLogFileWriter;
        this.componentLostWatcher = componentLostWatcher;
        this.nodeRestartWatcher = nodeRestartWatcher;
        this.serviceRegistryAccess = serviceRegistryAccess;
    }

    public WorkflowExecutionContext getWorkflowExecutionContext() {
        return workflowExecutionContext;
    }

    public WorkflowExecutionStorageBridge getWorkflowExecutionStorageBridge() {
        return workflowExecutionStorageBridge;
    }

    public ComponentStatesChangedEntirelyVerifier getComponentStatesChangedEntirelyVerifier() {
        return componentStatesChangedEntirelyVerifier;
    }

    public ComponentsConsoleLogFileWriter getComponentsConsoleLogFileWriter() {
        return componentsConsoleLogFileWriter;
    }

    public ComponentDisconnectWatcher getComponentLostWatcher() {
        return componentLostWatcher;
    }

    public NodeRestartWatcher getNodeRestartWatcher() {
        return nodeRestartWatcher;
    }

    public ServiceRegistryAccess getServiceRegistryAccess() {
        return serviceRegistryAccess;
    }
}
