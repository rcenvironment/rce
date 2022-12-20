/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationMetaDataDefinition;
import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Section for showing the dynamic property groups.
 * 
 * @author Sascha Zur
 * @author Kathrin Schaffert
 */
public class PropertiesSection extends ValidatingWorkflowNodePropertySection {

    private static final int COLUMN_WEIGHT = 8;

    private static final int COLUMN_WEIGHT_PLACEHOLDER = 6;

    private static final int COLUMN_WEIGHT_COMMENT = 20;

    private static final int LIST_WIDTH = 150;

    private static final int MIN_TABLE_WIDTH = 400;

    private List propGroupList;

    private Table propertyTable;

    private int groupSelection;

    private Map<String, String> previousValues;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        parent.setLayout(new GridLayout(1, true));

        final Composite composite = getWidgetFactory().createFlatFormComposite(parent);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        composite.setLayout(new GridLayout(1, true));

        final Section propertiesSection = getWidgetFactory().createSection(composite, Section.TITLE_BAR);
        propertiesSection.setText(Messages.propertyConfiguration);
        propertiesSection.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        Composite propertiesComposite = getWidgetFactory().createFlatFormComposite(propertiesSection);
        propertiesComposite.setLayout(new GridLayout(2, false));
        propertiesComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        new Label(propertiesComposite, SWT.NONE).setText(Messages.propGroupsLabel);
        new Label(propertiesComposite, SWT.NONE).setText(Messages.properties);
        
        propGroupList = new List(propertiesComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData groupsListGridData = new GridData(GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
        groupsListGridData.widthHint = LIST_WIDTH;
        propGroupList.setLayoutData(groupsListGridData);
        propGroupList.addSelectionListener(new PropertyGroupTableListener(propGroupList));

        Composite tableComposite = new Composite(propertiesComposite, SWT.NONE);
        GridData tableGridData =  new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        tableGridData.minimumWidth = MIN_TABLE_WIDTH;
        tableGridData.grabExcessHorizontalSpace = true;
        tableComposite.setLayoutData(tableGridData);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);
        
        propertyTable = new Table(tableComposite, SWT.HIDE_SELECTION | SWT.BORDER);
        propertyTable.setHeaderVisible(true);
        propertyTable.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        propertyTable.setLinesVisible(true);

        TableColumn nameColumn = new TableColumn(propertyTable, SWT.NONE);
        nameColumn.setText(Messages.name);

        TableColumn valueColumn = new TableColumn(propertyTable, SWT.NONE);
        valueColumn.setText(Messages.value);
        valueColumn.setAlignment(SWT.CENTER);

        TableColumn placeholderColumn = new TableColumn(propertyTable, SWT.NONE);
        placeholderColumn.setText("Define at workflow start");
        placeholderColumn.setAlignment(SWT.CENTER);

        TableColumn commentColumn = new TableColumn(propertyTable, SWT.NONE);
        commentColumn.setText("Comment");
        
        // Layout the column weight
        tableLayout.setColumnData(propertyTable.getColumn(0), new ColumnWeightData(COLUMN_WEIGHT));
        tableLayout.setColumnData(propertyTable.getColumn(1), new ColumnWeightData(COLUMN_WEIGHT));
        tableLayout.setColumnData(propertyTable.getColumn(2), new ColumnWeightData(COLUMN_WEIGHT_PLACEHOLDER));
        tableLayout.setColumnData(propertyTable.getColumn(3), new ColumnWeightData(COLUMN_WEIGHT_COMMENT));
        
        TableItem nullItem = new TableItem(propertyTable, SWT.NONE);
        nullItem.setText("");

        TableEditor editor = new TableEditor(propertyTable);
        Text nullText = new Text(propertyTable, SWT.NONE);
        editor.setEditor(nullText, nullItem, 1);

        propertiesSection.setClient(propertiesComposite);

    }

