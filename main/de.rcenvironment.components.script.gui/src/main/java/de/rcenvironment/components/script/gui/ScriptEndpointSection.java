/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;

/**
 * "Properties" view tab for endpoints.
 * 
 * @author Doreen Seider
 */
public class ScriptEndpointSection extends DefaultEndpointPropertySection {
    
    private EndpointSelectionPane inputPane;
    
    private Button orGroupCheckbox;

    public ScriptEndpointSection() {
        inputPane = new EndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, this, false, ScriptComponentConstants.GROUP_NAME_AND, false);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, this, false, "default", false);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }
    
    @Override
    public void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createCompositeContent(parent, aTabbedPropertySheetPage);
        TabbedPropertySheetWidgetFactory toolkit = aTabbedPropertySheetPage.getWidgetFactory();
        Composite noteComposite = toolkit.createComposite(endpointsComposite);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        noteComposite.setLayoutData(gridData);
        noteComposite.setLayout(new GridLayout(1, false));
        orGroupCheckbox = new Button(noteComposite, SWT.CHECK);
        orGroupCheckbox.setText("Execute on each new input value");
        orGroupCheckbox.setData(CONTROL_PROPERTY_KEY, ScriptComponentConstants.PROP_KEY_XOR);
        orGroupCheckbox.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        Label noteLabel = new Label(noteComposite, SWT.READ_ONLY);
        noteLabel.setText("(ie inputs have an 'xor' relation instead of an 'and' relation)");
    }
    
    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        inputPane.refresh();
    }
    
    @Override
    protected Synchronizer createSynchronizer() {
        return new ScriptWidgetsSynchronizer();
    }
    
    @Override
    protected Controller createController() {
        return new ScriptWidgetsController();
    }
    
    /**
     * Script-specific implementation of {@link Controller}.
     * 
     * @author Doreen Seider
     */
    private class ScriptWidgetsController extends DefaultController {
        
        @Override
        protected void widgetSelected(SelectionEvent event, Control source, String property) {
            if (property.equals(ScriptComponentConstants.PROP_KEY_XOR)) {
                switchDynamicInputIds(((Button) source).getSelection());
            }
            super.widgetSelected(event, source, property);
        }
    }
    
    /**
     * Script-specific implementation of {@link Synchronizer}.
     * 
     * @author Doreen Seider
     */
    private class ScriptWidgetsSynchronizer extends DefaultSynchronizer {
        
        @Override
        protected void handlePropertyChange(Control control, String key, String newValue, String oldValue) {
            if (key.equals(ScriptComponentConstants.PROP_KEY_XOR)) {
                switchDynamicInputIds(Boolean.parseBoolean(newValue));
            }
            super.handlePropertyChange(control, key, newValue, oldValue);
        }

    }
    
    private void switchDynamicInputIds(boolean xor) {
        if (xor) {
            switchDynamicInputsId(ScriptComponentConstants.GROUP_NAME_OR);
        } else {
            switchDynamicInputsId(ScriptComponentConstants.GROUP_NAME_AND);
        }
        inputPane.refresh();
    }
    
    private void switchDynamicInputsId(String toDynamicInputId) {
        EndpointDescriptionsManager inputDescriptionsManager = getConfiguration().getInputDescriptionsManager();
        for (EndpointDescription ep : inputDescriptionsManager.getEndpointDescriptions()) {
            inputDescriptionsManager.removeDynamicEndpointDescriptionQuietely(ep.getName());
            ep.getMetaData().remove(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT);
            inputDescriptionsManager.addDynamicEndpointDescription(toDynamicInputId,
                ep.getName(), ep.getDataType(), ep.getMetaData(), ep.getIdentifier());
            if (!ep.getConnectedDataTypes().isEmpty()) {
                // inputs can only be connected to one ouput
                inputDescriptionsManager.addConnectedDataType(ep.getName(), ep.getConnectedDataTypes().get(0));
            }
        }
    }
    
}
