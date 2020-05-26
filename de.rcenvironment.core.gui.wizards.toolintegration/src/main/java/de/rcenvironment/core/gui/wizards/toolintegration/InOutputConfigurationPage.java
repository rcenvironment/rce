/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.core.utils.common.StringUtils;
import jodd.mail.EmailAddress;

/**
 * @author Sascha Zur
 * @author Goekhan Guerkan
 * @author Doreen Seider
 */
public class InOutputConfigurationPage extends ToolIntegrationWizardPage {

    /** Key for in-/output from configuration map. */
    public static final String FOLDER = ToolIntegrationConstants.KEY_ENDPOINT_FOLDER;

    /** Key for in-/output from configuration map. */
    public static final String DATA_TYPE = ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE;

    /** Key for in-/output from configuration map. */
    public static final String NAME = ToolIntegrationConstants.KEY_ENDPOINT_NAME;

    /** Key for in-/output from configuration map. */
    public static final String FILENAME = ToolIntegrationConstants.KEY_ENDPOINT_FILENAME;

    /** Key for in-/output from configuration map. */
    public static final String USAGE = ToolIntegrationConstants.KEY_ENDPOINT_USAGE;

    /** Key for in-/output from configuration map. */
    public static final String HANDLING = ToolIntegrationConstants.KEY_INPUT_HANDLING;

    /** Key for in-/output from configuration map. */
    public static final String CONSTRAINT = ToolIntegrationConstants.KEY_INPUT_EXECUTION_CONSTRAINT;

    /** Key for in-/output from configuration map. */
    public static final String DEFAULT_HANDLING = ToolIntegrationConstants.KEY_DEFAULT_INPUT_HANDLING;

    /** Key for in-/output from configuration map. */
    public static final String DEFAULT_CONSTRAINT = ToolIntegrationConstants.KEY_DEFAULT_INPUT_EXECUTION_CONSTRAINT;

    /** Key for the input keyword. */
    public static final String INPUTS = ToolIntegrationConstants.KEY_ENDPOINT_INPUTS;

    private static final String HELP_CONTEXT_ID = "de.rcenvironment.core.gui.wizard.toolintegration.integration_inputOutput";

    private static final String OUTPUTS = ToolIntegrationConstants.KEY_ENDPOINT_OUTPUTS;

    private Map<String, Object> configurationMap;

    private ButtonSelectionListener btnSelectionListener;

    private Table inputTable;

    private Table outputTable;

    private Table activTable;

    private Button waitForVerificationButton;

    private Button defaultTokenLocationButton;

    private Button customTokenLocationButton;

    private Text customTokenLocationText;

    private Button chooseCustomTokenLocationButton;

    private Button sendMailButton;

    private Text emailReciepientsText;

    private boolean validateVerificationTab = true;

    protected InOutputConfigurationPage(String pageName, Map<String, Object> configurationMap) {
        super(pageName);
        setTitle(pageName);
        setDescription(Messages.inoutputPageDescription);
        this.configurationMap = configurationMap;
        if (configurationMap.get(INPUTS) == null) {
            configurationMap.put(INPUTS, new LinkedList<Object>());
        }
        if (configurationMap.get(OUTPUTS) == null) {
            configurationMap.put(OUTPUTS, new LinkedList<Object>());
        }
    }

