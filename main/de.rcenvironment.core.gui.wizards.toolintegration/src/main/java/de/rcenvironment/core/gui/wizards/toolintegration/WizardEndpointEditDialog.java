/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.utils.common.configuration.VariableNameVerifyListener;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A dialog for editing a single endpoint configuration.
 * 
 * @author Sascha Zur
 */
public class WizardEndpointEditDialog extends Dialog {

    /**
     * Map for getting from the gui display name of a DataType back to the class name.
     */
    public static Map<String, DataType> guiNameToDataType = new HashMap<String, DataType>();

    private static final String NO_VALUE_STRING = "";

    private static final String INPUT_FOLDER_STANDARD_NAME = "Input folder";

    private static List<EndpointDefinition.InputDatumHandling> inputDatumHandlings;

    private static List<EndpointDefinition.InputExecutionContraint> inputExecutionConstraints;
    
    private Text nameText;

    private Combo dataTypeCombo;

    private Button[] inputDatumHandlingButtons = new Button[3];
    
    private Button[] inputExecutionConstraintButtons = new Button[3];
    
    private Text filenameText;

    private final Map<String, String> config;

    private final String title;

    private final List<String> allEndpointNames;

    private final String oldName;

    private final String type;

    static {
        inputDatumHandlings = new ArrayList<>();
        inputDatumHandlings.add(EndpointDefinition.InputDatumHandling.Constant);
        inputDatumHandlings.add(EndpointDefinition.InputDatumHandling.Single);
        inputDatumHandlings.add(EndpointDefinition.InputDatumHandling.Queue);
        
        inputExecutionConstraints = new ArrayList<>();
        inputExecutionConstraints.add(EndpointDefinition.InputExecutionContraint.Required);
        inputExecutionConstraints.add(EndpointDefinition.InputExecutionContraint.RequiredIfConnected);
        inputExecutionConstraints.add(EndpointDefinition.InputExecutionContraint.NotRequired);
    }
    
    /**
     * Dialog for creating or editing an endpoint.
     * 
     * @param parentShell parent Shell
     * @param title
     * @param list
     * @param configuration the containing endpoint manager
     */
    public WizardEndpointEditDialog(Shell parentShell, String title, String type, List<String> allEndpointNames) {
        super(parentShell);
        config = new HashMap<String, String>();
        this.title = title;
        this.type = type;
        this.allEndpointNames = allEndpointNames;
        oldName = null;
        
    }

