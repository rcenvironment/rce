/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.cluster.configuration.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
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

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystemConstants;

/**
 * Dialog to create a new cluster connection configuration.
 * 
 * @author Doreen Seider
 */
public class CreateClusterConnectionConfigurationDialog extends TitleAreaDialog {

    protected static final int CREATE = 2;

    protected Combo queuingSystemCombo;
    
    protected Text qstatPathText;
    
    protected Text showqPathText;
    
    protected Text qdelPathText;
    
    protected Text hostText;

    protected Text portText;

    protected Text usernameText;

    protected Text passwordText;
    
    protected Button savePasswordCheckbox;
    
    protected Text configurationNameText;

    protected Button defaultConfigurationNameCheckbox;
    
    protected Button createButton;
    
    protected List<String> existingConfigurationNames;

    private ClusterQueuingSystem queuingSystem;
    
    private Map<String, String> pathsToClusterQueuingSystemCommands = new HashMap<>();
    
    private String host;

    private int port;

    private String username;

    private String password;
    
    private String configurationName;
    
    private boolean savePassword;

    private Label showqPathLabel;
    
    public CreateClusterConnectionConfigurationDialog(Shell parentShell, List<String> existingConfigurationNames) {
        super(parentShell);
        this.existingConfigurationNames = existingConfigurationNames;
    }
    
