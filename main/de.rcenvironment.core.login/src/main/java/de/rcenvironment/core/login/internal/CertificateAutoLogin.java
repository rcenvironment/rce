/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login.internal;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.login.AbstractLogin;
import de.rcenvironment.core.login.LoginInput;

/**
 * Concrete implementation of {@link AbstractLogin} for auto login.
 * 
 * @author Doreen Seider
 */
public class CertificateAutoLogin extends AbstractLogin {

    private int called = 0;

    @Override
    protected LoginInput getLoginInput() throws AuthenticationException {
        // this overridden method getLoginInput() must only return once an login input in this auto
        // login case because this input will never change and thus the behavior of calling method
        // login() will never change and will produce an endless loop
        if (called == 0) {
            called++;
            try {
                return createLoginInputCertificate(loginConfiguration.getCertificateFile(), loginConfiguration.getKeyFile(),
                    loginConfiguration.getAutoLoginPassword());
            } catch (AuthenticationException e) {
                informUserAboutError(Messages.autoLoginFailed, e);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected void informUserAboutError(String errorMessage, Throwable e) {
        LOGGER.error(errorMessage, e);
    }

}