    @Override
    public void createControl(Composite parent) {
        CTabFolder tabFolder = new CTabFolder(parent, SWT.BORDER);
        inputTable = createEndpointTableTab(Messages.inputs, INPUTS, tabFolder);
        outputTable = createEndpointTableTab(Messages.outputs, OUTPUTS, tabFolder);
        createResultVerificationTab(tabFolder);

        updateTable(INPUTS);
        updateTable(OUTPUTS);

        fillContextMenu(inputTable);
        fillContextMenu(outputTable);

        tabFolder.setSelection(0);
        setControl(tabFolder);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(),
            HELP_CONTEXT_ID);
        setPageComplete(true);
    }

    private Table createEndpointTableTab(String name, final String type, CTabFolder tabFolder) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText(name);

        Composite client = new Composite(tabFolder, SWT.NONE);
        client.setLayout(new GridLayout(2, false));
        item.setControl(client);

        final Composite tableComposite = new Composite(client, SWT.NONE);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);
        final Table table = new Table(tableComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);

        table.setHeaderVisible(true);

        GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 5);
        final int minHeight = 140;
        tableLayoutData.heightHint = minHeight; // effectively min height
        tableComposite.setLayoutData(tableLayoutData);

        // first column - name
        TableColumn col1 = new TableColumn(table, SWT.NONE);
        col1.setText(name.substring(0, name.length() - 1));
        // second column - data type
        TableColumn col2 = new TableColumn(table, SWT.NONE);
        col2.setText(Messages.dataType);
        TableColumn col3 = null;
        TableColumn col4 = null;

        /**
         * Commented out because of bug with renaming file / dir
         */
        // TableColumn col5 = null;

        if (type.equals(INPUTS)) {
            col3 = new TableColumn(table, SWT.NONE);
            col3.setText(Messages.inputHandling);

            col4 = new TableColumn(table, SWT.NONE);
            col4.setText(Messages.inputExecutionConstraint);

            table.setData(INPUTS);
            /**
             * Commented out because of bug with renaming file / dir
             */
            // col5 = new TableColumn(table, SWT.NONE);
            // col5.setText(Messages.filename);
        } else {

            table.setData("output");

        }

        // layout data for the columns
        final int columnWeight = 20;
        tableLayout.setColumnData(col1, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(col2, new ColumnWeightData(columnWeight, true));
        if (type.equals(INPUTS)) {
            tableLayout.setColumnData(col3, new ColumnWeightData(columnWeight, true));
            tableLayout.setColumnData(col4, new ColumnWeightData(columnWeight, true));
            // tableLayout.setColumnData(col5, new ColumnWeightData(columnWeight, true));
        }

        Button buttonAdd = new Button(client, SWT.FLAT);
        buttonAdd.setText(Messages.add);
        buttonAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonAdd.addSelectionListener(new ButtonSelectionListener(buttonAdd, table, name.toLowerCase()));
        Button buttonEdit = new Button(client, SWT.FLAT);
        buttonEdit.setText(Messages.edit);
        buttonEdit.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        btnSelectionListener = new ButtonSelectionListener(buttonEdit, table, name.toLowerCase());
        buttonEdit.addSelectionListener(btnSelectionListener);
        buttonEdit.setEnabled(false);

        Button buttonRemove = new Button(client, SWT.FLAT);
        buttonRemove.setText(Messages.remove);
        buttonRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonRemove.addSelectionListener(new ButtonSelectionListener(buttonRemove, table, name.toLowerCase()));
        buttonRemove.setEnabled(false);

        table.addSelectionListener(new TableSelectionListener(table, buttonEdit, buttonRemove));
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                TableItem[] selection = table.getSelection();
                editInputOutput(selection, type);
            }

            @Override
            public void mouseDown(MouseEvent e) {
                activTable = table;
            }
        });

        table.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.DEL) {
                    TableItem[] selection = table.getSelection();
                    removeInputOutput(selection, type);
                }
            }
        });

        return table;
    }

    private void updateButtonActivation(Table table, Button edit, Button remove) {
        boolean enabled = (table.getSelection() != null && table.getSelectionCount() > 0 && table.getItemCount() != 0);
        edit.setEnabled(enabled);
        remove.setEnabled(enabled);
    }

    private void fillTable(String type) {
        Table table;
        if (type.equals(INPUTS)) {
            table = inputTable;
        } else {
            table = outputTable;
        }
        table.removeAll();
        @SuppressWarnings("unchecked") List<Map<String, String>> staticEndpoints = (List<Map<String, String>>) configurationMap.get(type);
        fillCells(staticEndpoints, table, type);
    }

    private void fillCells(List<Map<String, String>> staticEndpoints,
        Table table, String type) {
        if (staticEndpoints != null) {
            Map<String, Map<String, String>> endpointNames = new TreeMap<String, Map<String, String>>();
            for (Map<String, String> endpoint : staticEndpoints) {
                endpointNames.put(endpoint.get(NAME), endpoint);
            }
            for (Entry<String, Map<String, String>> endpointName : endpointNames.entrySet()) {
                Map<String, String> endpoint = endpointName.getValue();
                TableItem item = new TableItem(table, SWT.None);
                item.setText(0, endpoint.get(NAME));
                item.setText(1, DataType.valueOf(endpoint.get(DATA_TYPE)).getDisplayName());
                // migration code: initial, required, optional -> immutable,
                // consuming; required, required if connected
                if (type.equals(INPUTS)) {
                    if (endpoint.containsKey(HANDLING)) {
                        String text = "";
                        for (String handling : StringUtils.splitAndUnescape(endpoint.get(HANDLING))) {
                            text += EndpointDefinition.InputDatumHandling.valueOf(handling).getDisplayName();
                            text += ", ";
                        }
                        item.setText(2, text.substring(0, text.length() - 2));
                    } else if (endpoint.containsKey(USAGE)
                        && endpoint.get(USAGE).equals("initial")) {
                        item.setText(2, EndpointDefinition.InputDatumHandling.Constant.getDisplayName());
                    } else {
                        item.setText(2, EndpointDefinition.InputDatumHandling.Single.getDisplayName());
                    }
                    if (endpoint.containsKey(CONSTRAINT)) {
                        String text = "";
                        for (String constraint : StringUtils
                            .splitAndUnescape(endpoint.get(CONSTRAINT))) {
                            text += EndpointDefinition.InputExecutionContraint.valueOf(constraint).getDisplayName();
                            text += ", ";
                        }
                        item.setText(3, text.substring(0, text.length() - 2));
                    } else if (endpoint.containsKey(USAGE) && endpoint.get(USAGE).equals("optional")) {
                        item.setText(3, EndpointDefinition.InputExecutionContraint.NotRequired.getDisplayName());
                    } else {
                        item.setText(3, EndpointDefinition.InputExecutionContraint.Required.getDisplayName());
                    }
                    /**
                     * Commented out because of bug with renaming file / dir
                     */
                    // if ((DataType.valueOf(endpoint.get(DATA_TYPE)) == DataType.FileReference ||
                    // DataType
                    // .valueOf(endpoint.get(DATA_TYPE)) == DataType.DirectoryReference)
                    // && endpoint.get(FILENAME) != null) {
                    // if (endpoint.get(FILENAME).isEmpty()) {
                    // item.setText(4, Messages.emptyFilenameTable);
                    // } else if (endpoint.get(FILENAME).equals("-")) {
                    // item.setText(
                    // 4,
                    // ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[2]
                    // + File.separator
                    // + endpoint.get(FILENAME));
                    // } else {
                    // item.setText(
                    // 4,
                    // ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[2]
                    // + File.separator
                    // + endpoint.get(NAME)
                    // + File.separator
                    // + endpoint.get(FILENAME));
                    // }
                    // } else {
                    // item.setText(4, "" + "-");
                    // }
                }
            }
        }
    }

    /**
     * @param type if it is for inputs or for outputs
     * @return all current configured in- and outputs.
     */
    @SuppressWarnings("unchecked")
    private List<String> getAllEndpointNames(String type) {
        List<String> result = new LinkedList<String>();
        List<Object> inputEndpoints = (List<Object>) configurationMap.get(type);
        if (inputEndpoints != null) {
            for (Object endpoint : inputEndpoints) {
                Map<String, String> endpointMap = (Map<String, String>) endpoint;
                result.add(endpointMap.get(NAME));
            }
        }
        return result;
    }

    protected void updateTable(String name) {
        fillTable(name);
    }

    private void createResultVerificationTab(CTabFolder tabFolder) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText("Verification");

        Composite client = new Composite(tabFolder, SWT.NONE);
        client.setLayout(new GridLayout(1, false));
        item.setControl(client);

        waitForVerificationButton = new Button(client, SWT.CHECK);
        waitForVerificationButton.setText("Wait for manual output approval (allows manual tool result review and verification)");

        Label label = new Label(client, SWT.WRAP);
        label.setText("Output values are only sent further if they are approved manually. For that, a so called verification key "
            + "is generated after tool execution.\nWith the key, the tool results can either be approved or rejected with a dialog "
            + "under 'Run -> Verify tool results...'.   ");

        final Group tokenGroup = new Group(client, SWT.NONE);
        tokenGroup.setText("Verification Key");
        tokenGroup.setLayout(new GridLayout(2, false));
        GridData layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        tokenGroup.setLayoutData(layoutData);

        defaultTokenLocationButton = new Button(tokenGroup, SWT.RADIO);
        defaultTokenLocationButton.setText("Store verification key in tool working directory (default)");
        defaultTokenLocationButton.setSelection(true);
        layoutData = new GridData();
        layoutData.horizontalSpan = 2;
        defaultTokenLocationButton.setLayoutData(layoutData);

        customTokenLocationButton = new Button(tokenGroup, SWT.RADIO);
        customTokenLocationButton.setText("Store verification key at custom location (on the tool's machine)");
        layoutData = new GridData();
        layoutData.horizontalSpan = 2;
        customTokenLocationButton.setLayoutData(layoutData);

        customTokenLocationText = new Text(tokenGroup, SWT.BORDER);
        layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        customTokenLocationText.setLayoutData(layoutData);

        chooseCustomTokenLocationButton = new Button(tokenGroup, SWT.PUSH);
        chooseCustomTokenLocationButton.setText("  ...  ");
        chooseCustomTokenLocationButton.addSelectionListener(new PathChooserButtonListener(customTokenLocationText, true, getShell()));

        sendMailButton = new Button(tokenGroup, SWT.CHECK);
        sendMailButton.setText("Send verification key via email (provide addresses below separated by ';')");
        layoutData = new GridData();
        layoutData.horizontalSpan = 2;
        sendMailButton.setLayoutData(layoutData);

        emailReciepientsText = new Text(tokenGroup, SWT.BORDER);
        layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        layoutData.horizontalSpan = 2;
        emailReciepientsText.setLayoutData(layoutData);

        setupVerificationTabListener();

        setEnabledStateForVerificationTabElements();
    }

    private void setupVerificationTabListener() {

        customTokenLocationText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent event) {
                validateVerificationTabAndStoreValues();
            }
        });

        customTokenLocationButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                widgetDefaultSelected(event);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                setEnabledStateForCustomVerificationTokenText();
                validateVerificationTabAndStoreValues();
            }
        });

        emailReciepientsText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent event) {
                validateVerificationTabAndStoreValues();
            }
        });

        sendMailButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                widgetDefaultSelected(event);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                setEnabledStateForEmailRecipientText();
                validateVerificationTabAndStoreValues();
            }
        });

        waitForVerificationButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                widgetDefaultSelected(event);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                setEnabledStateForVerificationTabElements();
                validateVerificationTabAndStoreValues();
            }
        });
    }

    private void setEnabledStateForEmailRecipientText() {
        boolean enabled = waitForVerificationButton.getSelection() && sendMailButton.getSelection();
        emailReciepientsText.setEnabled(enabled);
    }

    private void setEnabledStateForCustomVerificationTokenText() {
        boolean enabled = waitForVerificationButton.getSelection() && customTokenLocationButton.getSelection();
        customTokenLocationText.setEnabled(enabled);
        chooseCustomTokenLocationButton.setEnabled(enabled);
    }

    private void setEnabledStateForVerificationTabElements() {
        boolean enabled = waitForVerificationButton.getSelection();
        defaultTokenLocationButton.setEnabled(enabled);
        customTokenLocationButton.setEnabled(enabled);
        customTokenLocationText.setEnabled(customTokenLocationButton.getSelection() && enabled);
        chooseCustomTokenLocationButton.setEnabled(customTokenLocationButton.getSelection() && enabled);
        sendMailButton.setEnabled(enabled);
        emailReciepientsText.setEnabled(sendMailButton.getSelection() && enabled);
    }

    private void updateVerificationTab() {
        boolean waitForVerification = configurationMap.containsKey(ComponentConstants.COMPONENT_CONFIG_KEY_REQUIRES_OUTPUT_APPROVAL)
            && (boolean) configurationMap.get(ComponentConstants.COMPONENT_CONFIG_KEY_REQUIRES_OUTPUT_APPROVAL);
        waitForVerificationButton.setSelection(waitForVerification);

        String customTokenLocation = (String) configurationMap.get(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_LOCATION);
        if (customTokenLocation != null && !customTokenLocation.isEmpty()) {
            customTokenLocationButton.setSelection(true);
            customTokenLocationText.setText(customTokenLocation);
            defaultTokenLocationButton.setSelection(false);
        } else {
            defaultTokenLocationButton.setSelection(true);
            customTokenLocationButton.setSelection(false);
        }

        String emailRecipients = (String) configurationMap.get(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_RECIPIENTS);
        if (emailRecipients != null && !emailRecipients.isEmpty()) {
            sendMailButton.setSelection(true);
            emailReciepientsText.setText(emailRecipients);
        }
        setEnabledStateForVerificationTabElements();
    }

    private void validateVerificationTabAndStoreValues() {
        if (!validateVerificationTab) {
            return;
        }
        boolean validated = true;
        if (waitForVerificationButton.getSelection()) {
            if (sendMailButton.getSelection()) {
                if (emailReciepientsText.getText().isEmpty()) {
                    setMessage("Provide at least one email address.", DialogPage.ERROR);
                    validated = false;
                } else {
                    for (String emailAddress : emailReciepientsText.getText()
                        .split(ToolIntegrationConstants.VERIFICATION_TOKEN_RECIPIENTS_SEPARATOR)) {
                        if (!emailAddress.replaceAll("\\s+", "").isEmpty() && !new EmailAddress(emailAddress.trim()).isValid()) {
                            setMessage("Email address not valid: " + emailAddress.trim(), DialogPage.ERROR);
                            validated = false;
                        }
                    }
                }
            }
            if (customTokenLocationButton.getSelection() && (customTokenLocationText.getText().trim().isEmpty()
                || !new File(customTokenLocationText.getText().trim()).isAbsolute())) {
                setMessage("Provide location for verification key (absolute path required).", DialogPage.ERROR);
                validated = false;
            }
        }

        if (validated) {
            setMessage(null, DialogPage.NONE);
            configurationMap.put(ComponentConstants.COMPONENT_CONFIG_KEY_REQUIRES_OUTPUT_APPROVAL,
                waitForVerificationButton.getSelection());
            if (customTokenLocationButton.getSelection()) {
                configurationMap.put(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_LOCATION, customTokenLocationText.getText().trim());
            } else {
                configurationMap.remove(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_LOCATION);
            }
            if (sendMailButton.getSelection()) {
                configurationMap.put(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_RECIPIENTS, emailReciepientsText.getText().trim());
            } else {
                configurationMap.remove(ToolIntegrationConstants.KEY_VERIFICATION_TOKEN_RECIPIENTS);
            }
        }
        setPageComplete(validated);
    }

    /**
     * Listener for the in-/output tables.
     * 
     * @author Sascha Zur
     */
    private final class TableSelectionListener extends SelectionAdapter {

        private final Table table;

        private final Button editButton;

        private final Button removeButton;

        private TableSelectionListener(Table table, Button editButton,
            Button removeButton) {
            this.table = table;
            this.editButton = editButton;
            this.removeButton = removeButton;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            updateButtonActivation(table, editButton, removeButton);
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
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

        private final String type;

        ButtonSelectionListener(Button button, Table table, String name) {
            this.button = button;
            this.selectionTable = table;
            type = name;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {

            TableItem[] selection = selectionTable.getSelection();
            if (button.getText().equals(Messages.add)) {
                // addInputOutput(selection, title, type);
                addInputOutput(type);
            } else if (button.getText().equals(Messages.edit)) {
                editInputOutput(selection, type);
            } else if (button.getText().equals(Messages.remove)) {
                removeInputOutput(selection, type);

            }
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
        validateVerificationTab = false;
        updatePageValues();
        validateVerificationTab = true;
    }

    private void updatePageValues() {
        updateTable(INPUTS);
        updateTable(OUTPUTS);
        updateVerificationTab();
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        helpSystem.displayHelp(HELP_CONTEXT_ID);
    }

    @SuppressWarnings("unchecked")
    private void addInputOutput(String type) {

        String title = type.substring(0, 1).toUpperCase() + type.substring(1, type.length() - 1);

        WizardEndpointEditDialog weed = new WizardEndpointEditDialog(null, Messages.add + " " + title, type, getAllEndpointNames(type));
        int exit = weed.open();
        if (exit == Dialog.OK) {
            if (((List<Map<String, String>>) configurationMap.get(type)) == null) {
                configurationMap.put(type, new LinkedList<Map<String, String>>());
            }
            ((List<Map<String, String>>) configurationMap.get(type)).add(weed.getConfig());
            updateTable(type);
            ((ToolIntegrationWizard) getWizard()).updateAllPages();
        }
    }

    @SuppressWarnings("unchecked")
    private void editInputOutput(TableItem[] selection,
        String type) {

        String title = type.substring(0, 1).toUpperCase()
            + type.substring(1, type.length() - 1);

        if (selection != null && selection.length > 0) {
            List<Map<String, String>> endpointList = (List<Map<String, String>>) configurationMap.get(type);
            Map<String, String> oldConfig = null;
            Map<String, String> oldConfigCopy = new HashMap<String, String>();
            for (Map<String, String> endpoint : endpointList) {
                if (endpoint.get(NAME) != null && endpoint.get(NAME).equals(selection[0].getText())) {
                    oldConfigCopy.putAll(endpoint);
                    oldConfig = endpoint;
                }
            }
            WizardEndpointEditDialog weed =
                new WizardEndpointEditDialog(null, Messages.edit + " " + title, type, oldConfigCopy, getAllEndpointNames(type));
            int exit = weed.open();
            if (exit == Dialog.OK) {
                ((List<Map<String, String>>) configurationMap.get(type)).remove(oldConfig);
                ((List<Map<String, String>>) configurationMap.get(type)).add(weed.getConfig());
                updateTable(type);
                ((ToolIntegrationWizard) getWizard()).updateAllPages();
            }
        }

    }

    @SuppressWarnings("unchecked")
    private void removeInputOutput(TableItem[] selection,
        String type) {

        if (selection != null && selection.length > 0) {
            List<Map<String, String>> endpointList = (List<Map<String, String>>) configurationMap.get(type);
            Map<String, String> endpointToRemove = null;
            for (Map<String, String> endpoint : endpointList) {
                if (endpoint.get(NAME) != null && endpoint.get(NAME).equals(selection[0].getText())) {
                    endpointToRemove = endpoint;
                }
            }
            endpointList.remove(endpointToRemove);
            updateTable(type);
            ((ToolIntegrationWizard) getWizard()).updateAllPages();
        }

    }

    @Override
    public void updatePage() {}

    @Override
    protected void onAddClicked() {

        if (activTable.getData().equals(INPUTS)) {
            addInputOutput(INPUTS);
        } else {
            addInputOutput(OUTPUTS);
        }

    }

    @Override
    protected void onEditClicked() {

        TableItem[] selection = activTable.getSelection();
        if (activTable.getData().equals(INPUTS)) {
            editInputOutput(selection, INPUTS);
        } else {
            editInputOutput(selection, OUTPUTS);
        }

    }

    @Override
    protected void onRemoveClicked() {

        TableItem[] selection = activTable.getSelection();
        if (activTable.getData().equals(INPUTS)) {
            removeInputOutput(selection, INPUTS);
        } else {
            removeInputOutput(selection, OUTPUTS);
        }

    }

}
