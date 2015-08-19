/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * An adapted {@link EndpointSelectionPane} for the the inputs of the Script component, which need
 * an extra checkbox below the table.
 * 
 * @author Doreen Seider
 */
public class ScriptInputSelectionPane extends EndpointSelectionPane {

    private Button orGroupCheckbox;

    public ScriptInputSelectionPane(String genericEndpointTitle, EndpointType direction, Executor executor,
        boolean readonly, String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints) {
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
        noteComposite.setLayout(new GridLayout(1, false));
        orGroupCheckbox = new Button(noteComposite, SWT.CHECK);
        orGroupCheckbox.setText("Execute on each new input value");
        orGroupCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (orGroupCheckbox.getSelection()) {
                    endpointIdToManage = ScriptComponentConstants.GROUP_NAME_OR;
                } else {
                    endpointIdToManage = ScriptComponentConstants.GROUP_NAME_AND;
                }

                WorkflowNodeCommand command = new SwitchDynamicInputsCommand(endpointIdToManage, ScriptInputSelectionPane.this);
                execute(command);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);

            }
        });
        Label noteLabel = new Label(noteComposite, SWT.READ_ONLY);
        noteLabel.setText("(ie inputs have an 'xor' relation instead of an 'and' relation)");

        section.setClient(client);
        toolkit.paintBordersFor(client);
        section.setExpanded(true);

        return control;
    }

    @Override
    public void setConfiguration(ComponentInstanceProperties configuration) {
        if (configuration != null) {
            super.setConfiguration(configuration);
            setupOrGroupCheckbox();
        }
    }

    private void setupOrGroupCheckbox() {
        orGroupCheckbox.setSelection(endpointIdToManage.equals(ScriptComponentConstants.GROUP_NAME_OR)
            || (!getConfiguration().getInputDescriptionsManager().getEndpointDescriptions().isEmpty()
            && getConfiguration().getInputDescriptionsManager().getEndpointDescriptions().iterator()
                .next().getDynamicEndpointIdentifier().equals(ScriptComponentConstants.GROUP_NAME_OR)));
        if (orGroupCheckbox.getSelection()) {
            endpointIdToManage = ScriptComponentConstants.GROUP_NAME_OR;
        }
    }
}
