/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.RemotableExecutionControllerService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Workflow-specific {@link RemotableExecutionControllerService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
@RemotableService
public interface RemotableWorkflowExecutionControllerService extends RemotableExecutionControllerService {

    /**
     * Creates a new {@link WorkflowExecutionController} for the workflow represented be {@link WorkflowExecutionContext}.
     * 
     * @param executionContext {@link WorkflowExecutionContext} of the workflow to execute
     * @param authTokens access tokens that verify that the workflow's initiator is in fact allowed to access/execute each component within
     *        the workflow; also grants special access to local components on the initiator's instance, even if they are not published
     * @param calledFromRemote <code>true</code> if method is called remote remote node, <code>false</code> otherwise
     * @return {@link WorkflowExecutionInformation} of the workflow instance
     * @throws WorkflowExecutionException if node is not declared as workflow host
     * @throws RemoteOperationException if called from remote and remote method call failed
     */
    WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext executionContext,
        Map<String, String> authTokens, Boolean calledFromRemote) throws WorkflowExecutionException, RemoteOperationException;

    /**
     * @param executionId execution identifier of the workflow instance (provided by {@link WorkflowExecutionInformation})
     * @return {@link WorkflowState} of the workflow
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    WorkflowState getWorkflowState(String executionId) throws ExecutionControllerException, RemoteOperationException;

    /**
     * @param executionId execution identifier of the workflow instance (provided by {@link WorkflowExecutionInformation})
     * @return identifier the workflow is stored under in the data management, <code>null</code> if no data management entry exists for the
     *         workflow (yet)
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    Long getWorkflowDataManagementId(String executionId) throws ExecutionControllerException, RemoteOperationException;

    /**
     * @return {@link WorkflowExecutionInformation} objects of all active workflows
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    Collection<WorkflowExecutionInformation> getWorkflowExecutionInformations() throws ExecutionControllerException,
        RemoteOperationException;

    /**
     * The remote call performing the actual check behind
     * {@link WorkflowExecutionService#validateRemoteWorkflowControllerVisibilityOfComponents()}.
     * 
     * @param componentRefs a list of component descriptions comprised of parts joined with {@link StringUtils#escapeAndConcat()}
     * @return @return a map containing error messages, if any; keys are component identifiers (the common keys for validation errors), and
     *         the values are error messages
     * @throws RemoteOperationException common remote call exception
     */
    Map<String, String> verifyComponentVisibility(List<String> componentRefs) throws RemoteOperationException;

}
