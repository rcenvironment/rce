/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;


/**
 * Controls execution of workflows or components. There is one {@link RemotableExecutionControllerService} per node, which delegates
 * requests to {@link ExecutionController}s.
 * 
 * @author Doreen Seider
 */
@RemotableService
public interface RemotableExecutionControllerService {

    /**
     * Disposes a workflow/component.
     * 
     * @param executionId execution identifier of the component/workflow
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void performDispose(String executionId) throws ExecutionControllerException, RemoteOperationException;
    
    /**
     * Starts a workflow/component.
     * 
     * @param executionId execution identifier of the component/workflow
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void performStart(String executionId) throws ExecutionControllerException, RemoteOperationException;
    
    /**
     * Cancels a workflow/component.
     * 
     * @param executionId execution identifier of the component/workflow
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void performCancel(String executionId) throws ExecutionControllerException, RemoteOperationException;
    
    /**
     * Pauses a workflow/component.
     * 
     * @param executionId execution identifier of the component/workflow
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void performPause(String executionId) throws ExecutionControllerException, RemoteOperationException;
    
    /**
     * Resumes a workflow/component if it is paused.
     * 
     * @param executionId execution identifier of the component/workflow
     * @throws RemoteOperationException if called from remote and  remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void performResume(String executionId) throws ExecutionControllerException, RemoteOperationException;
    
}
