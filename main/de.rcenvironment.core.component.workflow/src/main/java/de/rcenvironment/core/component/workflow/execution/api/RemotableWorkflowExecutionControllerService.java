/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.RemotableExecutionControllerService;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Workflow-specific {@link RemotableExecutionControllerService}.
 * 
 * @author Doreen Seider
 */
@RemotableService
public interface RemotableWorkflowExecutionControllerService extends RemotableExecutionControllerService {

    /**
     * Creates a new {@link WorkflowExecutionController} for the workflow represented be {@link WorkflowExecutionContext}.
     * 
     * @param executionContext {@link WorkflowExecutionContext} of the workflow to execute
     * @param executionAuthTokens the auth tokens which authorizes the {@link WorkflowExecutionController} to execute its components
     * @param calledFromRemote <code>true</code> if method is called remote remote node, <code>false</code> otherwise
     * @return {@link WorkflowExecutionInformation} of the workflow instance
     * @throws WorkflowExecutionException if node is not declared as workflow host
     * @throws RemoteOperationException if called from remote and remote method call failed
     */
    WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext executionContext,
        Map<String, String> executionAuthTokens, Boolean calledFromRemote) throws WorkflowExecutionException, RemoteOperationException;
    
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

}
