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
 * Context information used by {@link WorkflowStateMachine}.
 * 
 * @author Doreen Seider
 */
public class WorkflowStateMachineContext {
    
    private final WorkflowExecutionContext workflowExecutionContext;
    
    private final WorkflowExecutionStorageBridge workflowExecutionStorageBridge;
    
    private final ComponentStatesChangedEntirelyVerifier componentStatesChangedEntirelyVerifier;
    
    private final ComponentsConsoleLogFileWriter componentsConsoleLogFileWriter;
    
    private final ComponentLostWatcher componentLostWatcher;
    
    public WorkflowStateMachineContext(WorkflowExecutionContext wfExeCtx, WorkflowExecutionStorageBridge workflowExecutionStorageBridge,
        ComponentStatesChangedEntirelyVerifier compChangedVerifier, ComponentsConsoleLogFileWriter componentsConsoleLogFileWriter,
        ComponentLostWatcher componentLostWatcher) {
        this.workflowExecutionContext = wfExeCtx;
        this.workflowExecutionStorageBridge = workflowExecutionStorageBridge;
        this.componentStatesChangedEntirelyVerifier = compChangedVerifier;
        this.componentsConsoleLogFileWriter = componentsConsoleLogFileWriter;
        this.componentLostWatcher = componentLostWatcher;
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
    
    public ComponentLostWatcher getComponentLostWatcher() {
        return componentLostWatcher;
    }

}
