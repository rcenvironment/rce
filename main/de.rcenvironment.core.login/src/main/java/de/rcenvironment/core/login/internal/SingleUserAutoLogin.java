/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
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
 * Single user implementation of {@link AbstractLogin}.
 * @author Doreen Seider
 */
public class SingleUserAutoLogin extends AbstractLogin {

    @Override
    protected void informUserAboutError(String errorMessage, Throwable e) {
        LOGGER.error(errorMessage, e);
    }

    @Override
    protected LoginInput getLoginInput() throws AuthenticationException {
        return new LoginInput(true);
    }

}
