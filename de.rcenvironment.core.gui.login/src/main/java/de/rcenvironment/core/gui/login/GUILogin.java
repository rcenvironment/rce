/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.login;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;

import de.rcenvironment.core.gui.login.internal.LoginDialog;
import de.rcenvironment.core.gui.login.internal.Messages;
import de.rcenvironment.core.login.AbstractLogin;
import de.rcenvironment.core.login.LoginInput;

/**
 * 
 * Concrete implementation of {@link AbstractLogin} for graphical login.
 *
 * @author Doreen Seider
 * @author Heinrich Wendel
 */
public class GUILogin extends AbstractLogin {

    @Override
    protected LoginInput getLoginInput() {
        LoginInput loginInput = null;
        
        LoginDialog loginDialog = new LoginDialog(authenticationService, loginConfiguration);

        if (loginDialog.open() == Window.OK) {
            loginInput = loginDialog.getLoginInput();
        }

        return loginInput;
    }

    @Override
    protected void informUserAboutError(String errorMessage, Throwable e) {
        MessageDialog.openError(null, Messages.login, errorMessage);
    }

}