    private void updateSelection() {
        for (org.eclipse.swt.widgets.Control c : propertyTable.getChildren()) {
            c.dispose();
        }
        propertyTable.removeAll();
        if (propGroupList.getItemCount() > groupSelection) {
            String group = propGroupList.getItem(groupSelection);
            Map<String, String> config =
                getConfiguration().getConfigurationDescription().getConfiguration();
            final ConfigurationMetaDataDefinition metadata =
                getConfiguration().getConfigurationDescription().getComponentConfigurationDefinition()
                    .getConfigurationMetaDataDefinition();

            java.util.List<String> groupKeys = new LinkedList<>();
            for (String configKey : config.keySet()) {
                if (metadata.getGuiGroupName(configKey).equals(group)) {
                    groupKeys.add(configKey);
                }
            }

            Collections.sort(groupKeys);
            previousValues = new HashMap<>();
            for (final String groupKey : groupKeys) {
                addSinglePropertyLine(config, groupKey,
                    metadata.getMetaDataValue(groupKey, ConfigurationDefinitionConstants.KEY_METADATA_COMMENT),
                    metadata.getGuiName(groupKey));
            }
        }
    }

    protected void addSinglePropertyLine(Map<String, String> config, final String defaultValuePropertyKey, final String commentString,
        final String displayName) {
        TableItem configItem = new TableItem(propertyTable, SWT.BEGINNING);
        configItem.setText(0, displayName);
        if (commentString != null) {
            configItem.setText(3, commentString);
        }
        TableEditor editor = new TableEditor(propertyTable);
        final Text textField = new Text(propertyTable, SWT.NONE);
        textField.setData(CONTROL_PROPERTY_KEY, defaultValuePropertyKey);
        final Button checkBox = new Button(propertyTable, SWT.CHECK | SWT.CENTER);
        checkBox.setData(CONTROL_PROPERTY_KEY, defaultValuePropertyKey);
        // because of technical issues, we have to distinguish empty string "" and space character string " " here
        // empty string "", if checkbox "Define at workflow start" is checked (default situation)
        // space character string " ", if checkbox is not checked, but no value is set during execution
        // K. Schaffert, 30.04.2019

        textField.setText(config.get(defaultValuePropertyKey));
        
        if (!config.get(defaultValuePropertyKey).matches(ComponentUtils.PLACEHOLDER_REGEX)) { // checkbox
            textField.setEnabled(true);
            checkBox.setSelection(false);
            previousValues.put(defaultValuePropertyKey, config.get(defaultValuePropertyKey));
        } else { // checkbox checked
            textField.setEnabled(false);
            checkBox.setSelection(true);
            previousValues.put(defaultValuePropertyKey, config.get(defaultValuePropertyKey));
        }

        textField.addFocusListener(new PropertyTableFocusListener(defaultValuePropertyKey));

        editor.grabHorizontal = true;
        editor.setEditor(textField, configItem, 1);

        TableEditor boxEditor = new TableEditor(propertyTable);

        checkBox.pack();
        boxEditor.minimumWidth = checkBox.getSize().x;
        boxEditor.horizontalAlignment = SWT.CENTER;
        checkBox.addSelectionListener(new PropertyTableCheckboxSelectionListener(defaultValuePropertyKey, checkBox));
        boxEditor.horizontalAlignment = SWT.CENTER;
        boxEditor.setEditor(checkBox, configItem, 2);
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        propGroupList.removeAll();
        ConfigurationDefinition config = getConfiguration().getConfigurationDescription()
            .getComponentConfigurationDefinition();

        Set<String> groupNames = new TreeSet<>();
        for (String key : config.getConfigurationKeys()) {
            if (!key.equals(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM)) {
                String group = config.getConfigurationMetaDataDefinition().getGuiGroupName(key);
                if (!groupNames.contains(group)) {
                    groupNames.add(group);
                }
            }
        }
        for (String group : groupNames) {
            propGroupList.add(group);
        }

        if (propGroupList.getItemCount() > 0) {
            propGroupList.select(0);
            groupSelection = 0;
        } else {
            TableItem nullItem = new TableItem(propertyTable, SWT.NONE);
            nullItem.setText("");
            TableEditor editor = new TableEditor(propertyTable);
            Text nullText = new Text(propertyTable, SWT.NONE);
            editor.grabHorizontal = true;
            editor.setEditor(nullText, nullItem, 1);
        }
        updateSelection();
        // TODO : create initial mechanism for properties and put this fix there
        // fix for a bug in temp dir section
        ReadOnlyConfiguration readOnlyconfig = getConfiguration().getConfigurationDescription()
            .getComponentConfigurationDefinition().getReadOnlyConfiguration();

        String chosen = getProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR);
        if (chosen == null) {
            boolean deleteOnceActive =
                Boolean.parseBoolean(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE));
            boolean deleteAlwaysActive =
                Boolean.parseBoolean(readOnlyconfig.getValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS));
            if (deleteOnceActive) {
                setPropertyNotUndoable(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                    ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE);
            } else if (deleteAlwaysActive) {
                setPropertyNotUndoable(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                    ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS);
            } else {
                setPropertyNotUndoable(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                    ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER);
            }
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        aboutToBeShown();
    }

    private final class PropertyTableFocusListener implements FocusListener {

        private final String defaultValuePropertyKey;

        private PropertyTableFocusListener(String defaultValuePropertyKey) {
            this.defaultValuePropertyKey = defaultValuePropertyKey;
        }

        @Override
        public void focusLost(FocusEvent arg0) {
            getPreviousValues().put(defaultValuePropertyKey, ((Text) arg0.getSource()).getText());
            setProperty(defaultValuePropertyKey, ((Text) arg0.getSource()).getText());
        }

        @Override
        public void focusGained(FocusEvent arg0) {
            // currently not needed
        }

        private Map<String, String> getPreviousValues() {
            return previousValues;
        }
    }

    private final class PropertyTableCheckboxSelectionListener implements SelectionListener {

        private final String defaultValuePropertyKey;

        private final Button checkBox;

        private PropertyTableCheckboxSelectionListener(String defaultValuePropertyKey, Button checkBox) {
            this.defaultValuePropertyKey = defaultValuePropertyKey;
            this.checkBox = checkBox;
        }

        @Override
        public void widgetSelected(SelectionEvent evt) {

            if (checkBox.getSelection()) {
                onCheckboxChecked(defaultValuePropertyKey);
            } else {
                onCheckboxUnchecked(defaultValuePropertyKey);
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent evt) {
            widgetSelected(evt);
        }

        protected void onCheckboxChecked(final String groupKey) {
            setProperty(groupKey, "${property." + groupKey + "}");
        }

        protected void onCheckboxUnchecked(final String groupKey) {
            if (previousValues.get(groupKey).matches(ComponentUtils.PLACEHOLDER_REGEX)) {
                setProperty(groupKey, "");
            } else {
                setProperty(groupKey, previousValues.get(groupKey));
            }
        }
    }

    /**
     * {@link SelectionListener} for the property group list.
     * 
     * @author Sascha Zur
     */
    private final class PropertyGroupTableListener implements SelectionListener {

        private final List tabsList;

        private PropertyGroupTableListener(List tabsList) {
            this.tabsList = tabsList;
        }

        @Override
        public void widgetSelected(SelectionEvent event) {
            int[] selectedItems = tabsList.getSelectionIndices();
            if (selectedItems.length == 1) {
                groupSelection = selectedItems[0];
            }
            updateSelection();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    @Override
    protected PropertiesSectionUpdater createUpdater() {
        return new PropertiesSectionUpdater();
    }

    /**
     * Properties Section {@link DefaultUpdater} implementation of the handler to update the Configuration UI of an integrated tool.
     * 
     * @author Kathrin Schaffert
     * 
     */
    protected class PropertiesSectionUpdater extends DefaultUpdater {

        @Override
        public void updateControl(Control control, String propertyName, String newValue, String oldValue) {

            propertyTable.setRedraw(false);
            if (control instanceof Button) {
                for (Control c : control.getParent().getChildren()) {
                    if (!(c instanceof Text) || !c.getData(CONTROL_PROPERTY_KEY).equals(control.getData(CONTROL_PROPERTY_KEY))) {
                        continue;
                    }
                    if (newValue == null || newValue.matches(ComponentUtils.PLACEHOLDER_REGEX)) { // checkbox checked
                        ((Text) c).setEnabled(false);
                        ((Button) control).setSelection(true);
                    } else { // checkbox unchecked
                        ((Text) c).setEnabled(true);
                        ((Button) control).setSelection(false);
                    }

                }
            }

            if (control instanceof Text) {
                if (newValue == null) {
                    ((Text) control).setText("${placeholder." + propertyName + "}");
                } else {
                    ((Text) control).setText(newValue);
                }
            }
            
            propertyTable.setRedraw(true);
        }
    }
}
