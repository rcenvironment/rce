/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.login.internal;

import java.text.DateFormat;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.authentication.AuthenticationService;
import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authentication.User.Type;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.login.LoginConfiguration;
import de.rcenvironment.core.login.LoginInput;

/**
 * Dialog for user login.
 * 
 * @author Jens Muuss
 * @author Bea Hornef
 * @author Doreen Seider
 * @author Alice Zorn
 */
public class LoginDialog extends Dialog {

    /**
     * The controller of this view.
     */
    private LoginDialogController loginDialogController;

    /**
     * Text field for the password.
     */
    private String password;

    /**
     * Text box for certificates.
     */
    private Text certificatePathText;

    /**
     * The button to choose the certificate.
     */
    private Button certificatePathButton;

    /**
     * The text the key file is written in.
     */
    private Text keyPathText;

    /**
     * The button to choose the key.
     */
    private Button keyPathButton;

    /**
     * Text field for the password.
     */
    private Text passwordTextCertificate;

    /**
     * Text field for the password.
     */
    private Text passwordTextLDAP;

    /**
     * Text field for the username for the LDAP login.
     */
    private Text usernameLdapText;

    /**
     * Small image of the dialog.
     */
    private Image iconImage;

    /**
     * The tabFolder for the organization of the tabs.
     */
    private TabFolder tabFolder;

    /**
     * The title of the currently selected tab.
     */
    private String currentlySelectedTabTitle;

    /**
     * When dialog used for login, NO proxy certificate exists. When dialog used for session
     * information, the proxy certificate exists.
     */
    private User user = null;

    /**
     *  The username with which the user tries to log in.
     */
    private String usernameLdap;

    /**
     * true, if there has been a login before. 
     */
    private boolean relogin;

    private Button anonymous;

    private boolean anonymousLogin;  

    /**
     * Initializes a new login dialog. The session must contain a loaded certificate. For Login.
     * 
     * @param authenticationService The authentication settings service.
     * @param settings The settings of the login bundle.
     * 
     */
    public LoginDialog(AuthenticationService authenticationService, LoginConfiguration settings) {
        super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
        loginDialogController = new LoginDialogController(this, authenticationService, settings);
        relogin = false;
    }

    /**
     * Initializes a dialog for the session informations. For ReLogin via certificate.
     * 
     * @param user The valid user of the given session.
     * @param authenticationService The authentication settings service.
     * @param settings The settings of the login bundle.
     */
    public LoginDialog(User user, AuthenticationService authenticationService, LoginConfiguration settings) {
        super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
        this.user = user;
        loginDialogController = new LoginDialogController(this, authenticationService, settings);
        relogin = true;
    }

    /** 
     * Initializes a dialog for the session informations. For ReLogin via LDAP.
     * 
     * @param username
     * @param authenticationService The authentication settings service.
     * @param settings The settings of the login bundle.
     */
    public LoginDialog(User user, String username, AuthenticationService authenticationService, LoginConfiguration settings) {
        super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
        loginDialogController = new LoginDialogController(this, authenticationService, settings);
        usernameLdap = username;
        this.user = user;
        relogin = true;
    }

    /**
     * 
     * Setter.
     * 
     * @param usernameLDAP The username for the LDAP login to set.
     */
    public void setUsernameLDAP(String usernameLDAP){
        usernameLdap = usernameLDAP;
    }

    /**
     * 
     * Set the currently selected tab to the new tabTitle and set that tab selected.
     * 
     * @param title The title of the tab to be selected
     */
    private void setCurrentlySelectedTab(){
        int index;
        for (index = 0; index < tabFolder.getItemCount(); index++){
            if (currentlySelectedTabTitle.equals(tabFolder.getItem(index).getText())){
                break;
            }
        }
        tabFolder.setSelection(tabFolder.getItem(index));
    }

    /**
     * Getter.
     * 
     * @return The login input.
     */
    public LoginInput getLoginInput() {
        // compares the title of the currently active tab with the name of the tabs
        if (currentlySelectedTabTitle.equals(Messages.ldapTabName)) {
            if (anonymousLogin){
                return new LoginInput(anonymousLogin);
            } else {
                return new LoginInput(usernameLdap, password);
            }
        } else {
            throw new AssertionError();
        }
    }

    public boolean getAnonymousLogin(){
        return anonymousLogin;
    }

    /**
     * Getter.
     * 
     * @return the username for the LDAP login
     */
    public String getUsernameLDAP() {
        return usernameLdap;
    }

