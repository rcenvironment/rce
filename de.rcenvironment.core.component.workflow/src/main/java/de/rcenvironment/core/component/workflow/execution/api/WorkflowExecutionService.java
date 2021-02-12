/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.component.execution.api.ExecutionController;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Provides the main entry method for executing a workflow, and various operations that control a workflow's life-cycle.
 * 
 * Note: Loading {@link WorkflowDescription} evolved over time. Especially, adding robustness and the fact that there are slight differences
 * when loaded in editor compared to loading prior to headless workflow execution result in multiple methods dealing with loading
 * {@link WorkflowDescription} which I'm not happy with. --seid_do
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface WorkflowExecutionService extends PersistentWorkflowDescriptionLoaderService {

    /**
     * Checks if the components of the workflow are installed on the configured target nodes and if the configured controller node is
     * available. Note that this check is based on the network knowledge of the local node, not the controller's.
     * 
     * @param workflowDescription the {@link WorkflowDescription} to validate
     * @return {@link WorkflowDescriptionValidationResult}
     */
    WorkflowDescriptionValidationResult validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(
        WorkflowDescription workflowDescription);

    /**
     * Checks if the user-selected nodes for this workflow's components are reachable/visible, and whether the individual components on
     * these nodes are known/visible. If the node is visible, but the component is not, the typical cause is assumed to be a lack of
     * authorization group membership of the controller node (the local node), as the client typically wouldn't send a request for
     * components that are not accessible for it. Of course, the cause may also be a race condition between the client sending the request
     * and the controller processing it, so this conclusion is not definite. -- misc_ro
     * 
     * @param wfDescription the {@link WorkflowDescription} to validate
     * @return a map containing error messages, if any; keys are component identifiers (the common keys for validation errors), and the
     *         values are error messages
     */
    Map<String, String> validateRemoteWorkflowControllerVisibilityOfComponents(WorkflowDescription wfDescription);

    /**
     * Executes a workflow represented by its {@link WorkflowExecutionContext}.
     * 
     * @param executionContext workflow's {@link WorkflowExecutionContext}
     * @return {@link WorkflowExecutionInformation} of the instantiated and running workflow
     * @throws WorkflowExecutionException if starting the workflow failed
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     */
    WorkflowExecutionInformation startWorkflowExecution(WorkflowExecutionContext executionContext) throws WorkflowExecutionException,
        RemoteOperationException;

    /**
     * Triggers workflow to cancel.
     * 
     * @param handle the handle identifying the workflow to cancel
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void cancel(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Triggers workflow to pause.
     * 
     * @param handle the handle identifying the workflow to pause
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void pause(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Triggers workflow to resume when paused.
     * 
     * @param handle the handle identifying the workflow to resume
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void resume(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Triggers workflow to dispose.
     * 
     * @param handle the handle identifying the workflow to dispose
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void dispose(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Deletes a workflow run from data management.
     * 
     * @param handle the {@link WorkflowExecutionHandle} identifying the workflow to delete
     * @throws ExecutionControllerException on failure
     */
    void deleteFromDataManagement(WorkflowExecutionHandle handle) throws ExecutionControllerException;

    /**
     * Gets current workflow state.
     * 
     * @param handle the handle identifying the workflow to get the state for
     * @return {@link WorkflowState}
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    WorkflowState getWorkflowState(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException;

    /**
     * Gets the data management id for a workflow.
     * 
     * @param handle the handle identifying the workflow to get the data management id for
     * @return {@link WorkflowState}
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    Long getWorkflowDataManagementId(WorkflowExecutionHandle handle) throws ExecutionControllerException,
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
