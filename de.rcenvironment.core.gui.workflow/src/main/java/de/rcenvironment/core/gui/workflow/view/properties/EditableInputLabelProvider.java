/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.EndpointContentProvider;
import de.rcenvironment.core.gui.workflow.EndpointLabelProvider;

/**
 * {@link LabelProvider} for the contents of the {@link EditableInputTreeViewer}.
 * 
 * @author Doreen Seider
 */
public class EditableInputLabelProvider extends EndpointLabelProvider implements ITableLabelProvider {

    private WorkflowExecutionInformation workflowInformation;

    private String workflowId;

    private String componentId;

    public EditableInputLabelProvider(WorkflowExecutionInformation workflowInformation) {
        super(EndpointType.INPUT);
        this.workflowInformation = workflowInformation;
        workflowId = workflowInformation.getExecutionIdentifier();
    }

    public EditableInputLabelProvider(String workflowId, String componentId) {
        super(EndpointType.INPUT);
        this.workflowId = workflowId;
        this.componentId = componentId;
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
        if (columnIndex == 0) {
            return getImage(element);
        }
        return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
        if (columnIndex == 0) {
            return getText(element);
        } else {
            String inputValue = ""; //$NON-NLS-1$;
            if (element instanceof EndpointContentProvider.Endpoint) {
                if (componentId == null) {
                    if (columnIndex == 1) {
                        inputValue = InputEditingHelper.getLatestInputValue(workflowInformation,
                            (EndpointContentProvider.Endpoint) element);
                    }
                } else {
                    if (columnIndex == 1) {
                        inputValue = InputEditingHelper.getLatestInputValueFromEndpoint(workflowId, componentId,
                            (EndpointContentProvider.Endpoint) element);
                    }
                }
            }
            return inputValue;
        }
    }

}