    /**
     * 
     * Getter.
     * 
     * @return the user of the login.
     */
    public User getUser(){
        return user;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        iconImage = ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_16);
        newShell.setImage(iconImage);
        if (!relogin) {
            newShell.setText(Messages.loginDialog);
        } else {
            newShell.setText(Messages.reLoginDialog);
        }
        newShell.setActive();
    }

    // create tabFolder and call methods which fill the tabs
    @Override
    protected Control createDialogArea(final Composite parent) {

        tabFolder = new TabFolder(parent, SWT.NONE);

        TabItem idLoginTab = new TabItem(tabFolder, SWT.NONE);
        idLoginTab.setText(Messages.ldapTabName);
        idLoginTab.setControl(createLDAPComposite(tabFolder));

        tabFolder.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                currentlySelectedTabTitle = tabFolder.getItem(tabFolder.getSelectionIndex()).getText();
                loginDialogController.setTabTitle(currentlySelectedTabTitle);
            }
        });
        currentlySelectedTabTitle = loginDialogController.getTabTitle();
        setCurrentlySelectedTab();

        return tabFolder;
    }

    private Composite createLDAPComposite(final Composite parent){
        final Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(3, false));
        GridData data = new GridData(GridData.FILL, GridData.CENTER, true, false);
        composite.setLayoutData(data);

        loginDialogController.setUpDialogForLDAP(relogin);
        createUsernameLdapArea(composite);

        if (user != null && user.getType() == Type.ldap){
            final Label expireLabel = new Label(composite, SWT.NONE);
            expireLabel.setText(Messages.validTill);

            final Text expireText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
            expireText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
            expireText.setText(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(user.getTimeUntilValid()));

            new Label(composite, SWT.NONE);
        }

        passwordTextLDAP = createPasswordArea(composite);
        anonymous = new Button(composite , SWT.CHECK);
        anonymous.setText(Messages.anonymousLogin);
        anonymous.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                usernameLdapText.setEnabled(!anonymous.getSelection());
                passwordTextLDAP.setEnabled(!anonymous.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });
        return composite;
    }

    @Override
    protected void okPressed() {

        if (currentlySelectedTabTitle.equals(Messages.ldapTabName)){
            // get values

            anonymousLogin = anonymous.getSelection();

            if (!anonymous.getSelection()){
                usernameLdap = usernameLdapText.getText();
                password = passwordTextLDAP.getText();
                // check values
                if (usernameLdap.isEmpty() || password.isEmpty()){
                    MessageDialog.openError(getShell(),
                        Messages.reLoginDialog,
                        Messages.ldapUsernameAndPasswordRequired);
                    return;
                }
            }
        } else {
            throw new AssertionError();
        }

        loginDialogController.saveSettings();

        super.okPressed();
        relogin = true;
    }

    /**
     * Creates the proxy certificate area.
     * 
     * @param composite The Composite the area belongs to.
     */
    private void createProxyCertificateArea(final Composite composite) {

        final Label nameLabel = new Label(composite, SWT.NONE);
        nameLabel.setText(Messages.cert);

        final Text nameText = new Text(composite, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
        nameText.setText(getCertificateText());

        new Label(composite, SWT.NONE);

        final Label expireLabel = new Label(composite, SWT.NONE);
        expireLabel.setText(Messages.validTill);

        final Text expireText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
        expireText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
        expireText.setText(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(user.getTimeUntilValid()));

        new Label(composite, SWT.NONE);

    }

    /** 
     * Creates the certificate area.
     * 
     * @param composite The Composite the area belongs to.
     */
    private void createCertificateArea(final Composite composite) {
        final Label certificateLabel = new Label(composite, SWT.NONE);
        certificateLabel.setText(Messages.cert);

        certificatePathText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
        certificatePathText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
        certificatePathText.setToolTipText(Messages.chooseValidCert);

        // set certificate path
        certificatePathButton = new Button(composite, SWT.PUSH);
        certificatePathButton.setText("..."); //$NON-NLS-1$
        certificatePathButton.setToolTipText(Messages.chooseNewCert);
    }

    /**
     * Creates the private key area.
     * 
     * @param composite The Composite the area belongs to.
     */
    private void createPrivateKeyArea(final Composite composite) {
        final Label keyLabel = new Label(composite, SWT.NONE);
        keyLabel.setText(Messages.privateKey);

        keyPathText = new Text(composite, SWT.BORDER);
        keyPathText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        keyPathText.setEditable(false);
        keyPathText.setToolTipText(Messages.searchMatchingKey);

        // choose key file
        keyPathButton = new Button(composite, SWT.PUSH);
        keyPathButton.setText("..."); //$NON-NLS-1$
        keyPathButton.setToolTipText(Messages.chooseNewKey);

    }

    /**
     * Creates the username area for the LDAP login.
     * 
     * @param composite The Composite the area belongs to.
     */
    private void createUsernameLdapArea(final Composite composite){
        final Label usernameLdapLabel = new Label(composite, SWT.NONE);
        usernameLdapLabel.setText(Messages.username);

        usernameLdapText = new Text(composite, SWT.BORDER);
        usernameLdapText.setText(usernameLdap); //$NON-NLS-1$
        usernameLdapText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

        new Label(composite, SWT.NONE);
    }

    /**
     * Creates the password area.
     * 
     * @param composite The Composite the area belongs to.
     */
    private Text createPasswordArea(final Composite composite) {
        final Label passwordLabel = new Label(composite, SWT.NONE);
        passwordLabel.setText(Messages.password);

        Text textField = new Text(composite, SWT.PASSWORD | SWT.BORDER);

        textField.setText(""); //$NON-NLS-1$
        textField.setToolTipText(Messages.validPassword);

        textField.setData("LoginDialog." + "passwordText"); //$NON-NLS-1$ //$NON-NLS-2$
        textField.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        textField.setFocus();

        new Label(composite, SWT.NONE);

        return textField;
    }


    /**
     * Splits the proxy certificate information in 2 parts.
     * 
     * @return splitted proxy certificate information.
     */
    private String getCertificateText() {

        String pc = user.getUserId();
        final int certificateHalfLength = pc.length() / 2;
        final String secondHalf = pc.substring(certificateHalfLength);
        final int commaPosition = secondHalf.indexOf(","); //$NON-NLS-1$

        int partingPositon = 0;
        if (commaPosition > 0) {
            partingPositon = commaPosition;
        }
        partingPositon = partingPositon + certificateHalfLength + 1;
        return pc.substring(0, partingPositon) + "\n" + pc.substring(partingPositon); //$NON-NLS-1$
    }


}
