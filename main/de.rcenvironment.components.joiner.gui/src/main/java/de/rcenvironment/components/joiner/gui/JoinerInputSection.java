/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.joiner.gui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
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

    private static final int CONFIG_COMBO_WIDTH = 95;

    private static final int COMPOSITES_HEIGHT = 274;

    private static final int MAXIMUM_INPUT_COUNT = 100;

    private int lastInputCount = 2;

    private DataType lastDataType;

    private Combo dataTypes;

    private Combo inputCount;

    private final Map<String, DataType> displayNameToDataType = new HashMap<String, DataType>();

    private final EndpointSelectionPane inputPane;

    private Section section;

    private Composite configurationComposite;

    private final EndpointSelectionPane outputPane;

    public JoinerInputSection() {
        inputPane = new EndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, this, true, null, false);

        outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, this, true, null, false);

        setPanes(inputPane, outputPane);
    }

    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createControls(parent, aTabbedPropertySheetPage);

        section = aTabbedPropertySheetPage.getWidgetFactory().createSection(endpointsComposite, Section.TITLE_BAR);
        section.setText(Messages.joinerConfig);

        configurationComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(section);

        configurationComposite.setLayout(new GridLayout(2, false));

        Label inputTypeLabel = new Label(configurationComposite, SWT.NONE);
        inputTypeLabel.setText(Messages.inputType);

        dataTypes = new Combo(configurationComposite, SWT.READ_ONLY);
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        dataTypes.setLayoutData(gridData);
        dataTypes.addSelectionListener(new DataTypeChangedListener());

        Label inputCountLabel = new Label(configurationComposite, SWT.NONE);
        inputCountLabel.setText(Messages.inputCount);
        inputCount = new Combo(configurationComposite, SWT.READ_ONLY);
        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        inputCount.setLayoutData(gridData);
        for (int i = 1; i <= MAXIMUM_INPUT_COUNT; i++) {
            inputCount.add("" + i);
        }
        inputCount.addSelectionListener(new InputCountChangedListener());

        Composite emptyComposite = new Composite(endpointsComposite, SWT.NONE);
        Display display = Display.getCurrent();
        emptyComposite.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
        new Label(emptyComposite, SWT.NONE);

        section.moveAbove(inputPane.getControl());
        emptyComposite.moveAbove(inputPane.getControl());

        section.setClient(configurationComposite);
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();

        dataTypes.removeAll();
        for (DataType dataType : getConfiguration().getInputDescriptionsManager()
            .getDynamicEndpointDefinition(JoinerComponentConstants.DYNAMIC_INPUT_ID).getPossibleDataTypes()) {
            dataTypes.add(dataType.getDisplayName());
            displayNameToDataType.put(dataType.getDisplayName(), dataType);
        }

        lastDataType =
            getConfiguration().getOutputDescriptionsManager().getEndpointDescription(JoinerComponentConstants.OUTPUT_NAME).getDataType();
        dataTypes.select(dataTypes.indexOf(lastDataType.getDisplayName()));

        ConfigurationDescription config =
            getConfiguration().getConfigurationDescription();
        if (getProperty(JoinerComponentConstants.OUTPUT_NAME) != null) {
            lastInputCount =
                Integer.valueOf(config.getConfigurationValue(JoinerComponentConstants.OUTPUT_NAME));
        } else {
            lastInputCount = getInputs().size();
        }
        inputCount.select(inputCount.indexOf("" + lastInputCount));
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        configurationComposite.setSize(parentComposite.getParent().getSize().x, COMPOSITES_HEIGHT);
        endpointsComposite.setSize(parentComposite.getParent().getSize().x, COMPOSITES_HEIGHT);
        parentComposite.setSize(parentComposite.getParent().getSize().x, COMPOSITES_HEIGHT);
        inputCount.setSize(CONFIG_COMBO_WIDTH, inputCount.getSize().y);
        dataTypes.setSize(CONFIG_COMBO_WIDTH, dataTypes.getSize().y);
    };

    /**
     * Edits endpoint's data types.
     * 
     * @author Sascha Zur
     * @author Doreen Seider
     */
    private class DataTypeChangedListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent arg0) {

            DataType newDataType = displayNameToDataType.get(dataTypes.getText());

            EndpointDescription oldInput = getConfiguration().getInputDescriptionsManager()
                .getEndpointDescription(JoinerComponentConstants.INPUT_NAME + "001");
            EndpointDescription oldOutput = getConfiguration().getOutputDescriptionsManager()
                .getEndpointDescription(JoinerComponentConstants.OUTPUT_NAME);

            if (EndpointHandlingHelper.editEndpointDataType(EndpointType.INPUT, oldInput, newDataType)
                && EndpointHandlingHelper.editEndpointDataType(EndpointType.OUTPUT, oldOutput, newDataType)) {

                execute(new JoinerEditDynamicEndpointCommand(lastInputCount, lastDataType, newDataType, dataTypes, inputPane,
                    outputPane));

                lastDataType = newDataType;
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
            int newCount = inputCount.getSelectionIndex() + 1;
            if (newCount != getInputs().size()) {
                execute(new JoinerAddOrRemoveDynamicEndpointsCommand(getInputs().size(), lastInputCount, newCount, lastDataType,
                    inputCount,
                    inputPane));
                lastInputCount = newCount;
                ConfigurationDescription config = getConfiguration().getConfigurationDescription();
                config.setConfigurationValue(JoinerComponentConstants.OUTPUT_NAME, String.valueOf(newCount));
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

}
