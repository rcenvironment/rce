/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.configuration.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import de.rcenvironment.core.configuration.SecurePreferencesFactory;
import de.rcenvironment.core.gui.cluster.internal.ErrorMessageDialogFactory;
import de.rcenvironment.core.gui.cluster.view.internal.ClusterConnectionInformation;
import de.rcenvironment.core.utils.cluster.ClusterService;
import de.rcenvironment.core.utils.cluster.ClusterServiceManager;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;


/**
 * Controller of cluster connection configuration related dialogs.
 *
 * @author Doreen Seider
 */
public class ClusterConnectionConfigurationDialogsController {

    protected static final String SETTINGS_KEY_CONFIGURATIONS = "de.rcenvironment.core.gui.cluster.connectionconfigurations";
    
    protected static final String SETTINGS_KEY_PASSWORD_STORED = "de.rcenvironment.core.gui.cluster.connectionconfigurations.password";
    
    private static final String READING_STORED_CONFIGURATION_FAILED_WILL_BE_RESET = "Reading stored configurations failed. Will be reset";

    private static final String STORING_CONFIGURATION_FAILED = "Storing configuration failed";
    
    private static final Log LOGGER = LogFactory.getLog(ClusterConnectionConfigurationDialogsController.class);

    protected final IDialogSettings dialogSettings;
    
    private Composite parent;
    
    private ClusterService jobInformationService;
    
    private String clusterConfigurationName;

    private ServiceRegistryAccess serviceRegistryAccess;
    
    public ClusterConnectionConfigurationDialogsController(Composite parent) {
        this.parent = parent;
        dialogSettings = Activator.getInstance().getDialogSettings();
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
    }
    
    /**
     * Opens dialog to manage configuration and select one to connect.
     * 
     * @return <code>true</code> if connection to a cluster was connecter, else <code>false</code>
     */
    public ClusterConnectionInformation openClusterConnectionSelectionDialog() {
        ClusterConnectionInformation connectionInformation = null;
        ClusterConnectionConfigurationSelectionDialog dialog = new ClusterConnectionConfigurationSelectionDialog(parent.getShell(), this);
        dialog.create();
        ClusterConnectionConfiguration configuration;
        switch(dialog.open()) {
        case ClusterConnectionConfigurationSelectionDialog.CONNECT:
            configuration = dialog.getSelectedElement();
            connectionInformation = connectToCluster(configuration);
            break;
        case Window.CANCEL:
            break;
        default:
            break;
        }
        
        return connectionInformation;
    }
    
    public ClusterService getClusterJobInformationService() {
        return jobInformationService;
    }
    
    public String getClusterConfigurationName() {
        return clusterConfigurationName;
    }
    
    protected void openNewClusterConnectionConfigurationDialog() {
        CreateClusterConnectionConfigurationDialog dialog = new CreateClusterConnectionConfigurationDialog(parent.getShell(),
            getClusterConnectionConfigurationNames(getStoredClusterConnectionConfigurations()));
        dialog.create();
        switch(dialog.open()) {
        case CreateClusterConnectionConfigurationDialog.CREATE:
            createNewClusterConnectionConfiguration(dialog);
        default: 
            break;
        }
    }
    
    protected void openEditClusterConnectionConfigurationDialog(ClusterConnectionConfiguration configuration) {
        EditClusterConnectionConfigurationDialog dialog = new EditClusterConnectionConfigurationDialog(parent.getShell(),
            getClusterConnectionConfigurationNames(getStoredClusterConnectionConfigurations()), configuration);
        dialog.create();
        switch(dialog.open()) {
        case CreateClusterConnectionConfigurationDialog.CREATE:
            editClusterConnectionConfiguration(dialog, configuration);
        default: 
            break;
        }
    }
    
    protected void openDeleteConfirmationDialog(ClusterConnectionConfiguration configuration) {
        MessageDialog dialog = new MessageDialog(parent.getShell(), Messages.deleteConfirmDialogTitle, null,
                StringUtils.format(Messages.deleteConfirmDialogQuestion, configuration), MessageDialog.QUESTION, new String[] {
                    Messages.yes, Messages.no }, 0);
        switch (dialog.open()) {
        case 0:
            deleteClusterConnectionConfiguration(configuration);
        default:
            break;
        }
        
    }
    
    private String openPasswordInputDialog() {
        String password = null;
        InputDialog dialog = new InputDialog(parent.getShell(), Messages.passwordDialogTitle, Messages.passwordDialogMessage, null, null) {
            @Override
            protected int getInputTextStyle() {
                return SWT.PASSWORD;
            }
        };
        switch (dialog.open()) {
        case Window.OK:
            password = dialog.getValue();
            break;
        case Window.CANCEL:
            openClusterConnectionSelectionDialog();
            break;
        default:
            break;
        }
        return password;
        
    }
    
