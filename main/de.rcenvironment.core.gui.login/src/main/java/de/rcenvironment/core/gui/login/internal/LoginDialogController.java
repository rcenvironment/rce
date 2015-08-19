/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.login.internal;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.authentication.AuthenticationService;
import de.rcenvironment.core.authentication.User.Type;
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
    private String tabTitle = Messages.certificateTabName;
    
    
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
    protected void setUpDialogForCertificate(boolean relogin) {
        loadSettings();

        addListenerToKeyPathButton();

        if (!relogin || loginDialog.getUser().getType() != Type.certificate) {
            addListenerToCertificatePathButton();
            tryToLoadDefaultCertificate();
        } else {
            loadCertificateFromFile();
        }
        tryToLoadDefaultKey();
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
     * Adds a listener.
     *
     */
    private void addListenerToCertificatePathButton() {
        
        Button certificateDirectoryButton = loginDialog.getCertificateFileButton();
        
        certificateDirectoryButton.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent event) {
                FileDialog fileDialog = new FileDialog(loginDialog.getShell(), SWT.OPEN);
                fileDialog.setText(Messages.chooseCert);
                fileDialog.setFilterPath(new File(certificatePath).getParent());

                Text myCertificateFileText = loginDialog.getCertificateFileText();
                
                final String newCertificatePath = fileDialog.open();
                if (newCertificatePath != null) {
                    try {
                        loginDialog.setCertificate(authenticationService.loadCertificate(newCertificatePath));
                        myCertificateFileText.setBackground(loginDialog.getShell().getBackground());
                        myCertificateFileText.setText(new File(newCertificatePath).getName());
                        
                        certificatePath = newCertificatePath;
                    } catch (AuthenticationException e) {
                        loginDialog.setCertificate(null);
                        myCertificateFileText.setBackground(loginDialog.getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
                        myCertificateFileText.setText(""); //$NON-NLS-1$
                        
                        MessageDialog.openError(loginDialog.getShell(), Messages.loginDialog,
                            Messages.certRevoked);
                    }
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });
    }
    
    /**
     * 
     * Sets the certificate in the loginDialog, if there was a certificate login before.
     *
     */
    private void loadCertificateFromFile(){
        
        try {
            loginDialog.setCertificate(authenticationService.loadCertificate(certificatePath));
        } catch (AuthenticationException e) {
            loginDialog.setCertificate(null);
        }
        
    }
    
    /**
     * Adds a listener.
     */
    private void addListenerToKeyPathButton() {
        
        Button keyFileButton = loginDialog.getkeyFileButton();
        
        keyFileButton.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent event) {
                FileDialog fileDialog = new FileDialog(loginDialog.getShell(), SWT.OPEN);
                fileDialog.setFilterPath(keyPath);
                fileDialog.setText(Messages.chooseKey);

                Text myKeyFileText = loginDialog.getKeyFileText();

                final String newKeyPath = fileDialog.open();
                if (newKeyPath != null) {
                    try {
                        loginDialog.setKey(authenticationService.loadKey(newKeyPath));
                        myKeyFileText.setBackground(loginDialog.getShell().getBackground());
                        myKeyFileText.setText(new File(newKeyPath).getName());
                        
                        keyPath = newKeyPath;
                    } catch (AuthenticationException e) {
                        loginDialog.setKey(null);
                        myKeyFileText.setBackground(loginDialog.getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
                        myKeyFileText.setText(""); //$NON-NLS-1$
                        
                        MessageDialog.openError(loginDialog.getShell(),
                                                Messages.loginDialog,
                                                Messages.keyRevoked);
                    }
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) { }
        });
    }

    /**
     * Tries to load default or previously used certificate and (if successful) set the GUI entry to it.
     */
    private void tryToLoadDefaultCertificate() {
        
        Text myCertificateFileText = loginDialog.getCertificateFileText();
        myCertificateFileText.setText(""); //$NON-NLS-1$
        myCertificateFileText.setBackground(loginDialog.getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
        loginDialog.setCertificate(null);
        
        if (new File(certificatePath).exists()) {
            try {
                loginDialog.setCertificate(authenticationService.loadCertificate(certificatePath));
                myCertificateFileText.setText(new File(certificatePath).getName());
                myCertificateFileText.setBackground(loginDialog.getShell().getBackground());
            } catch (AuthenticationException e) {
                LOGGER.error("Given certificate could not be loaded: " + certificatePath, e); //$NON-NLS-1$
            }
        }        
    }

    /**
     * Tries to load default or previously used private key and (if successful) set the GUI entry to it.
     */
    private void tryToLoadDefaultKey() {
        
        Text myKeyFileText = loginDialog.getKeyFileText();
        myKeyFileText.setText(""); //$NON-NLS-1$
        myKeyFileText.setBackground(loginDialog.getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
        loginDialog.setKey(null);
        
        if (new File(keyPath).exists()) {
            try {
                loginDialog.setKey(authenticationService.loadKey(keyPath));
                myKeyFileText.setText(new File(keyPath).getName());
                myKeyFileText.setBackground(loginDialog.getShell().getBackground());
            } catch (AuthenticationException e) {
                LOGGER.error("Given private key could not be loaded: " + keyPath, e); //$NON-NLS-1$
            }
        }
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
