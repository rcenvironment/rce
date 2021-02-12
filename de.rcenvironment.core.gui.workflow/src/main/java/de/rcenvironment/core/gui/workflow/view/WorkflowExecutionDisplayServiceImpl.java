/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import de.rcenvironment.core.component.workflow.command.api.WorkflowExecutionDisplayService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;


/**
 * Default implementation of the {@link WorkflowExecutionDisplayService}. Displays the workflow execution by opening a workflow editor.
 * 
 * @author Alexander Weinert
 */
// We do not annotate this class as a ``Component'', since doing so would cause OSGI to activate this bundle regardless of whether a GUI is
// actually present. Instead, we register the workflow execution display service in the activation-method of this bundle
public class WorkflowExecutionDisplayServiceImpl implements WorkflowExecutionDisplayService {

    @Override
    public boolean hasGui() {
        return PlatformUI.isWorkbenchRunning();
    }

    @Override
    public void displayWorkflowExecution(WorkflowExecutionInformation wfExecInfo) {
        new UIJob("display workflow execution information") {

            @Override
            public IStatus runInUIThread(IProgressMonitor progressMonitor) {
                new WorkflowRunEditorAction(wfExecInfo).run();
                return Status.OK_STATUS;
            }
        }.schedule();

    }

}
