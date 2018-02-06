/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.joiner.gui;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.EndpointHandlingHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * Provides a configuration GUI for the Merger component.
 * 
 * @author Sascha Zur
 */
public class JoinerInputSection extends DefaultEndpointPropertySection {

    private static final int MINIMUM_SIZE_CONTROLS = 70;

    private static final int MINIMUM_INPUT_COUNT = 2;

    private static final int MAXIMUM_INPUT_COUNT = 100;

    private Combo dataTypeCombo;

    private Spinner inputCountSpinner;

    private final List<DataType> dataTypesInCombo = new ArrayList<DataType>();

    private final EndpointSelectionPane inputPane;

    private final EndpointSelectionPane outputPane;

    private boolean disableSectionRefresh = false;

    public JoinerInputSection() {
        inputPane = new EndpointSelectionPane(Messages.inputs, EndpointType.INPUT, null,
            new String[] { JoinerComponentConstants.DYNAMIC_INPUT_ID }, new String[] {}, this, true);

        outputPane = new EndpointSelectionPane(Messages.outputs, EndpointType.OUTPUT, null,
            new String[] {}, new String[] { JoinerComponentConstants.OUTPUT_NAME }, this, true);

        setPanes(inputPane, outputPane);
    }

    @Override
    public void createCompositeContent(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createCompositeContent(parent, aTabbedPropertySheetPage);

        Section section = aTabbedPropertySheetPage.getWidgetFactory().createSection(endpointsComposite, Section.TITLE_BAR);
        section.setText(Messages.joinerConfig);
        GridData sectionData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        sectionData.horizontalSpan = 2;
        section.setLayoutData(sectionData);
        Composite configurationComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(section);

        configurationComposite.setLayout(new GridLayout(2, false));
        GridData cCompData = new GridData();
        cCompData.horizontalSpan = 2;
        configurationComposite.setLayoutData(cCompData);
        Label dataTypeLabel = new Label(configurationComposite, SWT.NONE);
        dataTypeLabel.setText(Messages.inputType);

        dataTypeCombo = new Combo(configurationComposite, SWT.READ_ONLY);
        dataTypeCombo.setData(CONTROL_PROPERTY_KEY, JoinerComponentConstants.DATATYPE);
        GridData gridData = new GridData();
        gridData.widthHint = MINIMUM_SIZE_CONTROLS;
        dataTypeCombo.setLayoutData(gridData);
        dataTypeCombo.addSelectionListener(new DataTypeChangedListener());

        Label inputCountLabel = new Label(configurationComposite, SWT.NONE);
        inputCountLabel.setText(Messages.inputCount);
        inputCountSpinner = new Spinner(configurationComposite, SWT.BORDER);
        inputCountSpinner.setData(CONTROL_PROPERTY_KEY, JoinerComponentConstants.INPUT_COUNT);
        inputCountSpinner.setMinimum(MINIMUM_INPUT_COUNT);
        inputCountSpinner.setMaximum(MAXIMUM_INPUT_COUNT);
        inputCountSpinner.addSelectionListener(new InputCountChangedListener());
        section.moveAbove(inputPane.getControl());
        section.setClient(configurationComposite);
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        if (dataTypeCombo.getItemCount() == 0) {
            for (DataType dataType : getConfiguration().getInputDescriptionsManager()
                .getDynamicEndpointDefinition(JoinerComponentConstants.DYNAMIC_INPUT_ID).getPossibleDataTypes()) {
                dataTypeCombo.add(dataType.getDisplayName());
                dataTypesInCombo.add(dataType);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(EndpointDescriptionsManager.PROPERTY_ENDPOINT) && !disableSectionRefresh) {
            refreshSection();
        }
    }

    /**
     * Edits endpoint's data types.
     * 
     * @author Sascha Zur
     * @author Doreen Seider
     */
    private class DataTypeChangedListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent arg0) {

            DataType newDataType = dataTypesInCombo.get(dataTypeCombo.getSelectionIndex());

            EndpointDescription oldInput = getConfiguration().getInputDescriptionsManager()
                .getEndpointDescription(JoinerComponentConstants.INPUT_NAME + "001");
            EndpointDescription oldOutput = getConfiguration().getOutputDescriptionsManager()
                .getEndpointDescription(JoinerComponentConstants.OUTPUT_NAME);

            if (EndpointHandlingHelper.editEndpointDataType(EndpointType.INPUT, oldInput, newDataType)
                && EndpointHandlingHelper.editEndpointDataType(EndpointType.OUTPUT, oldOutput, newDataType)) {
                disableSectionRefresh = true;
                execute(new JoinerEditDynamicEndpointCommand(newDataType));
                disableSectionRefresh = false;
                refreshSection();
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
    }

    /**
     * Adds or removes inputs.
     * 
     * @author Sascha Zur
     * @author Doreen Seider
     */
    private class InputCountChangedListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            int newCount = inputCountSpinner.getSelection();
            if (newCount != getInputs().size()) {
                disableSectionRefresh = true;
                execute(new JoinerAddOrRemoveDynamicEndpointsCommand(newCount));
                disableSectionRefresh = false;
                refreshSection();
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    @Override
    protected Synchronizer createSynchronizer() {
        return new JoinerWidgetsSynchronizer();
    }

    @Override
    protected Updater createUpdater() {
        return new JoinerConfigrationWidgetsUpdater();
    }

    /**
     * Joiner-specific implementation of {@link Updater}.
     * 
     * @author Doreen Seider
     *
     */
    private class JoinerConfigrationWidgetsUpdater extends DefaultUpdater {

        @Override
        public void updateControl(final Control control, final String propertyName, final String newValue,
            final String oldValue) {
            if (propertyName.equals(JoinerComponentConstants.DATATYPE) && newValue != null) {
                dataTypeCombo.select(dataTypesInCombo.indexOf(DataType.valueOf(newValue)));
            } else {
                super.updateControl(control, propertyName, newValue, oldValue);
            }
        }
    }

    /**
     * Joiner-specific implementation of {@link Synchronizer}.
     * 
     * @author Doreen Seider
     *
     */
    private class JoinerWidgetsSynchronizer extends DefaultSynchronizer {

        @Override
        protected void handlePropertyChange(Control control, String key, String newValue, String oldValue) {
            if (key.equals(JoinerComponentConstants.DATATYPE)) {
                dataTypeCombo.select(dataTypesInCombo.indexOf(DataType.valueOf(newValue)));
                inputPane.refresh();
                outputPane.refresh();
                return;
            } else if (key.equals(JoinerComponentConstants.INPUT_COUNT)) {
                inputPane.refresh();
            }
            super.handlePropertyChange(control, key, newValue, oldValue);
        }
    }

}