    public WizardEndpointEditDialog(Shell parentShell, String title, String type,
        Map<String, String> config, List<String> allEndpointNames) {
        super(parentShell);
        this.config = config;
        oldName = config.get(InOutputConfigurationPage.NAME);
        this.title = title;
        this.type = type;

        this.allEndpointNames = allEndpointNames;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(title);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));
        GridData g = new GridData(GridData.FILL_BOTH);
        g.grabExcessHorizontalSpace = true;
        g.horizontalAlignment = GridData.CENTER;
        container.setLayoutData(g);
        createEndpointSettings(container);
        updateInitValues();
        return container;
    }

    private void updateInitValues() {
        if (config.get(InOutputConfigurationPage.DATA_TYPE) != null) {
            dataTypeCombo.setText(DataType.valueOf(config.get(InOutputConfigurationPage.DATA_TYPE)).getDisplayName());
        }
        if (config.get(InOutputConfigurationPage.NAME) != null) {
            nameText.setText(config.get(InOutputConfigurationPage.NAME));
        }
        if (type.equals(InOutputConfigurationPage.INPUTS)) {
            if (config.containsKey(InOutputConfigurationPage.HANDLING)) {
                for (String handling : StringUtils.splitAndUnescape(config.get(InOutputConfigurationPage.HANDLING))) {
                    inputDatumHandlingButtons[inputDatumHandlings.indexOf(EndpointDefinition
                            .InputDatumHandling.valueOf(handling))].setSelection(true);
                }
            } else if (config.containsKey(InOutputConfigurationPage.USAGE)) {
                if (config.get(InOutputConfigurationPage.USAGE).equals("initial")) {
                    inputDatumHandlingButtons[inputDatumHandlings.indexOf(EndpointDefinition
                            .InputDatumHandling.Constant)].setSelection(true);
                } else {
                    inputDatumHandlingButtons[inputDatumHandlings.indexOf(EndpointDefinition
                            .InputDatumHandling.Single)].setSelection(true);
                }
            }
            if (config.containsKey(InOutputConfigurationPage.CONSTRAINT)) {
                for (String constraint : StringUtils.splitAndUnescape(config.get(InOutputConfigurationPage.CONSTRAINT))) {
                    inputExecutionConstraintButtons[inputExecutionConstraints.indexOf(EndpointDefinition
                            .InputExecutionContraint.valueOf(constraint))].setSelection(true);
                }
            } else if (config.containsKey(InOutputConfigurationPage.USAGE)) {
                if (config.get(InOutputConfigurationPage.USAGE).equals("optional")) {
                    inputExecutionConstraintButtons[inputExecutionConstraints.indexOf(EndpointDefinition
                            .InputExecutionContraint.NotRequired)].setSelection(true);
                } else {
                    inputExecutionConstraintButtons[inputExecutionConstraints.indexOf(EndpointDefinition
                            .InputExecutionContraint.Required)].setSelection(true);
                }
            }
        }
        if (config.get(InOutputConfigurationPage.FILENAME) != null && filenameText != null) {
            filenameText.setText(config.get(InOutputConfigurationPage.FILENAME));
        }
    }

    protected void createEndpointSettings(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, true));

        Composite propertyContainer = new Composite(container, SWT.None);
        propertyContainer.setLayout(new GridLayout(2, true));

        Label nameLabel = new Label(propertyContainer, SWT.NONE);
        nameLabel.setText(Messages.nameRequired);
        nameText = new Text(propertyContainer, SWT.BORDER);
        GridData textGridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        nameText.setLayoutData(textGridData);
        nameText.addListener(SWT.Verify, new VariableNameVerifyListener());

        List<DataType> supportedDataTypes = new LinkedList<DataType>();
        supportedDataTypes.add(DataType.ShortText);
        supportedDataTypes.add(DataType.Boolean);
        supportedDataTypes.add(DataType.Integer);
        supportedDataTypes.add(DataType.Float);
        supportedDataTypes.add(DataType.Vector);
        supportedDataTypes.add(DataType.FileReference);
        supportedDataTypes.add(DataType.DirectoryReference);
        new Label(propertyContainer, SWT.NONE).setText(Messages.dataTypeColon);
        dataTypeCombo = new Combo(propertyContainer, SWT.READ_ONLY);
        for (DataType t : DataType.values()) {
            if (supportedDataTypes.contains(t)) {
                dataTypeCombo.add(t.getDisplayName());
                guiNameToDataType.put(t.getDisplayName(), t);
            }
        }
        dataTypeCombo.select(0);
        GridData dataTypeData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        dataTypeCombo.setLayoutData(dataTypeData);

        if (type.equals(InOutputConfigurationPage.INPUTS)) {
            new Label(propertyContainer, SWT.NONE).setText(Messages.inputHandlingColon);
            inputDatumHandlingButtons[0] = new Button(propertyContainer, SWT.CHECK);
            inputDatumHandlingButtons[0].setText(inputDatumHandlings.get(0).getDisplayName());
            new Label(propertyContainer, SWT.NONE);
            inputDatumHandlingButtons[1] = new Button(propertyContainer, SWT.CHECK);
            inputDatumHandlingButtons[1].setText(inputDatumHandlings.get(1).getDisplayName());
            new Label(propertyContainer, SWT.NONE);
            inputDatumHandlingButtons[2] = new Button(propertyContainer, SWT.CHECK);
            inputDatumHandlingButtons[2].setText(inputDatumHandlings.get(2).getDisplayName());

            new Label(propertyContainer, SWT.NONE).setText(Messages.inputExecutionConstraintColon);
            inputExecutionConstraintButtons[0] = new Button(propertyContainer, SWT.CHECK);
            inputExecutionConstraintButtons[0].setText(inputExecutionConstraints.get(0).getDisplayName());
            new Label(propertyContainer, SWT.NONE);
            inputExecutionConstraintButtons[1] = new Button(propertyContainer, SWT.CHECK);
            inputExecutionConstraintButtons[1].setText(inputExecutionConstraints.get(1).getDisplayName());
            new Label(propertyContainer, SWT.NONE);
            inputExecutionConstraintButtons[2] = new Button(propertyContainer, SWT.CHECK);
            inputExecutionConstraintButtons[2].setText(inputExecutionConstraints.get(2).getDisplayName());
            
            Label fileNameLabel = new Label(propertyContainer, SWT.NONE);
            fileNameLabel.setText(Messages.filenameColon);
            filenameText = new Text(propertyContainer, SWT.BORDER);
            GridData fileNameData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
            filenameText.setLayoutData(fileNameData);
            filenameText.setEnabled(false);
            if (type.equals(InOutputConfigurationPage.INPUTS)) {
                filenameText.setMessage(Messages.emptyFilename);
            }
        }

    }

    private void saveAllConfig() {
        config.put(InOutputConfigurationPage.NAME, nameText.getText());
        if (type.equals(InOutputConfigurationPage.INPUTS)) {
            List<String> parts = new ArrayList<>();
            int i = 0;
            for (Button b : inputDatumHandlingButtons) {
                if (b.getSelection()) {
                    parts.add(inputDatumHandlings.get(i).name());                    
                }
                i++;
            }
            config.put(InOutputConfigurationPage.HANDLING, StringUtils.escapeAndConcat(parts));
            parts = new ArrayList<>();
            i = 0;
            for (Button b : inputExecutionConstraintButtons) {
                if (b.getSelection()) {
                    parts.add(inputExecutionConstraints.get(i).name());                    
                }
                i++;
            }
            config.put(InOutputConfigurationPage.CONSTRAINT, StringUtils.escapeAndConcat(parts));
        } else {
            config.put(InOutputConfigurationPage.HANDLING, "-");
            config.put(InOutputConfigurationPage.CONSTRAINT, "-");
        }
        config.put(InOutputConfigurationPage.DATA_TYPE, guiNameToDataType.get(dataTypeCombo.getText()).name());
        if (dataTypeCombo.getText().equals(DataType.FileReference.getDisplayName())
            || dataTypeCombo.getText().equals(DataType.DirectoryReference.getDisplayName())) {
            if (filenameText != null) {
                config.put(InOutputConfigurationPage.FILENAME, filenameText.getText());
            }
            if (type.equals(InOutputConfigurationPage.INPUTS)) {
                config.put(InOutputConfigurationPage.FOLDER, INPUT_FOLDER_STANDARD_NAME);
            }
        } else {
            config.put(InOutputConfigurationPage.FILENAME, NO_VALUE_STRING);
            config.put(InOutputConfigurationPage.FOLDER, NO_VALUE_STRING);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        super.create();
        // dialog title
        getShell().setText(title);
        validateInput();
        installModifyListeners();
    }

    private void installModifyListeners() {
        SelectionListener sl = new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                saveAllConfig();
                validateInput();
            }

        };
        dataTypeCombo.addSelectionListener(sl);
        if (type.equals(InOutputConfigurationPage.INPUTS)) {
            for (Button b : inputDatumHandlingButtons) {
                b.addSelectionListener(sl);
            }
            for (Button b : inputExecutionConstraintButtons) {
                b.addSelectionListener(sl);
            }
        }
        ModifyListener ml = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                saveAllConfig();
                validateInput();
            }
        };

        nameText.addModifyListener(ml);
        if (filenameText != null) {
            filenameText.addModifyListener(ml);
        }

    }

    protected void validateInput() {

        boolean isValid = true;
        if (dataTypeCombo.getText().equals(DataType.FileReference.getDisplayName())
            || dataTypeCombo.getText().equals(DataType.DirectoryReference.getDisplayName())) {
            if (filenameText != null) {
                filenameText.setEnabled(true);
            }
        } else {
            if (filenameText != null) {
                filenameText.setEnabled(false);
            }
        }
        if (nameText.getText() == null || nameText.getText().isEmpty()) {
            isValid = false;
        }
        // filter if name is already in use (but remind that on edit it can have the same name)
        if (allEndpointNames != null && allEndpointNames.contains(nameText.getText())) {
            if (oldName == null || !oldName.equals(nameText.getText())) {
                isValid = false;
            }
        }
        if (type.equals(InOutputConfigurationPage.INPUTS) && isValid) {
            boolean inputHandlingSelected = false;
            for (Button b : inputDatumHandlingButtons) {
                if (b.getSelection()) {
                    inputHandlingSelected = true;
                    break;
                }
            }
            isValid = inputHandlingSelected;
        }
        if (type.equals(InOutputConfigurationPage.INPUTS) && isValid) {
            boolean executionConstraintSelected = false;
            for (Button b : inputExecutionConstraintButtons) {
                if (b.getSelection()) {
                    executionConstraintSelected = true;
                    break;
                }
            }
            isValid = executionConstraintSelected;
        }
        getButton(IDialogConstants.OK_ID).setEnabled(isValid);
    }

    public Map<String, String> getConfig() {
        return config;
    }
}
