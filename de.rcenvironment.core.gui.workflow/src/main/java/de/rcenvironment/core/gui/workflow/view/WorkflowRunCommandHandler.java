/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view;

import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Abstract base class for all command handlers behind the toolbar buttons for manipulating workflow runs.
 * 
 * @author Alexander Weinert
 */
public abstract class WorkflowRunCommandHandler extends AbstractHandler {
    
    private WorkflowExecutionService executionService;
    
    private WorkflowRunEditorService workflowRunEditorService;
    
    protected WorkflowRunCommandHandler() {
        // We inject the services here manually instead of using OSGi-DS, since this class is not instantiated via OSGi,
        // but instead via Eclipse RCP. To the best of my knowledge (AW), there exists no way to ask RCP to instantiate
        // command handlers via OSGi
        final ServiceRegistryAccess serviceAccess = ServiceRegistry.createAccessFor(this);
        this.executionService = serviceAccess.getService(WorkflowExecutionService.class);
        this.workflowRunEditorService = serviceAccess.getService(WorkflowRunEditorService.class);
    }

    @Override
    // This method has to always return null due to the specifiation of IHandler. Thus, we exempt the following line from Sonar checking 
    public Object execute(ExecutionEvent arg0) throws ExecutionException { // NOSONAR
        final Optional<WorkflowRunEditor> editor = this.workflowRunEditorService.getCurrentWorkflowRunEditor();
        if (!editor.isPresent()) {
            // TODO Properly report this error to the user
            return null;
        }

        final WorkflowExecutionHandle executionInformation = editor.get().getWorkflowExecutionInformation().getWorkflowExecutionHandle();
        try {
            this.manipulateWorkflow(executionInformation);
        } catch (ExecutionControllerException | RemoteOperationException e) {
            // TODO Properly report this error to the user
        }
        return null;
    }
    
    protected WorkflowExecutionService getWorkflowExecutionService() {
        return this.executionService;
    }
    
    protected abstract void manipulateWorkflow(WorkflowExecutionHandle handle)
        throws ExecutionControllerException, RemoteOperationException;
}
