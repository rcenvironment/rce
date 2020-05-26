/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.authentication;


/**
 * Represents a user used for RCE single-user-mode.
 *
 * @author Doreen Seider
 */
public class SingleUser extends User {

    private static final long serialVersionUID = -6958573657014138419L;

    public SingleUser(int validityInDays) {
        super(validityInDays);
    }

    @Override
    public String getUserId() {
        return "Chief Engineer";
    }

    @Override
    public String getDomain() {
        return "DLR";
    }

    @Override
    public Type getType() {
        return Type.single;
    }

}
