/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.ConnectionPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowExecutionInformationPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;

/**
 * Returns the label displayed in the head of the properties tab.
 * 
 * @author Heinrich Wendel
 * @author Sascha Zur
 */
public final class WorkflowLabelProvider extends LabelProvider {

    @Override
    public String getText(Object objects) {
        final int maxLength = 40;
        String value = ""; //$NON-NLS-1$
        if (objects == null || objects.equals(StructuredSelection.EMPTY)) {
            value = Messages.noItemSelected;
        } else if (((IStructuredSelection) objects).size() > 1) {
            value = ((IStructuredSelection) objects).size() + Messages.itemSelected;
        } else {
            Object object = ((IStructuredSelection) objects).getFirstElement();
            if (object instanceof WorkflowNodePart) {
                value = de.rcenvironment.core.utils.common.StringUtils.format("Component Properties: %s",
                    StringUtils.abbreviate(((WorkflowNode) ((WorkflowNodePart) object).getModel()).getName(), maxLength));
            } else if (object instanceof WorkflowExecutionInformationPart) {
                value =
                    de.rcenvironment.core.utils.common.StringUtils.format("Workflow: %s",
                        StringUtils.abbreviate(
                            ((WorkflowExecutionInformation) ((WorkflowExecutionInformationPart) object).getModel()).getInstanceName(),
                            maxLength));
            } else if (object instanceof WorkflowLabelPart) {
                String[] labelParts = ((WorkflowLabel) ((WorkflowLabelPart) object).getModel()).getText().split("\n");
                String labelAbrev = StringUtils.abbreviate(labelParts[0].replaceAll("\\r", ""), maxLength);
                if (!labelAbrev.endsWith("...") && labelParts.length > 1) {
                    labelAbrev += "...";
                }
                value = de.rcenvironment.core.utils.common.StringUtils.format("Label Properties: \"%s\"", labelAbrev);
            } else if (object instanceof WorkflowPart || object instanceof ConnectionPart) {
                try {
                    String partName = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().getActivePartReference().getPartName();
                    value = de.rcenvironment.core.utils.common.StringUtils.format("Workflow Properties: %s",
                        StringUtils.abbreviate(StringUtils.removeEndIgnoreCase(partName, ".wf"), maxLength));
                } catch (NullPointerException npe){
                    value = "Workflow Properties";
                }
            }
        }
        return value;
    }
}