    private ClusterConnectionInformation connectToCluster(ClusterConnectionConfiguration configuration) {
        ClusterConnectionInformation connectionInformation = null;
        if (configuration.getPassword() == null || configuration.getPassword().isEmpty()) {
            configuration.setPassword(openPasswordInputDialog());
        }
        ClusterServiceManager clusterServiceManager = serviceRegistryAccess.getService(ClusterServiceManager.class);
        jobInformationService = clusterServiceManager.retrieveSshBasedClusterService(
            configuration.getClusterQueuingSystem(), configuration.getPathToClusterQueuingSystemCommands(),
            configuration.getHost(), configuration.getPort(), configuration.getUsername(), configuration.getPassword());
        clusterConfigurationName = configuration.getConfigurationName();
        connectionInformation = new ClusterConnectionInformation(configuration, new Date());
        return connectionInformation;
    }
    
    private void createNewClusterConnectionConfiguration(CreateClusterConnectionConfigurationDialog dialog) {
        ClusterConnectionConfiguration newConfiguration = new ClusterConnectionConfiguration(dialog.getClusterQueuingSystem(),
            dialog.getPathsToClusterQueuingSystemCommands(), dialog.getHost(), dialog.getPort(), dialog.getUsername(),
            dialog.getConfigurationName(), dialog.getPassword());
        ClusterConnectionConfiguration[] configurations = getStoredClusterConnectionConfigurations();
        ClusterConnectionConfiguration[] newConfigurations = new ClusterConnectionConfiguration[configurations.length + 1];
        System.arraycopy(configurations, 0, newConfigurations, 0, configurations.length);
        newConfigurations[configurations.length] = newConfiguration;
        
        storeClusterConnectionConfigurations(newConfigurations, dialog.getPassword() != null && !dialog.getPassword().isEmpty());

    }
        
    private void editClusterConnectionConfiguration(EditClusterConnectionConfigurationDialog dialog,
        ClusterConnectionConfiguration oldConfiguration) {
        
        ClusterConnectionConfiguration newConfiguration = new ClusterConnectionConfiguration(dialog.getClusterQueuingSystem(),
            dialog.getPathsToClusterQueuingSystemCommands(), dialog.getHost(), dialog.getPort(), dialog.getUsername(),
            dialog.getConfigurationName(), dialog.getPassword());
        
        ClusterConnectionConfiguration[] configurations = getStoredClusterConnectionConfigurations();
        for (int i = 0; i < configurations.length; i++) {
            if (configurations[i].getConfigurationName().equals(oldConfiguration.getConfigurationName())) {
                configurations[i] = newConfiguration;
                break;
            }
        }
        
        storeClusterConnectionConfigurations(configurations, dialog.getPassword() != null && !dialog.getPassword().isEmpty());
    }
    
    private void deleteClusterConnectionConfiguration(ClusterConnectionConfiguration configuration) {
        ClusterConnectionConfiguration[] configurations = getStoredClusterConnectionConfigurations();
        ClusterConnectionConfiguration[] newConfigurations = new ClusterConnectionConfiguration[configurations.length - 1];
        int j = 0;
        for (int i = 0; i < configurations.length; i++) {
            if (!configurations[i].getConfigurationName().equals(configuration.getConfigurationName())) {
                newConfigurations[j++] = configurations[i];
            }
        }
        storeClusterConnectionConfigurations(newConfigurations, configuration.getPassword() != null
            && !configuration.getPassword().isEmpty());
    }
    
    private void storeClusterConnectionConfigurations(ClusterConnectionConfiguration[] configurations, boolean wasPasswordSet) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            PlainClusterConnectionConfiguration[] plainConfigurations = new PlainClusterConnectionConfigurationImpl[configurations.length];
            for (int i = 0; i < plainConfigurations.length; i++) {
                plainConfigurations[i] = configurations[i].getPlainClusterConnectionConfiguration();
            }
            dialogSettings.put(ClusterConnectionConfigurationDialogsController.SETTINGS_KEY_CONFIGURATIONS,
                mapper.writeValueAsString(plainConfigurations));

