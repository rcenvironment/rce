/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.handlers;

import java.io.FileNotFoundException;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.integration.workflowintegration.WorkflowIntegrationController;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditorInput;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Handler to open a Workflow (e.g. via Palette).
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class EditWorkflowIntegrationHandler extends AbstractHandler {
    
    private static final Log LOG = LogFactory.getLog(EditWorkflowIntegrationHandler.class);

    private String integrationName;

    public EditWorkflowIntegrationHandler(String integrationName) {
        super();
        this.integrationName = integrationName;
    }

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        IEditorDescriptor desc =
            PlatformUI.getWorkbench().getEditorRegistry().findEditor("de.rcenvironment.rce.gui.workflowintegration.Editor");
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();

        WorkflowIntegrationController workflowIntegrationController;
        try {
            workflowIntegrationController = new WorkflowIntegrationController(integrationName);
        } catch (FileNotFoundException e) {
            LOG.error(StringUtils.format(
                "The Workflow Integration Editor cannot be displayed. The following file does not exist: \"%s\" ",
                e.getMessage()));
            return null;
        }
        workflowIntegrationController.setEditMode(true);
        try {
            WorkflowIntegrationEditorInput input =
                new WorkflowIntegrationEditorInput(workflowIntegrationController);
            Optional<String> validationMessage = input.validate();
            if (validationMessage.isPresent()) {
                LOG.warn(StringUtils.format("Error opening the workflow integration editor.\n%s", validationMessage.get()));
                MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error opening the integration editor",
                    "Could not open the workflow integration editor.\n"
                        + validationMessage.get());
            } else {
                activePage.openEditor(input, desc.getId());
            }
        } catch (PartInitException e) {
            LOG.error("Error opening the workflow integration editor.", e);
        }
        return null;
    }

}
