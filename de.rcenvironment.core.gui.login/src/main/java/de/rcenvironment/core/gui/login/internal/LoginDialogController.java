/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.login.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogSettings;

import de.rcenvironment.core.authentication.AuthenticationService;
import de.rcenvironment.core.login.LoginConfiguration;

/**
 * 
 * Controller of the {@link LoginDialog}.
 *
 * @author Doreen Seider
 */
public class LoginDialogController {

    /** Prefix for naming controls. */
    private static final String PROPERTIES_PREFIX = "de.rcenvironment.rce.gui.login"; //$NON-NLS-1$

    private static final Log LOGGER = LogFactory.getLog(LoginDialogController.class);

    /** The login dialog to control. */
    private LoginDialog loginDialog;
    
    /**
     * The authentication service used to load certificates and keys from file system and key
     * stores.
     */
    private AuthenticationService authenticationService;

    /**
     * The authentication service used to load certificates and keys from file system and key
     * stores.
     */
    private LoginConfiguration loginConfiguration;
    
    /**
     * The directory containing the currently selected certificate.
     */
    private String certificatePath;

    /**
     * The path to the currently selected key.
     */
    private String keyPath;
    
    /**
     * The username for the LDAP login.
     */
    private String usernameLDAP = "";
    
    /**
     * The name of the tab to set to the front at the next (Re-)Login.
     */
    private String tabTitle = Messages.ldapTabName;
    
    
    /**
     * Constructor.
     * 
     * @param loginDialog The login dialog to control.
     * @param authenticationService The authentication service.
     * @param loginConfiguration The settings to use.
     */
    protected LoginDialogController(LoginDialog loginDialog, AuthenticationService authenticationService, 
        LoginConfiguration loginConfiguration) {
        
        this.loginDialog = loginDialog;
        this.authenticationService = authenticationService;
        this.loginConfiguration = loginConfiguration;
    }
    
    /**
     * Set ups the dialog for the certificate login.
     * 
     * @param relogin true if re-login should be processed, else false.
     */
    protected void setUpDialogForLDAP(boolean relogin) {
        loadSettings();
        loadUsernameLDAP();
    }
    
    /** 
     * Saves settings.
     */
    protected void saveSettings() {
        IDialogSettings dialogSettings = Activator.getInstance().getDialogSettings();
        dialogSettings.put(PROPERTIES_PREFIX + LoginConfiguration.CERTIFICATE_FILE, certificatePath);
        dialogSettings.put(PROPERTIES_PREFIX + LoginConfiguration.KEY_FILE, keyPath);
        dialogSettings.put(PROPERTIES_PREFIX + LoginConfiguration.USERNAME_LDAP, loginDialog.getUsernameLDAP());
        dialogSettings.put(PROPERTIES_PREFIX + LoginConfiguration.TAB_TITLE, tabTitle);
        dialogSettings.put(PROPERTIES_PREFIX + LoginConfiguration.ANONYMOUSLOGIN, loginDialog.getAnonymousLogin());
    }
    
    /** 
     * Loads settings.
     */
    protected void loadSettings() {
        IDialogSettings dialogSettings = Activator.getInstance().getDialogSettings();

        String settingsCertificatePath = dialogSettings.get(PROPERTIES_PREFIX + LoginConfiguration.CERTIFICATE_FILE);
        if (settingsCertificatePath != null) {
            certificatePath = settingsCertificatePath;      
        } else {
            certificatePath = loginConfiguration.getCertificateFile();
        }

        String settingsKeyPath = dialogSettings.get(PROPERTIES_PREFIX + LoginConfiguration.KEY_FILE);
        if (settingsKeyPath != null) {
            keyPath = settingsKeyPath;
        } else {
            keyPath = loginConfiguration.getKeyFile();
        }
        
        String settingsUsernameLdap = dialogSettings.get(PROPERTIES_PREFIX + LoginConfiguration.USERNAME_LDAP);
        if (settingsUsernameLdap != null){
            usernameLDAP = settingsUsernameLdap;
        } else {
            usernameLDAP = loginConfiguration.getLdapUsername();
        }
        
        String settingsTabTitle = dialogSettings.get(PROPERTIES_PREFIX + LoginConfiguration.TAB_TITLE);
        if (settingsTabTitle != null){
            tabTitle = settingsTabTitle;
        } // else it is the default value set above
    }

    /**
     * 
     * Sets the entry of the usernameLDAP to username for the LDAP login. It might be "".
     *
     */
    private void loadUsernameLDAP(){
        loginDialog.setUsernameLDAP(usernameLDAP);
    }
    
    /**
     * Getter.
     * @return title of the tap last used 
     */
    public String getTabTitle(){
        return tabTitle;
    }
    
    /**
     * 
     * Setter.
     * 
     * @param title The new title of the selected tab.
     */
    public void setTabTitle(String title){
        this.tabTitle = title;
    }
    
}