            if (wasPasswordSet) {
                dialogSettings.put(ClusterConnectionConfigurationDialogsController.SETTINGS_KEY_PASSWORD_STORED, true);

                SensitiveClusterConnectionConfiguration[] sensitiveConfigurations = new SensitiveClusterConnectionConfigurationImpl
                    [configurations.length];
                for (int i = 0; i < sensitiveConfigurations.length; i++) {
                    sensitiveConfigurations[i] = configurations[i].getSensitiveClusterConnectionConfiguration();
                }
                ISecurePreferences prefs = SecurePreferencesFactory.getSecurePreferencesStore();
                prefs.put(ClusterConnectionConfigurationDialogsController.SETTINGS_KEY_CONFIGURATIONS,
                    mapper.writeValueAsString(sensitiveConfigurations), true);
            }
        } catch (JsonParseException e) {
            ErrorMessageDialogFactory.createMessageDialogForStoringConfigurationFailure(parent);
            LOGGER.error(STORING_CONFIGURATION_FAILED, e);
        } catch (JsonMappingException e) {
            ErrorMessageDialogFactory.createMessageDialogForStoringConfigurationFailure(parent);
            LOGGER.error(STORING_CONFIGURATION_FAILED, e);
        } catch (IOException e) {
            ErrorMessageDialogFactory.createMessageDialogForStoringConfigurationFailure(parent);
            LOGGER.error(STORING_CONFIGURATION_FAILED, e);
        } catch (StorageException e) {
            ErrorMessageDialogFactory.createMessageDialogForStoringConfigurationFailure(parent);
            LOGGER.error(STORING_CONFIGURATION_FAILED, e);
        }
    }

    protected ClusterConnectionConfiguration[] getStoredClusterConnectionConfigurations() {
        ObjectMapper mapper = new ObjectMapper();
       
        
        ClusterConnectionConfiguration[] configurations = new ClusterConnectionConfiguration[0];

        PlainClusterConnectionConfiguration[] plainConfigurations = new ClusterConnectionConfiguration[0];
        
        SensitiveClusterConnectionConfiguration[] sensitiveConfigurations = new ClusterConnectionConfiguration[0];

        try {
            if (dialogSettings.get(ClusterConnectionConfigurationDialogsController.SETTINGS_KEY_CONFIGURATIONS) != null
                && !dialogSettings.get(ClusterConnectionConfigurationDialogsController.SETTINGS_KEY_CONFIGURATIONS).isEmpty()) {
                String rawConfigurations = dialogSettings.get(ClusterConnectionConfigurationDialogsController.SETTINGS_KEY_CONFIGURATIONS);
                plainConfigurations = mapper.readValue(rawConfigurations, PlainClusterConnectionConfiguration[].class);
            }
            
            if (dialogSettings.getBoolean(SETTINGS_KEY_PASSWORD_STORED)) {
                ISecurePreferences prefs = SecurePreferencesFactory.getSecurePreferencesStore();
                String configurationJsonString = prefs.get(ClusterConnectionConfigurationDialogsController.SETTINGS_KEY_CONFIGURATIONS, "");
                if (!configurationJsonString.isEmpty()) {
                    sensitiveConfigurations =  mapper.readValue(configurationJsonString, SensitiveClusterConnectionConfiguration[].class);
                }
            }
            
            configurations = new ClusterConnectionConfiguration[plainConfigurations.length];
            
            for (int i = 0; i < configurations.length; i++) {
                configurations[i] = new ClusterConnectionConfiguration(plainConfigurations[i].getClusterQueuingSystem(),
                    plainConfigurations[i].getPathToClusterQueuingSystemCommands(),
                    plainConfigurations[i].getHost(), plainConfigurations[i].getPort(), plainConfigurations[i].getUsername(),
                    plainConfigurations[i].getConfigurationName());
                for (SensitiveClusterConnectionConfiguration sensitiveConfig : sensitiveConfigurations) {
                    if (sensitiveConfig.getKey().equals(configurations[i].getKey())) {
                        configurations[i].setPassword(sensitiveConfig.getPassword());
                        break;
                    }
                }
            }
            
        } catch (JsonParseException e) {
            ErrorMessageDialogFactory.createMessageDialogForReadingConfigurationsFailure(parent);
            storeClusterConnectionConfigurations(configurations, true);
            LOGGER.error(READING_STORED_CONFIGURATION_FAILED_WILL_BE_RESET, e);
        } catch (JsonMappingException e) {
            ErrorMessageDialogFactory.createMessageDialogForReadingConfigurationsFailure(parent);
            storeClusterConnectionConfigurations(configurations, true);
            LOGGER.error(READING_STORED_CONFIGURATION_FAILED_WILL_BE_RESET, e);
        } catch (IOException e) {
            ErrorMessageDialogFactory.createMessageDialogForReadingConfigurationsFailure(parent);
            storeClusterConnectionConfigurations(configurations, true);
            LOGGER.error(READING_STORED_CONFIGURATION_FAILED_WILL_BE_RESET, e);
        } catch (StorageException e) {
            ErrorMessageDialogFactory.createMessageDialogForReadingConfigurationsFailure(parent);
            storeClusterConnectionConfigurations(configurations, true);
            LOGGER.error(READING_STORED_CONFIGURATION_FAILED_WILL_BE_RESET, e);
        }
        
        return configurations;
    }
    
    private List<String> getClusterConnectionConfigurationNames(ClusterConnectionConfiguration[] configurations) {
        List<String> configurationNames = new ArrayList<String>();
        for (ClusterConnectionConfiguration configuration : configurations) {
            configurationNames.add(configuration.getConfigurationName());
        }
        return configurationNames;
    }
}
