/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.doe.gui.properties;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * 
 * @author Sascha Zur
 */
public class DOEEndpointSelectionPane extends EndpointSelectionPane {

    public DOEEndpointSelectionPane(String title, Executor executor) {
        super(title, EndpointType.OUTPUT, "default", new String[] {}, new String[] {}, executor, false, true);
    }

    @Override
    public Control createControl(Composite parent, String title, FormToolkit toolkit) {
        Control control = super.createControl(parent, title, toolkit);

        Label noteLabel = new Label(client, SWT.READ_ONLY | SWT.WRAP);
        GridData gridData = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
        gridData.horizontalSpan = 2;
        noteLabel.setLayoutData(gridData);
        noteLabel.setText(Messages.outputsNote);

        section.setClient(client);
        toolkit.paintBordersFor(client);
        section.setExpanded(true);
        return control;
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command = new DOEAddDynamicEndpointCommand(endpointType, dynEndpointIdToManage, name, type, metaData, this);
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        WorkflowNodeCommand command = new DOERemoveDynamicEndpointCommand(endpointType, dynEndpointIdToManage, names, this);
        execute(command);
    }

}
