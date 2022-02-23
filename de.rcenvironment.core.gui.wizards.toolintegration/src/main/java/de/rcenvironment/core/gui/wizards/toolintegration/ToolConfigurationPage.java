/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * @author Sascha Zur
 */
public class ToolConfigurationPage extends ToolIntegrationWizardPage {

    private static final String HELP_CONTEXT_ID = "de.rcenvironment.core.gui.wizard.toolintegration.integration_launchsettings";

    private static final int INDENT_KEEP_ON_ERROR_BUTTON = 20;

    private static final int TOOL_CONFIG_TABLE_HEIGHT = 100;

    private Table toolConfigTable;

    private Button tableButtonAdd;

    private Button tableButtonEdit;

    private Button tableButtonRemove;

    private Map<String, Object> configurationMap;

    private Button iterationDirectoryCheckbox;

    private Button copyNeverButton;

    private Button copyOnceButton;

    private Button copyAlwaysButton;

    private Button deleteTempDirNeverCheckbox;

    private Button deleteTempDirOnceCheckbox;

    private Button deleteTempDirAlwaysCheckbox;

    private Button deleteTempDirNotOnErrorIterationCheckbox;

    private Button deleteTempDirNotOnErrorOnceCheckbox;

    protected ToolConfigurationPage(String pageName, Map<String, Object> configurationMap) {
        super(pageName);
        setTitle(pageName);
        setDescription(Messages.toolConfigPageDescription);
        this.configurationMap = configurationMap;
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 2;
        GridData g = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        container.setLayoutData(g);

        createTable(container);
        Composite checkboxComposite = new Composite(container, SWT.NONE);
        checkboxComposite.setLayout(new GridLayout(1, false));
        GridData iterationCheckboxCompositeData = new GridData();
        iterationCheckboxCompositeData.horizontalSpan = 2;
        checkboxComposite.setLayoutData(iterationCheckboxCompositeData);

        // tool publication mixed into tool integration is disabled for now; see Mantis #16044
        // publishCheckbox = createSingleCheckboxes(checkboxComposite, Messages.publishComponentCheckbox,
        // ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);

        iterationDirectoryCheckbox = createSingleCheckboxes(checkboxComposite, Messages.useIterationDirectoriesText,
            ToolIntegrationConstants.KEY_TOOL_USE_ITERATION_DIRECTORIES);

        Composite groupsComposite = new Composite(container, SWT.NONE);
        groupsComposite.setLayout(new GridLayout(2, false));
        GridData groupCompositeData = new GridData();
        groupCompositeData.horizontalSpan = 2;
        groupsComposite.setLayoutData(groupCompositeData);

        Group copyToolBehaviorGroup = new Group(groupsComposite, SWT.NONE);
        copyToolBehaviorGroup.setText(Messages.copyGroupTitle);
        copyToolBehaviorGroup.setLayout(new GridLayout(1, false));
        copyToolBehaviorGroup.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));

        copyNeverButton =
            createCopyRadioButton(copyToolBehaviorGroup, Messages.copyNever,
                ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_NEVER);
        copyNeverButton.setSelection(true);
        copyOnceButton =
            createCopyRadioButton(copyToolBehaviorGroup, Messages.copyOnce,
                ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ONCE);
        copyAlwaysButton =
            createCopyRadioButton(copyToolBehaviorGroup, Messages.copyAlways,
                ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ALWAYS);

        Group deleteTempDirBehaviorGroup = new Group(groupsComposite, SWT.NONE);
        deleteTempDirBehaviorGroup.setText(Messages.deleteGroupTitle);
        deleteTempDirBehaviorGroup.setLayout(new GridLayout(1, false));
        deleteTempDirBehaviorGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        deleteTempDirNeverCheckbox =
            createDeleteTempDirectoryCheckbox(
                deleteTempDirBehaviorGroup, Messages.deleteNever,
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER);
        deleteTempDirOnceCheckbox =
            createDeleteTempDirectoryCheckbox(
                deleteTempDirBehaviorGroup, Messages.deleteOnce,
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE);
        deleteTempDirOnceCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                deleteTempDirNotOnErrorOnceCheckbox.setEnabled(deleteTempDirOnceCheckbox.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        deleteTempDirNotOnErrorOnceCheckbox =
            createDeleteTempDirectoryCheckbox(deleteTempDirBehaviorGroup, Messages.deleteNotOnErrorOnce,
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ONCE);
        GridData keepOnErrorOnceData = new GridData();
        keepOnErrorOnceData.horizontalIndent = INDENT_KEEP_ON_ERROR_BUTTON;
        deleteTempDirNotOnErrorOnceCheckbox.setLayoutData(keepOnErrorOnceData);
        deleteTempDirAlwaysCheckbox =
            createDeleteTempDirectoryCheckbox(
                deleteTempDirBehaviorGroup, Messages.deleteAlways,
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS);
        deleteTempDirAlwaysCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                deleteTempDirNotOnErrorIterationCheckbox.setEnabled(deleteTempDirAlwaysCheckbox.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        deleteTempDirNotOnErrorIterationCheckbox =
            createDeleteTempDirectoryCheckbox(deleteTempDirBehaviorGroup, Messages.deleteNotOnErrorIteration,
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ITERATION);
        GridData keepOnErrorIterationData = new GridData();
        keepOnErrorIterationData.horizontalIndent = INDENT_KEEP_ON_ERROR_BUTTON;
        deleteTempDirNotOnErrorIterationCheckbox.setLayoutData(keepOnErrorIterationData);
        new Label(deleteTempDirBehaviorGroup, SWT.NONE).setText(Messages.deleteToolNote);
        updateCheckBoxes();
        updateButtonActivation();
        setPageComplete(false);
        setControl(container);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(),
            HELP_CONTEXT_ID);
        updatePageComplete();
    }

    private Button createCopyRadioButton(Composite copyToolBehaviorGroup, String text, final String key) {
        Button newButton = new Button(copyToolBehaviorGroup, SWT.RADIO);
        newButton.setText(text);
        newButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR,
                    key);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });
        return newButton;
    }

    private Button createDeleteTempDirectoryCheckbox(Composite container, String text, final String key) {
        Button checkBox = new Button(container, SWT.CHECK);
        checkBox.setText(text);
        GridData checkBoxData = new GridData();
        checkBox.setLayoutData(checkBoxData);
        checkBox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(key, ((Button) arg0.getSource()).getSelection());
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        return checkBox;
    }

    private Button createSingleCheckboxes(Composite container, String text, final String key) {
        Button checkBox = new Button(container, SWT.CHECK);
        checkBox.setText(text);
        GridData checkBoxData = new GridData();
        checkBox.setLayoutData(checkBoxData);
        checkBox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(key, ((Button) arg0.getSource()).getSelection());
                if (arg0.getSource().equals(iterationDirectoryCheckbox)) {
                    if (copyAlwaysButton.getSelection() && !((Button) arg0.getSource()).getSelection()) {
                        copyAlwaysButton.setSelection(false);
                        copyOnceButton.setSelection(true);
                        configurationMap.put(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR,
                            ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ONCE);
                    }
                    if (deleteTempDirAlwaysCheckbox.getSelection() && !((Button) arg0.getSource()).getSelection()) {
                        deleteTempDirAlwaysCheckbox.setSelection(false);
                        deleteTempDirNotOnErrorIterationCheckbox.setSelection(false);
                        configurationMap.put(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS, false);
                    }
                    copyAlwaysButton.setEnabled(((Button) arg0.getSource()).getSelection());
                    deleteTempDirAlwaysCheckbox.setEnabled(((Button) arg0.getSource()).getSelection());
                    deleteTempDirNotOnErrorIterationCheckbox.setEnabled(((Button) arg0.getSource()).getSelection()
                        && deleteTempDirAlwaysCheckbox.isEnabled() && deleteTempDirAlwaysCheckbox.getSelection());
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        return checkBox;
    }

    private void createTable(Composite client) {
        final Composite tableComposite = new Composite(client, SWT.NONE);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);
        toolConfigTable = new Table(tableComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        toolConfigTable.setHeaderVisible(true);
        GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);
        tableLayoutData.heightHint = TOOL_CONFIG_TABLE_HEIGHT;
        tableComposite.setLayoutData(tableLayoutData);

        // first column - name
        TableColumn col1 = new TableColumn(toolConfigTable, SWT.NONE);
        col1.setText(Messages.host);
        // second column - data type
        TableColumn col2 = new TableColumn(toolConfigTable, SWT.NONE);
        col2.setText(Messages.toolDirectory);
        TableColumn col3 = new TableColumn(toolConfigTable, SWT.NONE);
        col3.setText(Messages.version);
        TableColumn col4 = new TableColumn(toolConfigTable, SWT.NONE);
        col4.setText(Messages.rootWorkingDir);
        // layout data for the columns
        final int columnWeight = 30;
        tableLayout.setColumnData(col1, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(col2, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(col3, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(col4, new ColumnWeightData(columnWeight, true));

        //
        tableButtonAdd = new Button(client, SWT.FLAT);
        tableButtonAdd.setText(Messages.add);
        tableButtonAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        tableButtonAdd.addSelectionListener(new ButtonSelectionListener(tableButtonAdd, toolConfigTable));
        tableButtonEdit = new Button(client, SWT.FLAT);
        tableButtonEdit.setText(Messages.edit);
        tableButtonEdit.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        tableButtonEdit.addSelectionListener(new ButtonSelectionListener(tableButtonEdit, toolConfigTable));
        tableButtonRemove = new Button(client, SWT.FLAT);
        tableButtonRemove.setText(Messages.remove);
        tableButtonRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        tableButtonRemove.addSelectionListener(new ButtonSelectionListener(tableButtonRemove, toolConfigTable));

        fillContextMenu(toolConfigTable);

        toolConfigTable.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateButtonActivation();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        toolConfigTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(MouseEvent e) {

                TableItem[] selection = toolConfigTable.getSelection();

                editSelection(selection);

            }
        });

        toolConfigTable.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {

                if (e.keyCode == SWT.DEL) {

                    TableItem[] selection = toolConfigTable.getSelection();
                    removeSelection(selection);

                    updateTable();
                    updateButtonActivation();

                }
            }
        });

    }

    @SuppressWarnings("unchecked")
    private void updatePageComplete() {
        String errorMessage = "";

        boolean isPageComplete = true;

        final List<Map<String, String>> launchSettings =
            (List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);
        if (!(launchSettings != null && !launchSettings.isEmpty())) {
            errorMessage += Messages.toolExecutionConfigurationNeeded + "\n";
            isPageComplete &= false;
        } else if (!deleteTempDirAlwaysCheckbox.getSelection() && !deleteTempDirNeverCheckbox.getSelection()
            && !deleteTempDirOnceCheckbox.getSelection()) {
            errorMessage += Messages.tempDirBehaviourNeeded;
            isPageComplete &= false;
        }

        if (!isPageComplete) {
            setMessage(errorMessage, DialogPage.ERROR);
        } else {
            if (toolConfigTable.getItemCount() > 0) {
                Optional<String> error = ComponentIdRules.validateComponentVersionRules(toolConfigTable.getItem(0).getText(2));
                if (error.isPresent()) {
                    setMessage(StringUtils.format("The chosen version is not valid.\n %s.", error.get()), DialogPage.ERROR);
                    isPageComplete = false;
                } else {
                    setMessage("Currently, only one launch setting is possible", DialogPage.WARNING);
                }
            } else {
                setMessage(errorMessage, DialogPage.NONE);
            }
        }
        setPageComplete(isPageComplete);

    }

    private void updateCheckBoxes() {
        // deprecated, remove when done; see Mantis #16044
        // if (configurationMap.get(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT) != null
        // && (Boolean) configurationMap.get(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT)) {
        // publishCheckbox.setSelection(true);
        // } else {
        // publishCheckbox.setSelection(false);
        // }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_USE_ITERATION_DIRECTORIES) != null
            && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_USE_ITERATION_DIRECTORIES)) {
            iterationDirectoryCheckbox.setSelection(true);
            copyAlwaysButton.setEnabled(true);
            deleteTempDirAlwaysCheckbox.setEnabled(true);
        } else {
            copyAlwaysButton.setEnabled(false);
            copyAlwaysButton.setSelection(false);
            deleteTempDirAlwaysCheckbox.setEnabled(false);
            iterationDirectoryCheckbox.setSelection(false);
        }

        ToolIntegrationContext context = ((ToolIntegrationWizard) getWizard()).getCurrentContext();
        copyNeverButton.setEnabled(true);
        copyOnceButton.setEnabled(true);
        copyAlwaysButton.setEnabled(iterationDirectoryCheckbox.getSelection());
        if (context.getDisabledIntegrationKeys() != null) {
            for (String key : context.getDisabledIntegrationKeys()) {
                if (ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_NEVER.equals(key)) {
                    copyNeverButton.setEnabled(false);
                }
                if (ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ONCE.equals(key)) {
                    copyOnceButton.setEnabled(false);
                }
                if (ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ALWAYS.equals(key)) {
                    copyAlwaysButton.setEnabled(false);
                }
            }
        }

        if (configurationMap.get(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR) != null) {
            if (((String) configurationMap.get(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR))
                .equals(ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_NEVER)) {
                copyNeverButton.setSelection(true);
                copyOnceButton.setSelection(false);
                copyAlwaysButton.setSelection(false);
            } else if (((String) configurationMap.get(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR))
                .equals(ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ONCE)) {
                copyOnceButton.setSelection(true);
                copyNeverButton.setSelection(false);
                copyAlwaysButton.setSelection(false);
            } else if (((String) configurationMap.get(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR))
                .equals(ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ALWAYS)) {
                if (copyAlwaysButton.isEnabled()) {
                    copyAlwaysButton.setSelection(true);
                    copyNeverButton.setSelection(false);
                    copyOnceButton.setSelection(false);
                } else {
                    copyAlwaysButton.setSelection(false);
                    copyNeverButton.setSelection(false);
                    copyOnceButton.setSelection(true);
                }
            }

        } else {
            if (copyNeverButton.isEnabled()) {
                copyAlwaysButton.setSelection(false);
                copyOnceButton.setSelection(false);
                copyNeverButton.setSelection(true);
                configurationMap.put(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR,
                    ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_NEVER);
            } else if (copyAlwaysButton.isEnabled()) {
                copyAlwaysButton.setSelection(true);
                copyNeverButton.setSelection(false);
                copyOnceButton.setSelection(false);
                configurationMap.put(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR,
                    ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ALWAYS);
            } else {
                configurationMap.put(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR,
                    ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ONCE);
            }

        }
        if (!copyNeverButton.isEnabled() && copyNeverButton.getSelection()) {
            copyOnceButton.setSelection(true);
            copyNeverButton.setSelection(false);
        }
        if (!copyOnceButton.isEnabled() && copyOnceButton.getSelection()) {
            copyNeverButton.setSelection(true);
            copyOnceButton.setSelection(false);
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER) != null
            && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER)) {
            deleteTempDirNeverCheckbox.setSelection(true);
        } else {
            deleteTempDirNeverCheckbox.setSelection(false);
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE) != null
            && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE)) {
            deleteTempDirOnceCheckbox.setSelection(true);
        } else {
            deleteTempDirOnceCheckbox.setSelection(false);
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS) != null
            && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS)) {
            deleteTempDirAlwaysCheckbox.setSelection(true);
        } else {
            deleteTempDirAlwaysCheckbox.setSelection(false);
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ITERATION) != null
            && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ITERATION)) {
            deleteTempDirNotOnErrorIterationCheckbox.setSelection(true);
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ONCE) != null
            && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ONCE)) {
            deleteTempDirNotOnErrorOnceCheckbox.setSelection(true);
        }
        deleteTempDirNotOnErrorIterationCheckbox.setEnabled(deleteTempDirAlwaysCheckbox.isEnabled()
            && deleteTempDirAlwaysCheckbox.getSelection());
        deleteTempDirNotOnErrorOnceCheckbox.setEnabled(deleteTempDirOnceCheckbox.getSelection());
        deleteTempDirNotOnErrorOnceCheckbox.setSelection(deleteTempDirNotOnErrorOnceCheckbox.getSelection()
            && deleteTempDirNotOnErrorOnceCheckbox.isEnabled());
        deleteTempDirNotOnErrorIterationCheckbox.setSelection(deleteTempDirNotOnErrorIterationCheckbox.getSelection()
            && deleteTempDirNotOnErrorIterationCheckbox.isEnabled() && iterationDirectoryCheckbox.getSelection());
    }

    @SuppressWarnings("unchecked")
    private void updateTable() {
        toolConfigTable.removeAll();
        if (configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS) != null) {
            List<Map<String, String>> configs =
                (List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);

            for (Map<String, String> currentConfig : configs) {
                TableItem item = new TableItem(toolConfigTable, SWT.None);
                item.setText(0, currentConfig.get(ToolIntegrationConstants.KEY_HOST));
                item.setText(1, currentConfig.get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY));
                item.setText(2, currentConfig.get(ToolIntegrationConstants.KEY_VERSION));
                String rootDir = currentConfig.get(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY);
                if (rootDir == null || rootDir.isEmpty()) {
                    item.setText(3, "RCE temp directory");
                } else {
                    item.setText(3, currentConfig.get(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY));
                }
            }
        }
        updatePageComplete();
    }

    private void updateButtonActivation() {
        boolean hasOneConfig = toolConfigTable.getItemCount() > 0;
        tableButtonAdd.setEnabled(!hasOneConfig);
        boolean enabled =
            (toolConfigTable.getSelection() != null && toolConfigTable.getSelectionCount() > 0 && toolConfigTable.getItemCount() != 0);
        tableButtonEdit.setEnabled(enabled);
        tableButtonRemove.setEnabled(enabled);
        itemAdd.setEnabled(!hasOneConfig);
        itemEdit.setEnabled(enabled);
        itemRemove.setEnabled(enabled);
    }

    /**
     * Listener for the table buttons.
     * 
     * @author Sascha Zur
     */
    private class ButtonSelectionListener implements SelectionListener {

        private final Button button;

        private final Table table;

        ButtonSelectionListener(Button button, Table table) {
            this.button = button;
            this.table = table;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);

        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            TableItem[] selection = table.getSelection();

            if (button.equals(tableButtonAdd)) {

                addSelection();

            } else if (button.equals(tableButtonEdit)) {

                editSelection(selection);

            } else if (button.equals(tableButtonRemove)) {

                removeSelection(selection);

            }
            updateTable();
            updateButtonActivation();
        }

    }

    /**
     * Sets a new configurationMap and updates all fields.
     * 
     * @param newConfigurationMap new map
     */
    @Override
    public void setConfigMap(Map<String, Object> newConfigurationMap) {
        configurationMap = newConfigurationMap;
        // tool publication mixed into tool integration is disabled for now; see Mantis #16044
        // ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        // ToolIntegrationService integrationService = serviceRegistryAccess.getService(ToolIntegrationService.class);
        // Set<String> published = integrationService.getPublishedComponents();
        // for (String path : published) {
        // if (path.substring(path.lastIndexOf(File.separator) + 1).equals(configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME))) {
        // configurationMap.put(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT, true);
        // }
        // }
        updatePageValues();
    }

    private void updatePageValues() {
        updateCheckBoxes();
        updateTable();
        updateButtonActivation();
        updatePageComplete();
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        helpSystem.displayHelp(HELP_CONTEXT_ID);
    }

    @SuppressWarnings("unchecked")
    private void addSelection() {

        List<Map<String, String>> configs = new LinkedList<>();
        if (configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS) != null) {
            configs = (List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);
        } else {
            configurationMap.put(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS, configs);
        }
        WizardToolConfigurationDialog wtcd =
            new WizardToolConfigurationDialog(null, Messages.addDiaglogTitle, ((ToolIntegrationWizard) getWizard()).getCurrentContext());

        int exit = wtcd.open();
        if (exit == 0) {
            configs.add(wtcd.getConfig());

        }

    }

    private void editSelection(TableItem[] selection) {

        if (selection != null && selection.length > 0) {
            @SuppressWarnings("unchecked") List<Map<String, String>> configs =
                (List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);
            Map<String, String> selectedConfig = null;
            for (Map<String, String> currentConfig : configs) {
                if (currentConfig.get(ToolIntegrationConstants.KEY_HOST).equals(selection[0].getText(0))
                    && currentConfig.get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY).equals(selection[0].getText(1))) {
                    selectedConfig = currentConfig;
                }
            }
            Map<String, String> selectedConfigCopy = new HashMap<>();
            selectedConfigCopy.putAll(selectedConfig);
            WizardToolConfigurationDialog wtcd =
                new WizardToolConfigurationDialog(null, Messages.editDiaglogTitle, selectedConfigCopy,
                    ((ToolIntegrationWizard) getWizard()).getCurrentContext());
            int exit = wtcd.open();
            if (exit == 0) {
                configs.remove(selectedConfig);
                configs.add(wtcd.getConfig());
                configurationMap.remove(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);
                configurationMap.put(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS, configs);
                updateTable();
            }
        }

    }

    @SuppressWarnings("unchecked")
    private void removeSelection(TableItem[] selection) {
        if (selection != null && selection.length > 0) {

            List<Map<String, String>> configs =
                (List<Map<String, String>>) configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);
            Map<String, String> selectedConfig = null;
            for (Map<String, String> currentConfig : configs) {
                if (currentConfig.get(ToolIntegrationConstants.KEY_HOST).equals(selection[0].getText(0))
                    && currentConfig.get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY).equals(selection[0].getText(1))) {
                    selectedConfig = currentConfig;
                }
            }
            configs.remove(selectedConfig);
        }

    }

    @Override
    public void updatePage() {}

    @Override
    protected void onAddClicked() {
        addSelection();
        updateTable();
        updateButtonActivation();
    }

    @Override
    protected void onEditClicked() {
        TableItem[] selection = toolConfigTable.getSelection();
        editSelection(selection);
        updateTable();
        updateButtonActivation();

    }

    @Override
    protected void onRemoveClicked() {

        TableItem[] selection = toolConfigTable.getSelection();
        removeSelection(selection);
        updateTable();
        updateButtonActivation();
    }
}
