/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

/**
 * Represents an SSH account, authenticated with a public key or password.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public interface SshAccount {

    /**
     * @return the login/username
     */
    String getLoginName();

    /**
     * @return the clear-text login password (deprecated, as this is insecure)
     */
    String getPassword();

    /**
     * @return the configured hash of the login password
     */
    String getPasswordHash();

    /**
     * @return the string representation of this account's public key (TODO document format)
     */
    String getPublicKey();

    /**
     * @return the name of the role used to determine permissions for this user
     */
    String getRole();

    /**
     * @return true if this account should be enabled
     */
    boolean isEnabled();

}
