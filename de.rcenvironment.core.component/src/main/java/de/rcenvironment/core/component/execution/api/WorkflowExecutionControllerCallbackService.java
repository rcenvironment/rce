/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Callback class used to callback workflow {@link ExecutionController}s, mainly by associated {@link ComponentExecutionController} objects.
 * 
 * @author Doreen Seider
 */
@RemotableService
public interface WorkflowExecutionControllerCallbackService {

    /**
     * Called if component changed its state.
     * 
     * @param wfExecutionId execution identifier of workflow execution controller addressed
     * @param compExeId execution identifier of component executed
     * @param newState new {@link ComponentState}
     * @param executionCount current execution count
     * @param executionCountOnResets execution count on component resets
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    // introduced as null parameter can not be passed to a remote accessible method (the appropriate method won't be find on the other node)
    void onComponentStateChanged(String wfExecutionId, String compExeId, ComponentState newState, Integer executionCount,
        String executionCountOnResets) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Called if component changed its state.
     * 
     * @param wfExecutionId execution identifier of workflow execution controlled addressed
     * @param compExeId execution identifier of component executed
     * @param newState new {@link ComponentState}
     * @param executionCount current execution count
     * @param executionCountOnResets execution count on component resets
     * @param errorMessage if new state was caused by an exception
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void onComponentStateChanged(String wfExecutionId, String compExeId,
        ComponentState newState, Integer executionCount, String executionCountOnResets, String errorMessage)
        throws ExecutionControllerException, RemoteOperationException;
    
    /**
     * Called if component changed its state.
     * 
     * @param wfExecutionId execution identifier of workflow execution controlled addressed
     * @param compExeId execution identifier of component executed
     * @param newState new {@link ComponentState}
     * @param executionCount current execution count
     * @param executionCountOnResets execution count on component resets
     * @param errorId id of the cause
     * @param errorMessage error message of the cause
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void onComponentStateChanged(String wfExecutionId, String compExeId,
        ComponentState newState, Integer executionCount, String executionCountOnResets, String errorId, String errorMessage)
        throws ExecutionControllerException, RemoteOperationException;

    /**
     * Called on for input values passed to an upcoming call of {@link Component#processInputs()}.
     * 
     * @param wfExecutionId execution identifier of workflow execution controlled addressed
     * @param serializedEndpointDatum serialized {@link EndpointDatum} read
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void onInputProcessed(String wfExecutionId, String serializedEndpointDatum) throws ExecutionControllerException,
        RemoteOperationException;

    /**
     * Called if a component said that he is still alive.
     * 
     * @param wfExecutionId execution identifier of workflow execution controlled addressed
     * @param executionIdentifier execution identifier of the calling component
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void onComponentHeartbeatReceived(String wfExecutionId, String executionIdentifier) throws ExecutionControllerException,
        RemoteOperationException;

    /**
     * Called when new {@link ConsoleRow}s are provided.
     * 
     * @param wfExecutionId execution identifier of workflow execution controlled addressed
     * @param consoleRows {@link ConsoleRow}s to process
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void onConsoleRowsProcessed(String wfExecutionId, ConsoleRow[] consoleRows) throws ExecutionControllerException,
        RemoteOperationException;

}
