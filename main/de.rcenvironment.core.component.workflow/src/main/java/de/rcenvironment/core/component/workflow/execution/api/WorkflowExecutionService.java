/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;
import java.util.Set;

import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.execution.api.ExecutionController;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Manages workflow executions.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (added fail-on-update flag)
 */
public interface WorkflowExecutionService {

    /**
     * Loads {@link WorkflowDescription} from a {@link File}. It checks for updates and perform updates if needed.
     * 
     * @param wfFile the worflow file to load the {@link WorkflowDescription} from
     * @param callback a {@link WorkflowDescriptionLoaderCallback} to announce certain events during loading
     * @return {@link WorkflowDescription}
     * @throws WorkflowFileException if loading the {@link WorkflowDescription} from the file failed
     */
    WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException;

    /**
     * Loads {@link WorkflowDescription} from a {@link File}. It checks for updates and perform updates if needed.
     * 
     * @param wfFile the worflow file to load the {@link WorkflowDescription} from
     * @param callback a {@link WorkflowDescriptionLoaderCallback} to announce certain events during loading
     * @param abortIfWorkflowUpdateRequired whether a required workflow update should be considered an error
     * @return {@link WorkflowDescription}
     * @throws WorkflowFileException if loading the {@link WorkflowDescription} from the file failed
     */
    WorkflowDescription loadWorkflowDescriptionFromFileConsideringUpdates(File wfFile, WorkflowDescriptionLoaderCallback callback,
        boolean abortIfWorkflowUpdateRequired) throws WorkflowFileException;

    /**
     * Loads {@link WorkflowDescription} from a {@link File}. It _doesn't_ check for updates and _doesn't_ perform updates at all.
     * 
     * @param wfFile the worflow file to load the {@link WorkflowDescription} from
     * @param callback a {@link WorkflowDescriptionLoaderCallback} to announce certain events during loading
     * @return {@link WorkflowDescription}
     * @throws WorkflowFileException if loading the {@link WorkflowDescription} from the file failed
     */
    WorkflowDescription loadWorkflowDescriptionFromFile(File wfFile, WorkflowDescriptionLoaderCallback callback)
        throws WorkflowFileException;

    /**
     * Checks if the components of the workflow are installed on the configured target nodes and if the configured controller node is
     * available.
     * 
     * @param workflowDescription {@link WorkflowDescription} to validate
     * @return {@link WorkflowDescriptionValidationResult}
     */
    WorkflowDescriptionValidationResult validateWorkflowDescription(WorkflowDescription workflowDescription);

    /**
     * Executes a workflow represented by its {@link WorkflowExecutionContext}.
     * 
     * @param executionContext workflow's {@link WorkflowExecutionContext}
     * @return {@link WorkflowExecutionInformation} of the instantiated and running workflow
     * @throws WorkflowExecutionException if starting the workflow failed
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     */
    WorkflowExecutionInformation executeWorkflowAsync(WorkflowExecutionContext executionContext) throws WorkflowExecutionException,
        RemoteOperationException;

    /**
     * Triggers workflow to cancel.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to cancel
     * @param node the node of the workflow controller
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void cancel(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Triggers workflow to pause.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to cancel
     * @param node the node of the workflow controller
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void pause(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Triggers workflow to resume when paused.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to cancel
     * @param node the node of the workflow controller
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void resume(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Triggers workflow to dispose.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to cancel
     * @param node the node of the workflow controller
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void dispose(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Gets current workflow state.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to get the state for
     * @param node the node of the workflow controller
     * @return {@link WorkflowState}
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    WorkflowState getWorkflowState(String executionId, ResolvableNodeId node) throws ExecutionControllerException,
        RemoteOperationException;

    /**
     * Gets current workflow state.
     * 
     * @param executionId execution identifier (part of {@link WorkflowExecutionInformation}) of the workflow to get the state for
     * @param node the node of the workflow controller
     * @return {@link WorkflowState}
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    Long getWorkflowDataManagementId(String executionId, ResolvableNodeId node) throws ExecutionControllerException,
        RemoteOperationException;

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
