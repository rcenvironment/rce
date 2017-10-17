/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

import de.rcenvironment.core.authentication.User;

/**
 * Dummy implementation of {@link User}.
 * @author Alice Zorn
 *
 */
public class DummyUser extends User {
    
    private static final long serialVersionUID = 5759924817385094533L;

    public DummyUser(int validityInDays) {
        super(validityInDays);
    }

    @Override
    public String getUserId() {
        return "dummy user";
    }

    @Override
    public String getDomain() {
        return "crash test";
    }

    @Override
    public Type getType() {
        return Type.ldap;
    }

}
