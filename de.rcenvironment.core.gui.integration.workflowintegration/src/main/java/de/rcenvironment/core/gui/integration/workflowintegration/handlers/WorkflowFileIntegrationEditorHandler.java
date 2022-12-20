/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowFileEditorInput;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditor;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Handler that opens the {@link WorkflowIntegrationEditor} with a workflow file as input.
 * 
 * @author Jan Flink
 */
public class WorkflowFileIntegrationEditorHandler extends AbstractHandler {
    
    private static final Log LOG = LogFactory.getLog(WorkflowFileIntegrationEditorHandler.class);

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {

        StructuredSelection selection =
            (StructuredSelection) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
        if (selection.getFirstElement() instanceof IFile) {

            IEditorDescriptor desc =
                PlatformUI.getWorkbench().getEditorRegistry().findEditor("de.rcenvironment.rce.gui.workflowintegration.Editor");
            IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IFile workflowFile = (IFile) selection.getFirstElement();
            File tempFile = null;
            try {
                tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("workflow_to_integrate_*.wf");
                Files.copy(workflowFile.getLocation().toFile().toPath(), tempFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
                WorkflowFileEditorInput editorInput = new WorkflowFileEditorInput(workflowFile);
                Optional<String> validationMessage = editorInput.validate();
                if (validationMessage.isPresent()) {
                    LOG.warn(StringUtils.format("Error opening the workflow integration editor.\n%s", validationMessage.get()));
                    MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error opening the integration editor",
                        "Could not open the workflow integration editor.\n"
                        + validationMessage.get());
                } else {
                    activePage.openEditor(editorInput, desc.getId());
                }
            } catch (IOException e) {
                LOG.error("Error during creation of a temporary workflow file.", e);
            } catch (PartInitException e) {
                LOG.error("Error opening the workflow integration editor.", e);
            } finally {
                try {
                    if (tempFile != null) {
                        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempFile);
                    }
                } catch (IOException e) {
                    LOG.error(e.getStackTrace());
                }
            }
        }
        return null;
    }

}