    @Override
    public void create() {
        super.create();
        setTitle(Messages.newConfigurationDialogTitle);
        setMessage(Messages.newConfigurationDialogMessage, IMessageProvider.INFORMATION);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite control = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(control, SWT.NONE);
        
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = false;
        container.setLayout(layout);
        
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        container.setLayoutData(gridData);
        
        Label queuingSystemLabel = new Label(container, SWT.NONE);
        queuingSystemLabel.setText(Messages.queueingSystemLabel);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        queuingSystemCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        queuingSystemCombo.setLayoutData(gridData);
        for (ClusterQueuingSystem system : ClusterQueuingSystem.values()) {
            queuingSystemCombo.add(system.name());            
        }
        queuingSystemCombo.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                showqPathText.setEnabled(queuingSystemCombo.getSelectionIndex() == 1);
                showqPathLabel.setEnabled(queuingSystemCombo.getSelectionIndex() == 1);
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        
        queuingSystemCombo.select(0);
        
        createQueuingSystemCommandPathsLabelsAndTexts(container);
        
        Label hostLabel = new Label(container, SWT.NONE);
        hostLabel.setText(Messages.hostLabel);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        hostText = new Text(container, SWT.BORDER);
        hostText.setLayoutData(gridData);

        Label portLabel = new Label(container, SWT.NONE);
        portLabel.setText(Messages.portLabel);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        portText = new Text(container, SWT.BORDER);
        portText.setLayoutData(gridData);
        portText.setText(String.valueOf(ClusterJobInformation.DEFAULT_SSH_PORT));
        
        Label usernameLabel = new Label(container, SWT.NONE);
        usernameLabel.setText(Messages.usernameLabel);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;

        usernameText = new Text(container, SWT.BORDER);
        usernameText.setLayoutData(gridData);

        Label passwordLabel = new Label(container, SWT.NONE);
        passwordLabel.setText(Messages.passwordLabel);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        passwordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(gridData);
        passwordText.setEnabled(false);

        // placeholder label
        new Label(container, SWT.NONE);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        
        savePasswordCheckbox = new Button(container, SWT.CHECK);
        savePasswordCheckbox.setText(Messages.savePasswordCheckboxLabel);
        savePasswordCheckbox.setLayoutData(gridData);
        savePasswordCheckbox.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                passwordText.setEnabled(savePasswordCheckbox.getSelection());
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        Label configurationNameLabel = new Label(container, SWT.NONE);
        configurationNameLabel.setText(Messages.configurationNameLabel);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        configurationNameText = new Text(container, SWT.BORDER);
        configurationNameText.setLayoutData(gridData);
        configurationNameText.setEnabled(false);
                
        // placeholder label
        new Label(container, SWT.NONE);

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        
        defaultConfigurationNameCheckbox = new Button(container, SWT.CHECK);
        defaultConfigurationNameCheckbox.setText(Messages.useDefaultNameCheckboxLabel);
        defaultConfigurationNameCheckbox.setLayoutData(gridData);
        defaultConfigurationNameCheckbox.setSelection(true);
        defaultConfigurationNameCheckbox.addSelectionListener(new SelectionListener() {
            
            @Override
            public void widgetSelected(SelectionEvent event) {
                configurationNameText.setEnabled(!defaultConfigurationNameCheckbox.getSelection());
            }
            
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        
        return container;
    }
    
    private void createQueuingSystemCommandPathsLabelsAndTexts(Composite container) {
        Label qstatPathLabel = new Label(container, SWT.NONE);
        qstatPathLabel.setText("Path 'qstat' (optional)");

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        qstatPathText = new Text(container, SWT.BORDER);
        qstatPathText.setLayoutData(gridData);
        
        Label qdelPathLabel = new Label(container, SWT.NONE);
        qdelPathLabel.setText("Path 'qdel' (optional)");

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        qdelPathText = new Text(container, SWT.BORDER);
        qdelPathText.setLayoutData(gridData);
        
        showqPathLabel = new Label(container, SWT.NONE);
        showqPathLabel.setText("Path 'showq' (optional)");

        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;

        showqPathText = new Text(container, SWT.BORDER);
        showqPathText.setLayoutData(gridData);
    }
    
    @Override
    protected Control createButtonBar(Composite parent) {
        final Composite buttonBar = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = false;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        buttonBar.setLayout(layout);

        final GridData data = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = false;
        buttonBar.setLayoutData(data);
        final Control buttonControl = super.createButtonBar(buttonBar);
        buttonControl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        return buttonBar;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton = createButton(parent, CREATE, Messages.createButtonTitle, true);
        createButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (isValidInput()) {
                    saveInput();
                    setReturnCode(CREATE);
                    close();
                }
            }
        });
        Button cancelButton = createButton(parent, CANCEL, Messages.cancel, false);
        cancelButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                setReturnCode(CANCEL);
                close();
            }
        });
    }
    
    private boolean isValidInput() {
        
        boolean valid = true;
        if (hostText.getText().length() == 0) {
            setErrorMessage(Messages.maintainHostLabel);
            valid = false;
        }
        if (portText.getText().length() == 0) {
            setErrorMessage(Messages.portLabel);
            valid = false;
        }
        try {
            Integer.valueOf(portText.getText());
        } catch (NumberFormatException e) {
            setErrorMessage(Messages.maintainPortNumberLabel);
            valid = false;
        }
        if (usernameText.getText().length() == 0) {
            setErrorMessage(Messages.maintainUsernameLabel);
            valid = false;
        }
        if (savePasswordCheckbox.getSelection()) {
            if (passwordText.getText().length() == 0) {
                setErrorMessage(Messages.maintainPasswordLabel);
                valid = false;
            }
        }
        
        valid = isConfigurationNameValid();
        
        return valid;
    }
    
    protected boolean isConfigurationNameValid() {
        boolean valid = true;
        if (!defaultConfigurationNameCheckbox.getSelection()) {
            if (configurationNameText.getText().length() == 0) {
                setErrorMessage(Messages.configurationNameLabel);
                valid = false;
            }
        } else if (existingConfigurationNames.contains(usernameText.getText() + "@" + hostText.getText())) {
            setErrorMessage(Messages.maintainAnotherConfigurationNameLabel);
            valid = false;            
            
        }
        if (existingConfigurationNames.contains(configurationNameText.getText())) {
            setErrorMessage(Messages.maintainAnotherConfigurationNameLabel);
            valid = false;            
        }
        return valid;
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }

    private void saveInput() {
        queuingSystem = ClusterQueuingSystem.valueOf(queuingSystemCombo.getItem(queuingSystemCombo.getSelectionIndex()));
        
        saveQueuingSystemCommandPath(ClusterQueuingSystemConstants.COMMAND_QSTAT, qstatPathText);
        saveQueuingSystemCommandPath(ClusterQueuingSystemConstants.COMMAND_QDEL, qdelPathText);
        saveQueuingSystemCommandPath(ClusterQueuingSystemConstants.COMMAND_SHOWQ, showqPathText);
        
        host = hostText.getText();
        port = Integer.valueOf(portText.getText());
        username = usernameText.getText();
        if (savePasswordCheckbox.getSelection()) {
            password = passwordText.getText();
        }
        if (defaultConfigurationNameCheckbox.getSelection()) {
            configurationName = username + "@" + host;
        } else {
            configurationName = configurationNameText.getText();            
        }
    }
    
    private void saveQueuingSystemCommandPath(String command, Text commandPathText) {
        String commandPath = commandPathText.getText();
        if (!commandPath.isEmpty()) {
            if (!commandPath.endsWith("/")) {
                commandPath = commandPath + "/";
            }
            pathsToClusterQueuingSystemCommands.put(command, commandPath);
        }
    }

    public ClusterQueuingSystem getClusterQueuingSystem() {
        return queuingSystem;
    }
    
    public Map<String, String> getPathsToClusterQueuingSystemCommands() {
        return Collections.unmodifiableMap(pathsToClusterQueuingSystemCommands);
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
    public String getConfigurationName() {
        return configurationName;
    }
    
    /**
     * @return <code>true</code> if password should be saved, otherwise <code>false</code>
     */
    public boolean savePassword() {
        return savePassword;
    }

}
