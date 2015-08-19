/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;

/**
 * @author Sascha Zur
 */
public class ScriptConfigurationPage extends ToolIntegrationWizardPage {

    /** Constant. */
    public static final int INPUT_COMBO = 0;

    /** Constant. */
    public static final int OUTPUT_COMBO = 1;

    /** Constant. */
    public static final int PROPERTY_COMBO = 2;

    /** Constant. */
    public static final int DIRECTORY_COMBO = 4;

    private static final int TEXTFIELD_HEIGHT = 270;

    private static final int TEXTFIELD_WIDTH = 300;

    private static final int NUMBER_OF_TABS = 3;

    protected Map<String, Object> configurationMap;

    private final Combo[] inputCombos;

    private final Combo[] outputCombos;

    private final Combo[] propertiesCombos;

    private final Combo[] directoryCombos;

    private final Text[] textFields;

    private Button winEnabledButton = null;

    private Button linuxEnabledButton = null;

    private boolean winScriptHasFocus = false;

    private Button noErrorOnOtherExitCodeButton;

    private Button setWorkingDirAsCwdButton;

    private Button setToolDirAsCwdButton;

    private Label executionPathLabel;

    protected ScriptConfigurationPage(String pageName, Map<String, Object> configurationMap) {
        super(pageName);
        setTitle(pageName);
        setDescription(Messages.scriptPageDescription);
        this.configurationMap = configurationMap;
        inputCombos = new Combo[NUMBER_OF_TABS];
        outputCombos = new Combo[NUMBER_OF_TABS];
        propertiesCombos = new Combo[NUMBER_OF_TABS];
        directoryCombos = new Combo[NUMBER_OF_TABS];
        textFields = new Text[NUMBER_OF_TABS + 1];

    }

    @Override
    public void createControl(Composite parent) {
        CTabFolder tabFolder = new CTabFolder(parent, SWT.BORDER);
        createScriptTabItem(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX, Messages.commandScriptMessage, tabFolder, 0);
        createScriptTabItem(ToolIntegrationConstants.KEY_PRE_SCRIPT, Messages.preScript, tabFolder, 1);
        createScriptTabItem(ToolIntegrationConstants.KEY_POST_SCRIPT, Messages.postScript, tabFolder, 2);

        tabFolder.setSelection(0);

        setControl(tabFolder);
        updatePageComplete();
    }

