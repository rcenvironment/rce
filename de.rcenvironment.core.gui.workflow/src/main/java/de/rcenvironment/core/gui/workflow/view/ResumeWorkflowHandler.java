/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view;

import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Handler for the toolbar-button labelled "Resume".
 * 
 * @author Alexander Weinert
 */
public class ResumeWorkflowHandler extends WorkflowRunCommandHandler {
    @Override
    protected void manipulateWorkflow(WorkflowExecutionHandle handle)
        throws ExecutionControllerException, RemoteOperationException {
        getWorkflowExecutionService().resume(handle);
    }
}
