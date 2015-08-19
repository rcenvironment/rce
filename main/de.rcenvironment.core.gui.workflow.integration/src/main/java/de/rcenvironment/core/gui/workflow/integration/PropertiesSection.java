/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationMetaDataDefinition;
import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;

/**
 * Section for showing the dynamic property groups.
 * 
 * @author Sascha Zur
 */
public class PropertiesSection extends ValidatingWorkflowNodePropertySection {

    private static final int LIST_HEIGHT = 300;

    private static final int LIST_WIDTH = 150;

    private static final int COMLUMN_WIDTH = 200;

    private List propGroupList;

    private Table propertyTable;

    private int groupSelection;

    private TableColumn valueColumn;

    private TableColumn nameColumn;

    @Override
    protected void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        final Section propertiesSection = PropertyTabGuiHelper.createSingleColumnSectionComposite(parent, getWidgetFactory(),
            Messages.propertyConfiguration);

        Composite propertiesComposite = getWidgetFactory().createFlatFormComposite(propertiesSection);
        propertiesComposite.setLayout(new GridLayout(2, false));
        propertiesComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        new Label(propertiesComposite, SWT.NONE).setText(Messages.propGroupsLabel);
        new Label(propertiesComposite, SWT.NONE).setText(Messages.properties);

        propGroupList = new List(propertiesComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData groupsListGridData = new GridData();
        groupsListGridData.widthHint = LIST_WIDTH;
        groupsListGridData.heightHint = LIST_HEIGHT;
        propGroupList.setLayoutData(groupsListGridData);
        propGroupList.addSelectionListener(new PropertyGroupTableListener(propGroupList));

        propertyTable = new Table(propertiesComposite, SWT.MULTI | SWT.HIDE_SELECTION | SWT.BORDER);
        propertyTable.setHeaderVisible(true);
        propertyTable.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        nameColumn = new TableColumn(propertyTable, SWT.NONE);
        nameColumn.setText(Messages.name);
        nameColumn.setResizable(true);
        nameColumn.setWidth(COMLUMN_WIDTH);
        valueColumn = new TableColumn(propertyTable, SWT.NONE);
        valueColumn.setText(Messages.value);
        valueColumn.setResizable(false);
        valueColumn.setWidth(COMLUMN_WIDTH);

        propertyTable.setLinesVisible(true);

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
            ConfigurationMetaDataDefinition metadata =
                getConfiguration().getConfigurationDescription().getComponentConfigurationDefinition()
                    .getConfigurationMetaDataDefinition();

            java.util.List<String> groupKeys = new LinkedList<String>();
            for (String configKey : config.keySet()) {
                if (metadata.getGuiGroupName(configKey).equals(group)) {
                    groupKeys.add(configKey);
                }
            }

            Collections.sort(groupKeys);

            for (final String groupKey : groupKeys) {
                TableItem configItem = new TableItem(propertyTable, SWT.NONE);
                configItem.setText(metadata.getGuiName(groupKey));
                TableEditor editor = new TableEditor(propertyTable);
                Text textField = new Text(propertyTable, SWT.NONE);
                textField.setText(config.get(groupKey));
                textField.addModifyListener(new ModifyListener() {

                    @Override
                    public void modifyText(ModifyEvent arg0) {
                        setProperty(groupKey, ((Text) arg0.getSource()).getText());
                    }
                });

                editor.grabHorizontal = true;
                editor.setEditor(textField, configItem, 1);
            }
        }
    }

    @Override
    public void aboutToBeShown() {
        super.aboutToBeShown();
        propGroupList.removeAll();
        ConfigurationDefinition config = getConfiguration().getConfigurationDescription()
            .getComponentConfigurationDefinition();

        Set<String> groupNames = new TreeSet<String>();
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
                setProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                    ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE);
            } else if (deleteAlwaysActive) {
                setProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                    ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS);
            } else {
                setProperty(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR,
                    ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER);
            }
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        aboutToBeShown();
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

}
