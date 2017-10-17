/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * @author Sascha Zur
 */
public class PropertyConfigurationPage extends ToolIntegrationWizardPage {


    /** Constant for the key "key" in the configuration map. */
    public static final String KEY_PROPERTY_KEY = ToolIntegrationConstants.KEY_PROPERTY_KEY;

    /** Constant for the key "displayName" in the configuration map. */
    public static final String KEY_PROPERTY_DISPLAY_NAME = ToolIntegrationConstants.KEY_PROPERTY_DISPLAYNAME;

    /** Constant for the key "defaultValue" in the configuration map. */
    public static final String KEY_PROPERTY_DEFAULT_VALUE = ToolIntegrationConstants.KEY_PROPERTY_DEFAULT_VALUE;

    private static final String STANDARD_SUFFIX = ToolIntegrationConstants.DEFAULT_CONFIG_FILE_SUFFIX;

    private static final String CREATE_CONFIG_FILE = ToolIntegrationConstants.KEY_PROPERTY_CREATE_CONFIG_FILE;

    private static final String CONFIG_FILE_NAME = ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME;

    private static final int PROPERTY_CONFIGURATION_WIDTH = 400;

    private static final int NO_PROPERTY_TAB_SELECTION = -1;

    private static final String DEFAULT_CONFIGURATION_GROUP = "Default";

    private static final String HELP_CONTEXT_ID = "de.rcenvironment.core.gui.wizard.toolintegration.integration_properties";

    protected Map<String, Object> configurationMap;

    private int groupSelection = NO_PROPERTY_TAB_SELECTION;

    private Button createConfigButton;

    private Text configurationFileNameText;

    private Button tableButtonAdd;

    private Button tableButtonEdit;

    private Button tableButtonRemove;

    private Table propertyTable;

    private Map<String, Object> propertyTabMap;

    private Text groupNameText;

    private List propGroupList;

    private Button removeGroupButton;

    private Label configGroupLabel;

    private ButtonSelectionListener btnSelectionListener;

    private Button editGroupButton;

    private Button addGroupButton;

