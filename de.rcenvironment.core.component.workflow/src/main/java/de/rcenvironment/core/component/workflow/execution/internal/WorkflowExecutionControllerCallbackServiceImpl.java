/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;

import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallbackService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionController;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link WorkflowExecutionControllerCallbackService}.
 * 
 * @author Doreen Seider
 *
 */
public class WorkflowExecutionControllerCallbackServiceImpl implements WorkflowExecutionControllerCallbackService {

    private LocalExecutionControllerUtilsService exeWfCtrlUtilsService;
    
    private BundleContext bundleContext;
    
    @Override
    @AllowRemoteAccess
    public void onComponentStateChanged(String executionId, String compExeId, ComponentState newState,
        Integer executionCount, String executionCountOnResets) throws ExecutionControllerException, RemoteOperationException {
        exeWfCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext)
            .onComponentStateChanged(compExeId, newState, executionCount, executionCountOnResets);
    }

    @Override
    @AllowRemoteAccess
    public void onComponentStateChanged(String executionId, String compExeId, ComponentState newState,
        Integer executionCount, String executionCountOnResets, String errorMessage) throws ExecutionControllerException,
        RemoteOperationException {
        exeWfCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext)
            .onComponentStateChanged(compExeId, newState, executionCount, executionCountOnResets, errorMessage);
    }
    
    @Override
    @AllowRemoteAccess
    public void onComponentStateChanged(String executionId, String compExeId, ComponentState newState, Integer executionCount,
        String executionCountOnResets, String errorId, String errorMessage) throws ExecutionControllerException, RemoteOperationException {
        exeWfCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext)
            .onComponentStateChanged(compExeId, newState, executionCount, executionCountOnResets, errorId, errorMessage);
    }

    @Override
    @AllowRemoteAccess
    public void onInputProcessed(String executionId, String serializedEndpointDatum) throws ExecutionControllerException,
        RemoteOperationException {
        exeWfCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext)
            .onInputProcessed(serializedEndpointDatum);
    }

    @Override
    @AllowRemoteAccess
    public void onComponentHeartbeatReceived(String executionId, String compExecutionId) throws ExecutionControllerException,
        RemoteOperationException {
        try {
            exeWfCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext)
                .onComponentHeartbeatReceived(compExecutionId);
        } catch (ServiceException e) {
            LogFactory.getLog(getClass()).warn(StringUtils.format("Failed to send heart beat to workflow controller %s;"
                + " it is not available (anymore): %s", executionId, e.toString()));
        }
    }

    @Override
    @AllowRemoteAccess
    public void onConsoleRowsProcessed(String executionId, ConsoleRow[] consoleRows) throws ExecutionControllerException,
        RemoteOperationException {
        exeWfCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext)
            .processConsoleRows(consoleRows);
    }
    
    protected void activate(ComponentContext osgiComponentContext) {
        bundleContext = osgiComponentContext.getBundleContext();
    }
    
    protected void bindLocalExecutionControllerUtilsService(LocalExecutionControllerUtilsService newService) {
        exeWfCtrlUtilsService = newService;
    }


}
