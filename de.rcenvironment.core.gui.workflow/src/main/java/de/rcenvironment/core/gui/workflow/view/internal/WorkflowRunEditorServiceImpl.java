/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view.internal;

import java.util.Optional;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;

import de.rcenvironment.core.gui.workflow.view.WorkflowRunEditor;
import de.rcenvironment.core.gui.workflow.view.WorkflowRunEditorService;

/**
 * Default implementation of {@link WorkflowRunEditorService}.
 * 
 * @author Alexander Weinert
 */
@Component
public class WorkflowRunEditorServiceImpl implements WorkflowRunEditorService {
    @Override
    public Optional<WorkflowRunEditor> getCurrentWorkflowRunEditor() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) {
            return Optional.empty();
        }
        
        final IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            return Optional.empty();
        }

        final IWorkbenchPage page = workbenchWindow.getActivePage();
        if (page == null) {
            return Optional.empty();
        }

        final IWorkbenchPart part = page.getActiveEditor();
        if (!(part instanceof WorkflowRunEditor)) {
            return Optional.empty();
        }
        
        return Optional.of((WorkflowRunEditor) part);
    }
}