    protected PropertyConfigurationPage(String pageName, Map<String, Object> configurationMap) {
        super(pageName);
        setTitle(pageName);
        setDescription(Messages.propertyPageDescription);
        this.configurationMap = configurationMap;
        if (configurationMap.get(ToolIntegrationConstants.KEY_PROPERTIES) == null) {
            propertyTabMap = new HashMap<String, Object>();
            configurationMap.put(ToolIntegrationConstants.KEY_PROPERTIES, propertyTabMap);
            propertyTabMap.put(DEFAULT_CONFIGURATION_GROUP, new HashMap<String, Object>());
        }
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        GridData containerGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        container.setLayoutData(containerGridData);

        createTabConfiguration(container);
        createPropertyConfiguration(container);
        updateTabListContent();
        if (propGroupList.getItemCount() > 0) {
            propGroupList.setSelection(0);
            groupSelection = 0;
        }
        updateSelection();
        setControl(container);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(),
            HELP_CONTEXT_ID);
        setPageComplete(true);

    }

    private void createTabConfiguration(Composite container) {
        Composite tabConfigurationComposite = new Composite(container, SWT.NONE);
        tabConfigurationComposite.setLayout(new GridLayout(2, false));

        GridData tabConfigurationGridData =
            new GridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.GRAB_HORIZONTAL);
        tabConfigurationComposite.setLayoutData(tabConfigurationGridData);

        Label propertyGroupLabel = new Label(tabConfigurationComposite, SWT.NONE);
        propertyGroupLabel.setText(Messages.configGroupsHeader);
        GridData propertyGroupLabelData = new GridData();
        propertyGroupLabelData.horizontalSpan = 2;
        propertyGroupLabel.setLayoutData(propertyGroupLabelData);

        propGroupList = new List(tabConfigurationComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData tabsListGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL
            | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
        tabsListGridData.horizontalSpan = 2;
        propGroupList.setLayoutData(tabsListGridData);
        propGroupList.addSelectionListener(new PropertyGroupTableListener(propGroupList));
        propGroupList.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {

                if (e.keyCode == SWT.DEL) {

                    if (propGroupList.getSelection().length == 1) {
                        if (propertyTabMap.containsKey(propGroupList.getSelection()[0])) {
                            propertyTabMap.remove(propGroupList.getSelection()[0]);
                            updateTabListContent();
                        }
                    }

                }
            }
        });

        groupNameText = new Text(tabConfigurationComposite, SWT.BORDER);
        GridData tabNameTextGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        groupNameText.setLayoutData(tabNameTextGridData);
        groupNameText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                groupNameText.forceFocus();
                addGroupButton.setEnabled(groupNameText.getText() != null && !groupNameText.getText().isEmpty());
            }
        });
        groupNameText.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR) {
                    e.doit = false;
                }

            }
        });
        addGroupButton = new Button(tabConfigurationComposite, SWT.PUSH);
        addGroupButton.setText(Messages.add);
        GridData addGroupButtonGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        addGroupButton.setLayoutData(addGroupButtonGridData);
        addGroupButton.addSelectionListener(new AddGroupButtonListener());
        addGroupButton.setEnabled(false);
        editGroupButton = new Button(tabConfigurationComposite, SWT.PUSH);
        editGroupButton.setText(Messages.rename);
        GridData editGroupButtonGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        editGroupButton.setLayoutData(editGroupButtonGridData);
        editGroupButtonGridData.horizontalSpan = 2;
        editGroupButton.addSelectionListener(new EditGroupButtonListener());

        removeGroupButton = new Button(tabConfigurationComposite, SWT.PUSH);
        removeGroupButton.setText(Messages.remove);
        GridData removeGroupButtonGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        removeGroupButton.setLayoutData(removeGroupButtonGridData);
        removeGroupButtonGridData.horizontalSpan = 2;
        removeGroupButton.addSelectionListener(new RemoveGroupButtonListener());
    }

    private void createPropertyConfiguration(Composite container) {

        Composite propertyConfigurationComposite = new Composite(container, SWT.NONE);
        propertyConfigurationComposite.setLayout(new GridLayout(2, false));
        GridData propertyConfigurationGridData =
            new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        propertyConfigurationGridData.widthHint = PROPERTY_CONFIGURATION_WIDTH;
        propertyConfigurationComposite.setLayoutData(propertyConfigurationGridData);

        configGroupLabel = new Label(propertyConfigurationComposite, SWT.NONE);
        GridData configGroupLabelData = new GridData();
        configGroupLabelData.horizontalSpan = 2;
        configGroupLabel.setLayoutData(configGroupLabelData);

        createTable(propertyConfigurationComposite);
        Composite createConfigComposite = new Composite(propertyConfigurationComposite, SWT.NONE);
        createConfigComposite.setLayout(new GridLayout(2, false));
        GridData createConfigGridData = new GridData(GridData.FILL_HORIZONTAL);
        createConfigGridData.horizontalSpan = 2;
        createConfigComposite.setLayoutData(createConfigGridData);
        createConfigButton = new Button(createConfigComposite, SWT.CHECK);
        createConfigButton.setText(Messages.createConfigFileButton);
        GridData createConfigButtonGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        createConfigButtonGridData.horizontalSpan = 2;
        createConfigButton.setLayoutData(createConfigButtonGridData);
        createConfigButton.addSelectionListener(new CreateConfigSelectionListener());

        new Label(createConfigComposite, SWT.NONE).setText(ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[1] + "/"
            + ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[0].replace('c', 'C') + "/");

        configurationFileNameText = new Text(createConfigComposite, SWT.BORDER);
        GridData configFilenameTextData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        configurationFileNameText.setLayoutData(configFilenameTextData);
        configurationFileNameText.addModifyListener(new ConfigurationFilenameTextModifyListener());
    }

    private void createTable(Composite client) {
        final Composite tableComposite = new Composite(client, SWT.NONE);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);

        GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);
        tableComposite.setLayoutData(tableLayoutData);

        propertyTable = new Table(tableComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        propertyTable.setHeaderVisible(true);

        GridData tableData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        propertyTable.setLayoutData(tableData);
        fillContextMenu(propertyTable);
        // first column - name
        TableColumn col1 = new TableColumn(propertyTable, SWT.NONE);
        col1.setText(Messages.key);
        // second column - data type
        TableColumn col2 = new TableColumn(propertyTable, SWT.NONE);
        col2.setText(Messages.displayName);
        TableColumn col3 = new TableColumn(propertyTable, SWT.NONE);
        col3.setText(Messages.defaultValue);

        // layout data for the columns
        final int columnWeight = 30;
        tableLayout.setColumnData(col1, new ColumnWeightData(columnWeight - 5, true));
        tableLayout.setColumnData(col2, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(col3, new ColumnWeightData(columnWeight - 5, true));
        //
        tableButtonAdd = new Button(client, SWT.FLAT);
        tableButtonAdd.setText(Messages.add);
        tableButtonAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        tableButtonAdd.addSelectionListener(new ButtonSelectionListener(tableButtonAdd, propertyTable));
        tableButtonEdit = new Button(client, SWT.FLAT);
        tableButtonEdit.setText(Messages.edit);
        tableButtonEdit.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        btnSelectionListener = new ButtonSelectionListener(tableButtonEdit, propertyTable);
        tableButtonEdit.addSelectionListener(btnSelectionListener);
        tableButtonRemove = new Button(client, SWT.FLAT);
        tableButtonRemove.setText(Messages.remove);
        tableButtonRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        tableButtonRemove.addSelectionListener(new ButtonSelectionListener(tableButtonRemove, propertyTable));

        propertyTable.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateButtonActivation();
            }

            private void updateButtonActivation() {
                if (propertyTable.getSelectionCount() > 0) {
                    tableButtonRemove.setEnabled(true);
                    tableButtonEdit.setEnabled(true);
                } else {
                    tableButtonRemove.setEnabled(false);
                    tableButtonEdit.setEnabled(false);
                }
            }

        });

        propertyTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(MouseEvent e) {

                TableItem[] selection = propertyTable.getSelection();
                if (selection != null && selection.length > 0) {
                    editProperty(selection);
                    updatePropertyTable();
                }

            }
        });

        propertyTable.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {

                if (e.keyCode == SWT.DEL) {

                    TableItem[] selection = propertyTable.getSelection();
                    removeProperty(selection);
                    updatePropertyTable();

                }
            }
        });

    }

    private void updateTabListContent() {
        String oldSelection = "";
        if (groupSelection != NO_PROPERTY_TAB_SELECTION && propGroupList.getItemCount() > groupSelection) {
            oldSelection = propGroupList.getItem(groupSelection);
        }
        propGroupList.removeAll();
        if (propertyTabMap != null) {
            for (String key : propertyTabMap.keySet()) {
                propGroupList.add(key);
            }
            String[] items = propGroupList.getItems();
            java.util.Arrays.sort(items);
            propGroupList.setItems(items);
            if (propGroupList.indexOf(oldSelection) != NO_PROPERTY_TAB_SELECTION) {
                propGroupList.select(propGroupList.indexOf(oldSelection));
                updatePropertyTable();
            } else if (groupSelection == 0) {
                groupSelection = 1;
                propGroupList.setSelection(0);
                updateSelection();
            } else {
                groupSelection = NO_PROPERTY_TAB_SELECTION;
                updateSelection();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateSelection() {
        if (propGroupList.getItemCount() > 0) {
            if (groupSelection != NO_PROPERTY_TAB_SELECTION) {
                changePropertyTableActivation(true);
                if (groupSelection >= propGroupList.getItemCount()) {
                    groupSelection = 0;
                }
                if (propGroupList.getItemCount() != 0 && propGroupList.getSelection().length > 0) {
                    configGroupLabel.setText(StringUtils.format(Messages.groupConfigHeader, propGroupList.getItem(groupSelection)));
                    Map<String, Object> propertyTabConfig = (Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0]);
                    if (propertyTabConfig.get(CREATE_CONFIG_FILE) != null && (Boolean) propertyTabConfig.get(CREATE_CONFIG_FILE)) {
                        createConfigButton.setSelection(true);
                        configurationFileNameText.setEnabled(true);
                        Map<String, Object> tabProperties = (Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0]);
                        configurationFileNameText.setText((String) tabProperties.get(CONFIG_FILE_NAME));
                    } else {
                        createConfigButton.setSelection(false);
                        configurationFileNameText.setEnabled(false);
                    }
                }
                updatePropertyTable();
                editGroupButton.setEnabled(true);
                removeGroupButton.setEnabled(true);
            } else {

                editGroupButton.setEnabled(false);
                removeGroupButton.setEnabled(false);
                changePropertyTableActivation(false);
                configGroupLabel.setText(Messages.noConfigGroupSelected);
                updatePropertyTable();
            }
        } else {
            editGroupButton.setEnabled(false);
            removeGroupButton.setEnabled(false);
            changePropertyTableActivation(false);
            configGroupLabel.setText(Messages.noConfigGroupSelected);
            updatePropertyTable();
        }
        configGroupLabel.pack();
    }

    private void updateTabList() {
        propGroupList.removeAll();
        @SuppressWarnings("unchecked") Map<String, Object> propGroups =
            (Map<String, Object>) configurationMap.get(ToolIntegrationConstants.KEY_PROPERTIES);
        if (propGroups != null) {
            Set<String> keys = propGroups.keySet();
            for (String groupName : keys) {
                propGroupList.add(groupName);
            }
            groupSelection = NO_PROPERTY_TAB_SELECTION;
            updateSelection();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (propGroupList.getItems().length > 0 && propGroupList.getSelection().length == 0) {
            propGroupList.select(0);
            groupSelection = 0;
        }
        updateSelection();
    }

    @SuppressWarnings("unchecked")
    private void updatePropertyTable() {
        propertyTable.removeAll();
        if (propGroupList.getSelectionCount() > 0) {
            Map<String, Object> tabProperties = (Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0]);

            for (String key : tabProperties.keySet()) {
                if (!(key.equals(CONFIG_FILE_NAME) || key.equals(CREATE_CONFIG_FILE))) {
                    TableItem item = new TableItem(propertyTable, SWT.None);
                    item.setText(0, key);
                    item.setText(1, ((Map<String, String>) tabProperties.get(key)).get(KEY_PROPERTY_DISPLAY_NAME));
                    item.setText(2, ((Map<String, String>) tabProperties.get(key)).get(KEY_PROPERTY_DEFAULT_VALUE));
                }
            }
        }
        tableButtonEdit.setEnabled(propertyTable.getItemCount() != 0);
        tableButtonRemove.setEnabled(propertyTable.getItemCount() != 0);
    }

    private void changePropertyTableActivation(boolean enabled) {
        createConfigButton.setEnabled(enabled);
        configurationFileNameText.setEnabled(enabled);
        tableButtonAdd.setEnabled(enabled);
        tableButtonEdit.setEnabled(enabled);
        tableButtonRemove.setEnabled(enabled);
        propertyTable.setEnabled(enabled);
        removeGroupButton.setEnabled(enabled);

        createConfigButton.setSelection(false);
        propertyTable.clearAll();
    }

    private java.util.List<String> getAllPropertyNames() {
        java.util.List<String> result = new LinkedList<String>();
        for (String tabKey : propertyTabMap.keySet()) {
            @SuppressWarnings("unchecked") Map<String, Object> tab = (Map<String, Object>) propertyTabMap.get(tabKey);
            result.addAll(tab.keySet());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<String> getAllPropertyDisplayNames() {
        java.util.List<String> result = new LinkedList<String>();
        for (String tabKey : propertyTabMap.keySet()) {
            Map<String, Object> tab =
                (Map<String, Object>) propertyTabMap.get(tabKey);
            for (String propKey : tab.keySet()) {
                if (!(tab.get(propKey) instanceof String || tab.get(propKey) instanceof Boolean)) {
                    result.add(((Map<String, String>) tab.get(propKey)).get(KEY_PROPERTY_DISPLAY_NAME));
                }
            }
        }
        return result;
    }

    /**
     * Modify listener for the text widget for the configuration filename.
     * 
     * @author Sascha Zur
     */
    private final class ConfigurationFilenameTextModifyListener implements ModifyListener {

        @SuppressWarnings("unchecked")
        @Override
        public void modifyText(ModifyEvent arg0) {
            if (createConfigButton.getSelection() && propGroupList.getSelection().length > 0) {
                Map<String, Object> propertyTabConfig = (Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0]);
                propertyTabConfig.put(CONFIG_FILE_NAME, configurationFileNameText.getText());
                if (configurationFileNameText.getText().equals("")) {
                    Display display = Display.getCurrent();
                    configurationFileNameText.setBackground(display.getSystemColor(SWT.COLOR_RED));
                    setMessage(Messages.configFilenameInvalid, DialogPage.ERROR);
                } else {
                    Display display = Display.getCurrent();
                    configurationFileNameText.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
                    setMessage(null, DialogPage.NONE);
                }
            }
        }
    }

    /**
     * Listener for the checkbox if a config file should be created.
     * 
     * @author Sascha Zur
     */
    private final class CreateConfigSelectionListener implements SelectionListener {

        @SuppressWarnings("unchecked")
        @Override
        public void widgetSelected(SelectionEvent arg0) {
            Map<String, Object> propertyTabConfig = (Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0]);
            propertyTabConfig.put(CREATE_CONFIG_FILE, createConfigButton.getSelection());
            if (propertyTabConfig.get(CONFIG_FILE_NAME) == null) {
                propertyTabConfig.put(CONFIG_FILE_NAME, propGroupList.getSelection()[0] + STANDARD_SUFFIX);
            }
            updateSelection();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
    }

    /**
     * Listener for the edit button of the tab name list.
     * 
     * @author Sascha Zur
     */
    private final class EditGroupButtonListener implements SelectionListener {

        @SuppressWarnings("unchecked")
        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if (propGroupList.getSelection().length == 1) {
                WizardEditGroupTabNameDialog wegntd = new WizardEditGroupTabNameDialog(null, propGroupList.getSelection()[0]);
                int returnCode = wegntd.open();
                if (returnCode == Dialog.OK) {
                    String newName = wegntd.getNewName();
                    Map<String, Object> oldConfig = (Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0]);
                    if (configurationFileNameText.getText().equals(propGroupList.getSelection()[0] + ".conf")) {
                        configurationFileNameText.setText(newName + ".conf");
                    }
                    if (!newName.equals(propGroupList.getSelection()[0])) {
                        propertyTabMap.remove(propGroupList.getSelection()[0]);
                        propertyTabMap.put(newName, oldConfig);
                    }
                    updateTabListContent();
                    propGroupList.setSelection(propGroupList.indexOf(newName));
                    groupSelection = propGroupList.indexOf(newName);

                    updateSelection();
                    updatePropertyTable();
                }
            }

        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
    }

    /**
     * Listener for the remove button of the tab name list.
     * 
     * @author Sascha Zur
     */
    private final class RemoveGroupButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if (propGroupList.getSelection().length == 1) {
                if (propertyTabMap.containsKey(propGroupList.getSelection()[0])) {
                    propertyTabMap.remove(propGroupList.getSelection()[0]);
                    updateTabListContent();
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
    }

    /**
     * Listener for the add button of the tab name list.
     * 
     * @author Sascha Zur
     */
    private final class AddGroupButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            String newTabName = groupNameText.getText();
            if (propertyTabMap == null) {
                propertyTabMap = new HashMap<String, Object>();
            }
            if (!newTabName.isEmpty() && !propertyTabMap.containsKey(newTabName)) {
                propertyTabMap.put(newTabName, new HashMap<String, Object>());
                groupNameText.setText("");
                Display display = Display.getCurrent();
                groupNameText.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
                setMessage(null, DialogPage.NONE);
                updateTabListContent();
            } else {
                Display display = Display.getCurrent();
                groupNameText.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
                if (!newTabName.isEmpty()) {
                    setMessage(Messages.propTabNameInvalid, DialogPage.WARNING);
                } else {
                    setMessage(Messages.propTabNameEmpty, DialogPage.WARNING);
                }

            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }
    }

    /**
     * {@link SelectionListener} for the property tab list.
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

    /**
     * Listener for the table buttons.
     * 
     * @author Sascha Zur
     */
    private class ButtonSelectionListener implements SelectionListener {

        private final Button button;

        private final Table selectionTable;

        ButtonSelectionListener(Button button, Table table) {
            this.button = button;
            this.selectionTable = table;

        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            TableItem[] selection = selectionTable.getSelection();

            if (button.equals(tableButtonAdd)) {

                addProperty();

            } else if (button.equals(tableButtonEdit)) {
                if (selection != null && selection.length > 0) {

                    editProperty(selection);

                }
            } else if (button.equals(tableButtonRemove)) {

                removeProperty(selection);

            }
            updatePropertyTable();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);

        }

    }

    /**
     * Sets a new configurationMap and updates all fields.
     * 
     * @param newConfigurationMap new map
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setConfigMap(Map<String, Object> newConfigurationMap) {
        configurationMap = newConfigurationMap;
        propertyTabMap = (Map<String, Object>) newConfigurationMap.get(ToolIntegrationConstants.KEY_PROPERTIES);
        if (propertyTabMap == null) {
            propertyTabMap = new HashMap<String, Object>();
            configurationMap.put(ToolIntegrationConstants.KEY_PROPERTIES, propertyTabMap);
            propertyTabMap.put(DEFAULT_CONFIGURATION_GROUP, new HashMap<String, Object>());
        }
        updatePageValues();
    }

    private void updatePageValues() {
        updateTabList();
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        helpSystem.displayHelp(HELP_CONTEXT_ID);
    }

    @Override
    public void updatePage() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onAddClicked() {
        addProperty();
        updatePropertyTable();

    }

    @Override
    protected void onEditClicked() {

        TableItem[] selection = propertyTable.getSelection();
        editProperty(selection);

    }

    @Override
    protected void onRemoveClicked() {

        TableItem[] selection = propertyTable.getSelection();
        removeProperty(selection);
        updatePropertyTable();

    }

    @SuppressWarnings("unchecked")
    private void editProperty(TableItem[] selection) {

        Map<String, String> propertyConfigCopy = new HashMap<String, String>();
        if (propGroupList.getSelection().length > 0 && selection != null && selection.length > 0) {
            propertyConfigCopy.putAll((Map<String, String>) ((Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0]))
                .get(selection[0].getText()));
            String oldpropertyConfig = selection[0].getText();
            WizardPropertyEditDialog wped =
                new WizardPropertyEditDialog(null, Messages.edit + " " + Messages.property, propertyConfigCopy,
                    getAllPropertyNames(), getAllPropertyDisplayNames());
            int exit = wped.open();
            if (exit == Dialog.OK) {
                ((Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0])).remove(oldpropertyConfig);
                ((Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0])).put(
                    wped.getConfig().get(KEY_PROPERTY_KEY),
                    wped.getConfig());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void removeProperty(TableItem[] selection) {

        if (selection != null && selection.length > 0) {

            Map<String, String> propertyConfig =
                (Map<String, String>) ((Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0])).get(selection[0]
                    .getText());
            ((Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0]))
                .remove(propertyConfig.get(KEY_PROPERTY_KEY));
        }

    }

    @SuppressWarnings("unchecked")
    private void addProperty() {

        WizardPropertyEditDialog wped =
            new WizardPropertyEditDialog(null, Messages.add + " " + Messages.property, new HashMap<String, String>(),
                getAllPropertyNames(),
                getAllPropertyDisplayNames());
        int exit = wped.open();
        if (exit == Dialog.OK) {
            ((Map<String, Object>) propertyTabMap.get(propGroupList.getSelection()[0])).put(wped.getConfig().get(KEY_PROPERTY_KEY),
                wped.getConfig());
        }

    }
}
