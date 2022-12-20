/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.handlers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditor;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.WorkflowIntegrationEditorInput;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Handler that opens the {@link WorkflowIntegrationEditor} with selected components nodes within a {@link WorkflowEditor} as input.
 *
 * @author Jan Flink
 */
public class SelectedNodesIntegrationHandler extends AbstractHandler {

    private static final Log LOG = LogFactory.getLog(SelectedNodesIntegrationHandler.class);

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {

        final IWorkbenchPart activePart = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart instanceof WorkflowEditor) {
            WorkflowEditor editor = (WorkflowEditor) activePart;
            GraphicalViewer viewer = editor.getViewer();
            WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
            List<?> selection = viewer.getSelectedEditParts();

            List<WorkflowNode> nodes = selection.stream().filter(WorkflowNodePart.class::isInstance).map(WorkflowNodePart.class::cast)
                .map(AbstractEditPart::getModel).filter(WorkflowNode.class::isInstance).map(WorkflowNode.class::cast)
                .collect(Collectors.toList());

            List<WorkflowLabel> labels = selection.stream().filter(WorkflowLabelPart.class::isInstance).map(WorkflowLabelPart.class::cast)
                .map(AbstractEditPart::getModel).filter(WorkflowLabel.class::isInstance).map(WorkflowLabel.class::cast)
                .collect(Collectors.toList());

            WorkflowDescription clone = model.clone();

            for (WorkflowNode node : clone.getWorkflowNodes()) {
                if (nodes.contains(node)) {
                    continue;
                }
                clone.removeWorkflowNodeAndRelatedConnections(node);
            }

            for (WorkflowLabel label : clone.getWorkflowLabels()) {
                if (labels.contains(label)) {
                    continue;
                }
                clone.removeWorkflowLabel(label);
            }

            if (!clone.getWorkflowNodes().isEmpty()) {
                try {
                    IEditorDescriptor desc =
                        PlatformUI.getWorkbench().getEditorRegistry().findEditor("de.rcenvironment.rce.gui.workflowintegration.Editor");
                    IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    clone.setName(StringUtils.format(editor.getTitle()));

                    WorkflowIntegrationEditorInput editorInput = new WorkflowIntegrationEditorInput(clone);
                    Optional<String> validationMessage = editorInput.validate();
                    if (validationMessage.isPresent()) {
                        LOG.warn(StringUtils.format("Error opening the workflow integration editor.\n%s", validationMessage.get()));
                        MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error opening the integration editor",
                            "Could not open the workflow integration editor.\n"
                                + validationMessage.get());
                    } else {
                        activePage.openEditor(editorInput, desc.getId());
                    }
                } catch (PartInitException e) {
                    LOG.error("Error opening the workflow integration editor.", e);
                }
            }
        }
        return null;
    }

}
