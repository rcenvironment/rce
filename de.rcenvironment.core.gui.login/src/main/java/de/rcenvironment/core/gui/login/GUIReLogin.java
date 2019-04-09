/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.login;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.authentication.Session;
import de.rcenvironment.core.authentication.User.Type;
import de.rcenvironment.core.gui.login.internal.LoginDialog;
import de.rcenvironment.core.gui.login.internal.Messages;
import de.rcenvironment.core.login.AbstractLogin;
import de.rcenvironment.core.login.LoginInput;

/**
 * 
 * Concrete implementation of {@link AbstractLogin} for graphical re-login.
 *
 * @author Bea Hornef
 * @author Alice Zorn
 */
public class GUIReLogin extends AbstractLogin {

    @Override
    protected LoginInput getLoginInput() {
        LoginInput loginInput = null;
        
        LoginDialog loginDialog;
        try {
            Session s = Session.getInstance();
            if (s.getUser().getType() == Type.single) {
                loginDialog = new LoginDialog(authenticationService, loginConfiguration);
            } else if (s.getUser().getType() == Type.ldap) {
                loginDialog = new LoginDialog(s.getUser(), s.getUser().getUserId(), authenticationService, loginConfiguration);
            } else {
                throw new AssertionError();
            }
        } catch (AuthenticationException e) {
            loginDialog = new LoginDialog(authenticationService, loginConfiguration);
        }
        
        // login window is open... waits until the ok-button is pressed
        if (loginDialog.open() == Window.OK) {
            // gets an object LoginInput with the data saved in loginDialog
            loginInput = loginDialog.getLoginInput();
        }

        return loginInput;
    }

    @Override
    protected void informUserAboutError(String errorMessage, Throwable e) {
        MessageDialog.openError(null, Messages.login, errorMessage);
    }

}
