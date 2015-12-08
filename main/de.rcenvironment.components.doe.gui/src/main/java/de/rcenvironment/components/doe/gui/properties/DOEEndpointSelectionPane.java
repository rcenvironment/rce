/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.doe.gui.properties;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
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

    public DOEEndpointSelectionPane(String genericEndpointTitle, EndpointType direction, Executor executor, boolean readonly,
        String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints) {
        super(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, showOnlyManagedEndpoints);
    }

    @Override
    public Control createControl(Composite parent, String title, FormToolkit toolkit) {
        Control control = super.createControl(parent, title, toolkit);
        // empty label to get desired layout - feel free to improve
        new Label(client, SWT.READ_ONLY);
        Composite noteComposite = toolkit.createComposite(client);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        noteComposite.setLayoutData(gridData);
        noteComposite.setLayout(new GridLayout(2, false));
        Label noteLabel = new Label(noteComposite, SWT.READ_ONLY);
        noteLabel.setText(Messages.outputsNote);

        section.setClient(client);
        toolkit.paintBordersFor(client);
        section.setExpanded(true);
        return control;
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command = new DOEAddDynamicEndpointCommand(endpointType, endpointIdToManage, name, type, metaData, this);
        metaData.put(LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE, LoopEndpointType.SelfLoopEndpoint.name());
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        WorkflowNodeCommand command = new DOERemoveDynamicEndpointCommand(endpointType, endpointIdToManage, names, this);
        execute(command);
    }
}
