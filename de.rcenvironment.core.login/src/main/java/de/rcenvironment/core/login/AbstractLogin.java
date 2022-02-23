/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.authentication.AuthenticationService;
import de.rcenvironment.core.authentication.Session;
import de.rcenvironment.core.authentication.User.Type;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.login.internal.Messages;
import de.rcenvironment.core.login.internal.ServiceHandler;
import de.rcenvironment.core.notification.DistributedNotificationService;

/**
 * Class handling the login process.
 * 
 * @author Bea Hornef
 * @author Doreen Seider
 * @author Heinrich Wendel
 */
public abstract class AbstractLogin {

    /** Identifier of the notification sent on successful login. */
    public static final String LOGIN_NOTIFICATION_ID = "de.rcenvironment.rce.login.success"; //$NON-NLS-1$

    protected static final Log LOGGER = LogFactory.getLog(AbstractLogin.class);

    private static final String LOGIN_FAILED_1_9 = "Authentication failed due to an invalid certificate or private key"; //$NON-NLS-1$

    private static final String LOGIN_FAILED_1_2 = "Authentication failed due to an missing password"; //$NON-NLS-1$

    private static final String LOGIN_FAILED_1_3 = "Authentication failed due to an incorrect password"; //$NON-NLS-1$

    private static final String LOGIN_FAILED_1_4 = "Authentication failed due to an not matching caertificate - key file"; //$NON-NLS-1$

    private static final String LOGIN_FAILED_1_5 =
        "Authentication failed cause certificate not signed by trusted certificate authority (CA)."; //$NON-NLS-1$

    private static final String LOGIN_FAILED_1_6 = "Authentication failed cause certificate is revoked by its "
        + "certificate authority (CA)."; //$NON-NLS-1$

    private static final String LOGIN_FAILED_1_7 = "Authentication failed due to an unknown reason."; //$NON-NLS-1$

    private static final String LOGIN_FAILED_2_1 = "Authentication failed due to an invalid password.";

    private static final String LOGIN_FAILED_2_2 = "Authentication failed due to an incorrect password or username.";

    private static final String LOGIN_SUCCESFULL = "Authentication successful. Session created."; //$NON-NLS-1$

    protected AuthenticationService authenticationService;

    protected DistributedNotificationService notificationService;

    protected ConfigurationService configurationService;

    protected LoginConfiguration loginConfiguration;

    public AbstractLogin() {
        authenticationService = ServiceHandler.getAuthenticationService();
        notificationService = ServiceHandler.getNotificationService();
        configurationService = ServiceHandler.getConfigurationService();
        // note: disabled old configuration loading for 6.0.0 as it is not being used anyway
        // loginConfiguration = configurationService.getConfiguration(ServiceHandler.getBundleSymbolicName(), LoginConfiguration.class);
        // TODO using default values until reworked or removed
        loginConfiguration = new LoginConfiguration();
        loginConfiguration.setCertificateFile(configurationService.resolveBundleConfigurationPath(ServiceHandler.getBundleSymbolicName(),
            loginConfiguration.getCertificateFile()));
        loginConfiguration.setKeyFile(configurationService.resolveBundleConfigurationPath(ServiceHandler.getBundleSymbolicName(),
            loginConfiguration.getKeyFile()));
    }

    /**
     * Handles login.
     * 
     * @return True for success, else false.
     */
    public final boolean login() {

        try {
            boolean loginSuccess = false;
            // while login not successful try ... harder
            while (!loginSuccess) {
                LoginInput loginInput = getLoginInput();
                loginSuccess = singleUserLogin();
                if (loginInput == null) {
                    break;
                }
                if (loginInput.getType() == Type.single) {
                    notificationService.send(LOGIN_NOTIFICATION_ID, "Anonymouslogin"); //$NON-NLS-1$
                    LOGGER.debug("Using anonymous/default login");
                } else if (loginInput.getType() == Type.ldap) {
                    loginSuccess = ldapLogin(loginInput.getUsernameLDAP(), loginInput.getPassword());
                }
            }
            if (loginSuccess) {
                notificationService.send(LOGIN_NOTIFICATION_ID, "Login successful."); //$NON-NLS-1$
                return true;
            }
            // do something with the login info
        } catch (AuthenticationException e) {
            informUserAboutError(Messages.authenticationFailed + " " //$NON-NLS-1$
                + Messages.certOrKeyIncorrect, e);
            LOGGER.error(LOGIN_FAILED_1_9, e);
        }
        return false;
    }

    private boolean singleUserLogin() {
        Session.create(authenticationService.createUser(loginConfiguration.getValidityInDays()));
        return true;
    }

    /**
     * 
     * Handles login via LDAP.
     * 
     * @return True for success, else false.
     */
    private boolean ldapLogin(String userID, String password) {
        boolean isLoginSuccessful = false;

        AuthenticationService.LDAPAuthenticationResult authenticationResult = authenticationService.
            authenticate(userID, password);

        if (AuthenticationService.LDAPAuthenticationResult.AUTHENTICATED == authenticationResult) {
            isLoginSuccessful = true;
            Session.create(userID, loginConfiguration.getValidityInDays());
            LOGGER.info(LOGIN_SUCCESFULL);
        } else {
            isLoginSuccessful = false;
            String reasonForFailing;
            String reasonForFailingEN;
            switch (authenticationResult) {
            case PASSWORD__OR_USERNAME_INVALID:
                reasonForFailing = Messages.passwordInvalid;
                reasonForFailingEN = LOGIN_FAILED_2_1;
                break;
            case PASSWORD_OR_USERNAME_INCORRECT:
                reasonForFailing = Messages.passwordOrUsernameIncorrect;
                reasonForFailingEN = LOGIN_FAILED_2_2;
                break;
            default:
                reasonForFailing = Messages.unknownReason;
                reasonForFailingEN = LOGIN_FAILED_1_7;
                break;
            }
            informUserAboutError(Messages.authenticationFailed + " " //$NON-NLS-1$
                + reasonForFailing, null);
            LOGGER.error(reasonForFailingEN);
        }

        return isLoginSuccessful;
    }

    /**
     * Handles logout.
     */
    public final void logout() {
        try {
            Session.getInstance().destroy();
        } catch (AuthenticationException e) {
            LOGGER.warn("Already logged out."); //$NON-NLS-1$
        }
    }

    /**
     * Informs the user about an error.
     * 
     * @param errorMessage The message containing the information about the error.
     * @param e {@link Throwable} if there is one.
     */
    protected abstract void informUserAboutError(String errorMessage, Throwable e);

    /**
     * Must be implemented by login provider. Return the chosen login input. Can make use of createLoginInput.
     * 
     * @return the login input getting from the user.
     * @throws AuthenticationException Thrown when loading key or path failed.
     */
    protected abstract LoginInput getLoginInput() throws AuthenticationException;

    /**
     * 
     * @param usernameLDAP The username for the LDAP login.
     * @param password The password for the login.
     * @return The generated {@link LoginInput}.
     * @throws AuthenticationException
     */
    protected LoginInput createLoginInputLDAP(String usernameLDAP, String password)
        throws AuthenticationException {

        LoginInput loginInput = new LoginInput(usernameLDAP, password);
        return loginInput;
    }

}