    /**
     * Updates all Combos when page is shown.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void updatePage() {
        for (int i = 0; i < NUMBER_OF_TABS; i++) {
            addAllEndpoints(inputCombos[i], ToolIntegrationConstants.KEY_ENDPOINT_INPUTS, i);
            setComboEnabled(inputCombos[i]);
            if (i > 0) {
                addAllEndpoints(outputCombos[i], ToolIntegrationConstants.KEY_ENDPOINT_OUTPUTS, i);
            }
            setComboEnabled(outputCombos[i]);
            propertiesCombos[i].removeAll();
            if (configurationMap.containsKey(ToolIntegrationConstants.KEY_PROPERTIES)) {
                Map<String, Object> properties = (Map<String, Object>) configurationMap.get(ToolIntegrationConstants.KEY_PROPERTIES);
                for (String propTabName : properties.keySet()) {
                    Map<String, Object> proptab = (Map<String, Object>) properties.get(propTabName);
                    for (String propkey : proptab.keySet()) {
                        if (proptab.get(propkey) instanceof Map<?, ?>) {
                            Map<String, String> property = (Map<String, String>) proptab.get(propkey);
                            propertiesCombos[i].add((property.get(ToolIntegrationConstants.KEY_PROPERTY_DISPLAYNAME)));
                        }
                    }
                }
            }
            if (propertiesCombos[i].getItemCount() > 0) {
                propertiesCombos[i].select(0);
            }
            setComboEnabled(propertiesCombos[i]);
            directoryCombos[i].select(0);
        }
        updateButtons();
        updatePageComplete();
    }

    private void setComboEnabled(Combo combo) {
        if (combo != null) {
            if (combo.getItemCount() == 0) {
                combo.setEnabled(false);
            } else {
                combo.setEnabled(true);
            }
        }
    }

    private void updateButtons() {

        boolean windowsEnabled = false;
        boolean linuxEnabled = false;
        if (configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED) != null) {
            windowsEnabled = (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED);
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED) != null) {
            linuxEnabled = (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED);
        }
        winEnabledButton.setSelection(windowsEnabled);
        linuxEnabledButton.setSelection(linuxEnabled);
        textFields[0].setEnabled(linuxEnabled);
        textFields[textFields.length - 1].setEnabled(windowsEnabled);

        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX) != null) {
            textFields[0].setText((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX));
        } else {
            textFields[0].setText("");
        }
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS) != null) {
            textFields[textFields.length - 1].setText((String) configurationMap
                .get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS));
        } else {
            textFields[textFields.length - 1].setText("");
        }
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_PRE_SCRIPT) != null) {
            textFields[1].setText((String) configurationMap.get(ToolIntegrationConstants.KEY_PRE_SCRIPT));
        } else {
            textFields[1].setText("");
        }
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_POST_SCRIPT) != null) {
            textFields[2].setText((String) configurationMap.get(ToolIntegrationConstants.KEY_POST_SCRIPT));
        } else {
            textFields[2].setText("");
        }

        if (configurationMap.get(ToolIntegrationConstants.DONT_CRASH_ON_NON_ZERO_EXIT_CODES) != null) {
            noErrorOnOtherExitCodeButton.setSelection((Boolean) configurationMap
                .get(ToolIntegrationConstants.DONT_CRASH_ON_NON_ZERO_EXIT_CODES));
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR) != null) {
            boolean toolDirIsCwd = (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR);
            setWorkingDirAsCwdButton.setSelection(!toolDirIsCwd);
            setToolDirAsCwdButton.setSelection(toolDirIsCwd);
        }

        setWorkingDirAsCwdButton.setEnabled(true);
        setToolDirAsCwdButton.setEnabled(true);
        executionPathLabel.setEnabled(true);
        for (String key : ((ToolIntegrationWizard) this.getWizard()).getCurrentContext().getDisabledIntegrationKeys()) {
            if (ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR.equals(key)) {
                setToolDirAsCwdButton.setEnabled(false);
                setWorkingDirAsCwdButton.setEnabled(false);
                executionPathLabel.setEnabled(false);
            }
        }
    }

    private void updatePageComplete() {
        boolean winEnabled = (configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED) != null
            && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED));
        boolean linuxEnabled =
            (configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED) != null && (Boolean) configurationMap
                .get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED));
        if (!(winEnabled || linuxEnabled)) {
            setPageComplete(false);
        } else {
            boolean winScriptNotEmpty = ((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS)) != null
                && !((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS)).isEmpty();
            boolean linuxScriptNotEmpty = ((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX)) != null
                && !((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX)).isEmpty();
            if ((winEnabled && winScriptNotEmpty) || (linuxEnabled && linuxScriptNotEmpty)) {
                setMessage(null, DialogPage.NONE);
                setPageComplete(true);
            } else {
                setMessage(Messages.toolExecutionCommandNeeded, DialogPage.ERROR);
                setPageComplete(false);
            }
        }
    }

    private Text createScriptTabItem(String propertyKey, String name, CTabFolder tabFolder, int buttonIndex) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText(name);
        Composite client = new Composite(tabFolder, SWT.NONE);
        if (buttonIndex == 0) {
            winEnabledButton = new Button(client, SWT.CHECK);
            winEnabledButton.setText(Messages.winCommandUse);
            linuxEnabledButton = new Button(client, SWT.CHECK);
            linuxEnabledButton.setText(Messages.linuxCommandUse);
            new Label(client, SWT.NONE);
        }

        final Text scriptAreaWin;
        if (buttonIndex == 0) {
            client.setLayout(new GridLayout(3, false));
            scriptAreaWin = new Text(client, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
            GridData scriptAreaWinData = new GridData(GridData.FILL_BOTH);
            scriptAreaWinData.widthHint = TEXTFIELD_WIDTH / 2;
            scriptAreaWinData.heightHint = TEXTFIELD_HEIGHT;
            scriptAreaWin.setLayoutData(scriptAreaWinData);
            scriptAreaWin.addModifyListener(new TextAreaModifyListener(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS));
            scriptAreaWin.setEnabled(false);
            scriptAreaWin.addFocusListener(new FocusListener() {

                @Override
                public void focusLost(FocusEvent arg0) {}

                @Override
                public void focusGained(FocusEvent arg0) {
                    winScriptHasFocus = true;
                }
            });
            textFields[textFields.length - 1] = scriptAreaWin;
        } else {
            scriptAreaWin = null;
            client.setLayout(new GridLayout(2, false));
        }
        item.setControl(client);
        final Text scriptArea = new Text(client, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData scriptAreaData = new GridData(GridData.FILL_BOTH);
        if (buttonIndex == 0) {
            scriptArea.setEnabled(false);
            scriptArea.addFocusListener(new FocusListener() {

                @Override
                public void focusLost(FocusEvent arg0) {}

                @Override
                public void focusGained(FocusEvent arg0) {
                    winScriptHasFocus = false;
                }
            });
            scriptAreaData.widthHint = TEXTFIELD_WIDTH / 2;
        } else {
            scriptAreaData.widthHint = TEXTFIELD_WIDTH;
        }
        scriptAreaData.heightHint = TEXTFIELD_HEIGHT;
        scriptArea.setLayoutData(scriptAreaData);
        scriptArea.addModifyListener(new TextAreaModifyListener(propertyKey));
        textFields[buttonIndex] = scriptArea;
        if (buttonIndex == 0) {
            addScriptSelectButtonListener();
        }

        createInsertFields(buttonIndex, client, scriptAreaWin, scriptArea);

        if (buttonIndex > 0) {
            Label jythonLabel = new Label(client, SWT.NONE);
            jythonLabel.setText(Messages.scriptLanguageHint);
        } else {
            new Label(client, SWT.NONE).setText(Messages.scriptLanguageHintBatch);
            new Label(client, SWT.NONE).setText(Messages.scriptLanguageHintBash);
            new Label(client, SWT.NONE).setText("");

            Group executionPropertiesGroup = new Group(client, SWT.NONE);
            executionPropertiesGroup.setText("Execution Options");
            executionPropertiesGroup.setLayout(new GridLayout(1, false));
            GridData executionPropertiesData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
            executionPropertiesData.horizontalSpan = 3;
            executionPropertiesGroup.setLayoutData(executionPropertiesData);

            noErrorOnOtherExitCodeButton = new Button(executionPropertiesGroup, SWT.CHECK);
            noErrorOnOtherExitCodeButton.setText(Messages.dontCrashOtherThanZeroLabel);

            noErrorOnOtherExitCodeButton.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    configurationMap.put(ToolIntegrationConstants.DONT_CRASH_ON_NON_ZERO_EXIT_CODES,
                        noErrorOnOtherExitCodeButton.getSelection());
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
            });
            executionPathLabel = new Label(executionPropertiesGroup, SWT.NONE);
            executionPathLabel.setText("Execute (command(s), pre/post script) from");
            setWorkingDirAsCwdButton = new Button(executionPropertiesGroup, SWT.RADIO);
            setWorkingDirAsCwdButton.setSelection(true);
            setWorkingDirAsCwdButton.setText("Working directory");
            setWorkingDirAsCwdButton.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    configurationMap.put(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR,
                        !setWorkingDirAsCwdButton.getSelection());
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
            });
            setToolDirAsCwdButton = new Button(executionPropertiesGroup, SWT.RADIO);
            setToolDirAsCwdButton.setText("Tool directory");
            setToolDirAsCwdButton.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    configurationMap.put(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR,
                        setToolDirAsCwdButton.getSelection());
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
            });
        }

        for (int i = 0; i < textFields.length; i++) {
            if (textFields[i] != null) {
                textFields[i].setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
            }
        }
        return scriptArea;
    }

    private void createInsertFields(int buttonIndex, Composite client, final Text scriptAreaWin, final Text scriptArea) {
        Composite buttonComposite = new Composite(client, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, false));
        GridData buttonCompositeData = new GridData();
        buttonCompositeData.verticalAlignment = GridData.BEGINNING;
        buttonComposite.setLayoutData(buttonCompositeData);
        GridData labelData = new GridData();
        labelData.horizontalSpan = 2;
        Label inputLabel = new Label(buttonComposite, SWT.NONE);
        inputLabel.setText(Messages.inputs);
        inputLabel.setLayoutData(labelData);
        Combo inputCombo = new Combo(buttonComposite, SWT.READ_ONLY);
        GridData inputComboData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        inputCombo.setLayoutData(inputComboData);
        Button inputInsertButton = new Button(buttonComposite, SWT.PUSH);
        inputInsertButton.setText(Messages.insertButtonLabel);
        if (buttonIndex == 0) {
            inputInsertButton.addSelectionListener(new InsertButtonListener(inputCombo, scriptArea, scriptAreaWin, INPUT_COMBO));
        } else {
            inputInsertButton.addSelectionListener(new InsertButtonListener(inputCombo, scriptArea, INPUT_COMBO));
        }
        inputCombos[buttonIndex] = inputCombo;
        if (buttonIndex > 0) {
            GridData labelDataOutput = new GridData();
            labelDataOutput.horizontalSpan = 2;
            Label outputLabel = new Label(buttonComposite, SWT.NONE);
            outputLabel.setText(Messages.outputs);
            outputLabel.setLayoutData(labelDataOutput);
            Combo outputCombo = new Combo(buttonComposite, SWT.READ_ONLY);
            GridData outputComboData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
            outputCombo.setLayoutData(outputComboData);
            Button outputInsertButton = new Button(buttonComposite, SWT.PUSH);
            outputInsertButton.setText(Messages.insertButtonLabel);
            if (buttonIndex == 0) {
                outputInsertButton.addSelectionListener(new InsertButtonListener(outputCombo, scriptArea, scriptAreaWin, OUTPUT_COMBO));
            } else {
                outputInsertButton.addSelectionListener(new InsertButtonListener(outputCombo, scriptArea, OUTPUT_COMBO));
            }
            outputCombos[buttonIndex] = outputCombo;
        }
        GridData labelDataProperties = new GridData();
        labelDataProperties.horizontalSpan = 2;
        Label propertiesLabel = new Label(buttonComposite, SWT.NONE);
        propertiesLabel.setText(Messages.properties);
        propertiesLabel.setLayoutData(labelDataProperties);
        Combo propertiesCombo = new Combo(buttonComposite, SWT.READ_ONLY);
        GridData propertiesComboData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        propertiesCombo.setLayoutData(propertiesComboData);
        Button propertyInsertButton = new Button(buttonComposite, SWT.PUSH);
        propertyInsertButton.setText(Messages.insertButtonLabel);
        if (buttonIndex == 0) {
            propertyInsertButton.addSelectionListener(new InsertButtonListener(propertiesCombo, scriptArea, scriptAreaWin, PROPERTY_COMBO));
        } else {
            propertyInsertButton.addSelectionListener(new InsertButtonListener(propertiesCombo, scriptArea, PROPERTY_COMBO));
        }
        propertiesCombos[buttonIndex] = propertiesCombo;
        GridData labelDataDirectories = new GridData();
        labelDataDirectories.horizontalSpan = 2;
        Label directoryLabel = new Label(buttonComposite, SWT.NONE);
        directoryLabel.setText(Messages.directory);
        directoryLabel.setLayoutData(labelDataDirectories);
        Combo directoriesCombo = new Combo(buttonComposite, SWT.READ_ONLY);
        GridData directoriesComboData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        directoriesCombo.setLayoutData(directoriesComboData);
        directoriesCombo.setItems(ToolIntegrationConstants.DIRECTORIES_PLACEHOLDERS_DISPLAYNAMES);
        Button directoryInsertButton = new Button(buttonComposite, SWT.PUSH);
        directoryInsertButton.setText(Messages.insertButtonLabel);
        if (scriptAreaWin != null) {
            directoryInsertButton.addSelectionListener(new InsertButtonListener(directoriesCombo, scriptArea, scriptAreaWin,
                DIRECTORY_COMBO));
        } else {
            directoryInsertButton.addSelectionListener(new InsertButtonListener(directoriesCombo, scriptArea, DIRECTORY_COMBO));
        }
        directoryCombos[buttonIndex] = directoriesCombo;

        if (buttonIndex == 2) {
            GridData labelDataAddProp = new GridData();
            labelDataAddProp.horizontalSpan = 2;
            Label addPropLabel = new Label(buttonComposite, SWT.NONE);
            addPropLabel.setText("Additional Properties");
            addPropLabel.setLayoutData(labelDataAddProp);
            Combo addPropCombo = new Combo(buttonComposite, SWT.READ_ONLY);
            GridData addPropComboData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
            addPropCombo.setLayoutData(addPropComboData);
            addPropCombo.add(Messages.exitCodeLabel);
            addPropCombo.select(0);
            Button addPropInsertButton = new Button(buttonComposite, SWT.PUSH);
            addPropInsertButton.setText(Messages.insertButtonLabel);
            addPropInsertButton.addSelectionListener(new InsertButtonListener(addPropCombo, scriptArea, 3));

        }
    }

    private void addScriptSelectButtonListener() {
        winEnabledButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                textFields[NUMBER_OF_TABS].setEnabled(winEnabledButton.getSelection());
                configurationMap.put(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED, winEnabledButton.getSelection());
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        linuxEnabledButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                textFields[0].setEnabled(linuxEnabledButton.getSelection());
                configurationMap.put(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED, linuxEnabledButton.getSelection());
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
    }

    private void addAllEndpoints(Combo endpointCombo, String key, int tabNumber) {
        endpointCombo.removeAll();
        @SuppressWarnings("unchecked") List<Map<String, String>> endpointList = (List<Map<String, String>>) configurationMap.get(key);
        if (endpointList != null) {
            for (Map<String, String> endpoint : endpointList) {
                if (!(tabNumber == 0 && endpoint.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.Vector.name()))) {
                    endpointCombo.add(endpoint.get(ToolIntegrationConstants.KEY_ENDPOINT_NAME));
                }
            }
            if (endpointCombo.getItemCount() > 0) {
                endpointCombo.select(0);
            }
        }
    }

    /**
     * Listener for the script text areas for saving the content to the correct key.
     * 
     * @author Sascha Zur
     */
    private class TextAreaModifyListener implements ModifyListener {

