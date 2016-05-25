/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

/**
 * An interface that allows management of temporary SSH/SCP accounts by other services.
 * 
 * @author Robert Mischke
 * 
 */
// TODO review: is this interface still needed/useful?
public interface TemporarySshAccountControl {

    /**
     * Creates a new temporary account with an random password and name, and randomly generated SCP paths.
     * 
     * @return the new temporary SSH account
     */
    TemporarySshAccount createTemporarySshAccount();

    /**
     * Discards the temporary account with the given user name.
     * 
     * @param name the login name of the account to discard
     */
    void discardTemporarySshAccount(String name);
}
