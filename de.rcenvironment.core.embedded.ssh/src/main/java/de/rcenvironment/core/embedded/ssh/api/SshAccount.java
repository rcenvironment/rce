/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

import java.security.PublicKey;

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
     * @return the string representation of this account's public key (in ssh-rsa format like in an authorized_keys file)
     */
    String getPublicKey();
    
    /**
     * @return this account's public key (in ssh-rsa format like in an authorized_keys file)
     */
    PublicKey getPublicKeyObj();

    /**
     * @return the name of the role used to determine permissions for this user
     */
    String getRole();

    /**
     * @return true if this account should be enabled
     */
    boolean isEnabled();

}