        private final String key;

        public TextAreaModifyListener(String key) {
            this.key = key;
        }

        @Override
        public void modifyText(ModifyEvent arg0) {
            configurationMap.put(key, ((Text) arg0.getSource()).getText());
            updatePageComplete();
        }
    }

    /**
     * Listener to insert the current selected text from the combo box to the given text.
     * 
     * @author Sascha Zur
     */
    private class InsertButtonListener implements SelectionListener {

        private static final String QUOTE = "\"";

        private final Combo combo;

        private final Text text;

        private final int comboType;

        private Text text2;

        public InsertButtonListener(Combo inputCombo, Text scriptArea, int comboType) {
            combo = inputCombo;
            text = scriptArea;
            this.comboType = comboType;
        }

        public InsertButtonListener(Combo inputCombo, Text scriptArea, Text scriptArea2, int comboType) {
            combo = inputCombo;
            text = scriptArea;
            text2 = scriptArea2;
            this.comboType = comboType;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);

        }

        @SuppressWarnings("unchecked")
        @Override
        public void widgetSelected(SelectionEvent arg0) {
            String insertText = combo.getText();
            Text currentText = text;
            if (winScriptHasFocus && text2 != null) {
                currentText = text2;
            }
            if (currentText.isEnabled()) {
                if (comboType == INPUT_COMBO && insertText != null && !insertText.isEmpty()) {
                    String possibleQuotes = "";
                    List<Map<String, Object>> endpointList =
                        (List<Map<String, Object>>) configurationMap.get(ToolIntegrationConstants.KEY_ENDPOINT_INPUTS);
                    Map<String, Object> endpoint = null;
                    for (Map<String, Object> ep : endpointList) {
                        if (ep.get(ToolIntegrationConstants.KEY_ENDPOINT_NAME).equals(insertText)) {
                            endpoint = ep;
                        }
                    }
                    if (endpoint != null) {
                        if (endpoint.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.ShortText.name())
                            || endpoint.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.FileReference.name())
                            || endpoint.get(ToolIntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.DirectoryReference.name())) {
                            possibleQuotes = QUOTE;
                        }
                    }
                    currentText.insert(possibleQuotes + ToolIntegrationConstants.PLACEHOLDER_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_INPUT_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR + insertText + ToolIntegrationConstants.PLACEHOLDER_SUFFIX
                        + possibleQuotes);
                }
                if (comboType == OUTPUT_COMBO && insertText != null && !insertText.isEmpty()) {
                    currentText.insert(ToolIntegrationConstants.PLACEHOLDER_PREFIX + ToolIntegrationConstants.PLACEHOLDER_OUTPUT_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR + insertText + ToolIntegrationConstants.PLACEHOLDER_SUFFIX);
                }
                if (comboType == PROPERTY_COMBO && insertText != null && !insertText.isEmpty()
                    && configurationMap.containsKey(ToolIntegrationConstants.KEY_PROPERTIES)) {
                    Map<String, Object> properties = (Map<String, Object>) configurationMap.get(ToolIntegrationConstants.KEY_PROPERTIES);
                    for (String propTabName : properties.keySet()) {
                        Map<String, Object> proptab = (Map<String, Object>) properties.get(propTabName);
                        for (String propkey : proptab.keySet()) {
                            if (proptab.get(propkey) instanceof Map<?, ?>) {
                                Map<String, String> property = (Map<String, String>) proptab.get(propkey);
                                if (property.get(ToolIntegrationConstants.KEY_PROPERTY_DISPLAYNAME).equals(insertText)) {
                                    currentText.insert(QUOTE + ToolIntegrationConstants.PLACEHOLDER_PREFIX
                                        + ToolIntegrationConstants.PLACEHOLDER_PROPERTY_PREFIX
                                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR
                                        + propkey + ToolIntegrationConstants.PLACEHOLDER_SUFFIX + QUOTE);
                                }
                            }
                        }
                    }
                }
                if (comboType == DIRECTORY_COMBO && insertText != null && !insertText.isEmpty()) {
                    currentText.insert(QUOTE + ToolIntegrationConstants.PLACEHOLDER_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_DIRECTORY_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR
                        + ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[combo.getSelectionIndex()]
                        + ToolIntegrationConstants.PLACEHOLDER_SUFFIX + QUOTE);
                }
                if (comboType == 3 && insertText != null && !insertText.isEmpty()) {
                    currentText.insert(ToolIntegrationConstants.PLACEHOLDER_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_ADDITIONAL_PROPERTIES_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR
                        + "exitCode"
                        + ToolIntegrationConstants.PLACEHOLDER_SUFFIX);
                }
                currentText.setFocus();
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
        updatePageValues();
    }

    private void updatePageValues() {
        updatePage();
        updatePageComplete();
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        helpSystem.displayHelp("de.rcenvironment.core.gui.wizard.toolintegration.integration_execution");
    }

}
