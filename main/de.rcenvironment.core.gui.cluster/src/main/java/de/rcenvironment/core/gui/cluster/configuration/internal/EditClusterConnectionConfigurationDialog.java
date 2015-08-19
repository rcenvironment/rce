/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.cluster.configuration.internal;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystemConstants;


/**
 * Dialog to edit an existing cluster connection configuration.
 * 
 * @author Doreen Seider
 */
public class EditClusterConnectionConfigurationDialog extends CreateClusterConnectionConfigurationDialog {

    private ClusterConnectionConfiguration configuration;
    
    public EditClusterConnectionConfigurationDialog(Shell parentShell, List<String> existingConfigurationNames,
        ClusterConnectionConfiguration configuration) {
        super(parentShell, existingConfigurationNames);
        this.configuration = configuration;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Control control = super.createDialogArea(parent);
        prefillForm();
        return control;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        createButton.setText(Messages.editButtonTitle2);
    }
    
    @Override
    protected boolean isConfigurationNameValid() {
        boolean valid = true;
        if (!defaultConfigurationNameCheckbox.getSelection()) {
            if (configurationNameText.getText().length() == 0) {
                setErrorMessage(Messages.maintainConfigurationNameLabel);
                valid = false;
            }
        } else if (existingConfigurationNames.contains(usernameText.getText() + "@" + hostText.getText())) {
            setErrorMessage(Messages.maintainAnotherConfigurationNameLabel);
            valid = false;            
            
        }
        if (existingConfigurationNames.contains(configurationNameText.getText())
            && !configuration.getConfigurationName().equals(configurationNameText.getText())) {
            setErrorMessage(Messages.maintainConfigurationNameLabel);
            valid = false;
        }
        return valid;
    }
    
    private void prefillForm() {
        if (configuration.getClusterQueuingSystem() == ClusterQueuingSystem.TORQUE) {
            queuingSystemCombo.select(1);
        }

        Map<String, String> paths = configuration.getPathToClusterQueuingSystemCommands();
        prefillCommandPath(ClusterQueuingSystemConstants.COMMAND_QSTAT, qstatPathText, paths);
        prefillCommandPath(ClusterQueuingSystemConstants.COMMAND_QDEL, qdelPathText, paths);
        prefillCommandPath(ClusterQueuingSystemConstants.COMMAND_SHOWQ, showqPathText, paths);
        
        hostText.setText(configuration.getHost());
        portText.setText(String.valueOf(configuration.getPort()));
        usernameText.setText(configuration.getUsername());
        if (configuration.getPassword() != null) {
            passwordText.setText(configuration.getPassword());
            passwordText.setEnabled(true);
            savePasswordCheckbox.setSelection(true);
        }
        configurationNameText.setText(configuration.getConfigurationName());
        configurationNameText.setEnabled(true);
        defaultConfigurationNameCheckbox.setSelection(false);
    }
    
    private void prefillCommandPath(String command, Text pathText, Map<String, String> paths) {
        if (paths.containsKey(command)) {
            pathText.setText(paths.get(command));            
        }
    }
}
