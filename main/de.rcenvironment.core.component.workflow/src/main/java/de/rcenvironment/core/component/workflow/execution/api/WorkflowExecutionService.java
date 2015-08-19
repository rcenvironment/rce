/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Set;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Manages workflow executions.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionService {

    /**
     * Executes a workflow represented by its {@link WorkflowExecutionContext}.
     * 
     * @param executionContext workflow's {@link WorkflowExecutionContext}
     * @return {@link WorkflowExecutionInformation} of the instantiated and running workflow
     * @throws WorkflowExecutionException if starting the workflow failed
     * @throws CommunicationException if communication error occurs (cannot occur if controller and components run locally)
     */
    WorkflowExecutionInformation execute(WorkflowExecutionContext executionContext) throws WorkflowExecutionException,
        CommunicationException;

    /**
     * Triggers workflow to cancel.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to cancel
     * @param node the node of the workflow controller
     * @throws CommunicationException if communication error occurs (cannot occur if controller and components run locally)
     */
    void cancel(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * Triggers workflow to pause.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to cancel
     * @param node the node of the workflow controller
     * @throws CommunicationException if communication error occurs (cannot occur if controller and components run locally)
     */
    void pause(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * Triggers workflow to resume when paused.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to cancel
     * @param node the node of the workflow controller
     * @throws CommunicationException if communication error occurs (cannot occur if controller and components run locally)
     */
    void resume(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * Triggers workflow to dispose.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to cancel
     * @param node the node of the workflow controller
     * @throws CommunicationException if communication error occurs (cannot occur if controller and components run locally)
     */
    void dispose(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * Gets current workflow state.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to get the state for
     * @param node the node of the workflow controller
     * @return {@link WorkflowState}
     * @throws CommunicationException if communication error occurs (cannot occur if controller and components run locally)
     */
    WorkflowState getWorkflowState(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * @return {@link WorkflowExecutionInformation} objects of all active and local workflows
     */
    Set<WorkflowExecutionInformation> getLocalWorkflowExecutionInformations();
    
    /**
     * @return {@link WorkflowExecutionInformation} objects of all active workflows
     */
    Set<WorkflowExecutionInformation> getWorkflowExecutionInformations();
    
    /**
     * @param forceRefresh <code>true</code> if the cache of {@link WorkflowExecutionInformation}s shall be refreshed, <code>false</code>
     *        otherwise
     * @return {@link WorkflowExecutionInformation} objects of all active workflows
     */
    Set<WorkflowExecutionInformation> getWorkflowExecutionInformations(boolean forceRefresh);

}
