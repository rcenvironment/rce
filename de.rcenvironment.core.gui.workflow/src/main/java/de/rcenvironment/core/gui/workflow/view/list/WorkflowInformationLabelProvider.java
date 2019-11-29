/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.list;

import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.gui.workflow.Activator;

/**
 * LabelProvider for WorkflowInformation objects.
 * 
 * @author Heinrich Wendel
 */
public class WorkflowInformationLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public String getColumnText(Object element, int column) {
        String text = ""; //$NON-NLS-1$
        if (element instanceof WorkflowExecutionInformation) {
            if (column == 0) {
                text = ((WorkflowExecutionInformation) element).getInstanceName();
            } else if (column == 1) {
                final WorkflowState state = WorkflowStateModel.getInstance().getState(((WorkflowExecutionInformation) element)
                    .getExecutionIdentifier());
                if (state != null) {
                    text = state.getDisplayName();
                }
            } else if (column == 2) {
                return ((WorkflowExecutionInformation) element).getWorkflowDescription().getControllerNode().getAssociatedDisplayName();
            } else if (column == 3) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd  HH:mm:ss"); //$NON-NLS-1$
                text = df.format(((WorkflowExecutionInformation) element).getStartTime());
            } else if (column == 4) {
                text = ((WorkflowExecutionInformation) element).getNodeIdStartedExecution().getAssociatedDisplayName();
            } else if (column == 5) {
                text = ((WorkflowExecutionInformation) element).getAdditionalInformationProvidedAtStart();
            }
        }

        return text;
    }

    @Override
    public Image getColumnImage(Object element, int column) {
        if (element instanceof WorkflowExecutionInformation) {
            if (column == 0) {
                final WorkflowState state = WorkflowStateModel.getInstance().getState(((WorkflowExecutionInformation) element)
                    .getExecutionIdentifier());
                return Activator.getInstance().getImageRegistry().get(state.name());
            }
        }
        return null;
    }
}
