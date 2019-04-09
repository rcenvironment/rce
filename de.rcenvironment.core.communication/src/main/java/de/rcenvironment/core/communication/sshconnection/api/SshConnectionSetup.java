/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.api;

import com.jcraft.jsch.Session;

/**
 * Represents an SSH connection setup.
 *
 * @author Brigitte Boden
 */
public interface SshConnectionSetup {

    /**
     * @return the identifier for this connection.
     */
    String getId();

    /**
     * @return target host (name or ip) of the connection.
     */
    String getHost();

    /**
     * @return port of the connection.
     */
    int getPort();

    /**
     * @return username set for this connection.
     */
    String getUsername();

    /**
     * @return Displayname for this connection.
     */
    String getDisplayName();

    /**
     * @return location of private key file for this connection, or null, if password authentication is used.
     */
    String getKeyfileLocation();

    /**
     * @return true, if currently connected.
     */
    boolean isConnected();

    /**
     * @return true, if waiting for automatic retry.
     */
    boolean isWaitingForRetry();

    /**
     * Sets the waitingForRetry flag.
     * 
     * @param waitingForRetry "waiting for retry".
     */
    void setWaitingForRetry(boolean waitingForRetry);

    /**
     * @return the active sshSession, or null if this ConnectionSetup is not connected;
     */
    Session getSession();

    /**
     * Connects the stored sshSession.
     * 
     * @param passphrase the passphrase for this SSH account.
     * @return the active SSH session, or null, if connecting the session did not succeed.
     */
    Session connect(String passphrase);

    /**
     * Disconnects the stored sshSession.
     * 
     */
    void disconnect();

    /**
     * Get the setting for connect on startup.
     * 
     * @return true, if the connection should be immediately connected.
     */
    boolean getConnectOnStartUp();

    /**
     * Get the setting for auto retry.
     * 
     * @return true, if the connection should be automatically reconnected.
     */
    boolean getAutoRetry();

    /**
     * Get the setting for storing the passphrase.
     * 
     * @return true, if the passphrase is to be stored.
     */
    boolean getStorePassphrase();

    /**
     * Get the setting using a passphrase.
     * 
     * @return true, if a passphrase is used.
     */
    boolean getUsePassphrase();
}
