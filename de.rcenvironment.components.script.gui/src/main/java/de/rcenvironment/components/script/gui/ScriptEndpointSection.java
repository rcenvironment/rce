/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.gui;

import org.eclipse.swt.SWT;
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
            EndpointType.INPUT, ScriptComponentConstants.GROUP_NAME_AND, new String[] { ScriptComponentConstants.GROUP_NAME_OR },
            new String[] {}, this);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, "default", new String[] {}, new String[] {}, this);
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
    protected Updater createUpdater() {
        return new ScriptWidgetsUpdater();
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

    /**
     * Script-specific implementation of {@link Updater}.
     * 
     * @author Doreen Seider
     */
    private class ScriptWidgetsUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {
            if (propertyName.equals(ScriptComponentConstants.PROP_KEY_XOR) && Boolean.parseBoolean(newValue)) {
                inputPane.updateDynamicEndpointIdToManage(ScriptComponentConstants.GROUP_NAME_OR);
            }
            super.updateControl(control, propertyName, newValue, oldValue);
        }

    }

    private void switchDynamicInputIds(boolean xor) {
        if (xor) {
            switchDynamicInputsId(ScriptComponentConstants.GROUP_NAME_OR);
        } else {
            switchDynamicInputsId(ScriptComponentConstants.GROUP_NAME_AND);
        }
    }

    private void switchDynamicInputsId(String toDynamicInputId) {
        inputPane.updateDynamicEndpointIdToManage(toDynamicInputId);
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
