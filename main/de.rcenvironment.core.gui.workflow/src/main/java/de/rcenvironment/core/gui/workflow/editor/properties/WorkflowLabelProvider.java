/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
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
        String value = ""; //$NON-NLS-1$
        if (objects == null || objects.equals(StructuredSelection.EMPTY)) {
            value = Messages.noItemSelected;
        } else if (((IStructuredSelection) objects).size() > 1) {
            value = ((IStructuredSelection) objects).size() + Messages.itemSelected;
        } else {
            Object object = ((IStructuredSelection) objects).getFirstElement();
            if (object instanceof WorkflowNodePart) {
                value = ((WorkflowNode) ((WorkflowNodePart) object).getModel()).getName();
            } else if (object instanceof WorkflowExecutionInformationPart) {
                value = ((WorkflowExecutionInformation) ((WorkflowExecutionInformationPart) object).getModel()).getInstanceName();
            } else if (object instanceof WorkflowLabelPart) {
                value = "Label Properties";
            } else if (object instanceof WorkflowPart || object instanceof ConnectionPart) {
                try {
                    String partName =
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePartReference().getPartName();
                    value = StringUtils.removeEndIgnoreCase(partName, ".wf");
                } catch (NullPointerException npe){
                    value = "Connection Editor";
                }
            }
        }
        return value;
    }
}
