/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

/**
 * Represents an SSH connection setup.
 *
 * @author Brigitte Boden
 * @author Kathrin Schaffert (added method setDisplayName)
 */
public interface SshUplinkConnectionSetup {

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
     * @return qualifier for this connection.
     */
    String getQualifier();

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
    ClientSideUplinkSession getSession();

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
     * Get the setting using a passphrase.
     * 
     * @return true, if a passphrase is used.
     */
    boolean getUsePassphrase();

    /**
     * Reset count of consecutive failures to 0.
     * 
     */
    void resetConsecutiveConnectionFailures();

    /**
     * Raise count of consecutive failures.
     * 
     */
    void raiseConsecutiveConnectionFailures();

    /**
     * Get count of consecutive failures.
     * 
     * @return number of connection failures.
     */
    int getConsecutiveConnectionFailures();

    /**
     * Set the session.
     * 
     * @param session The ClientSideUplinkSession
     */
    void setSession(ClientSideUplinkSession session);

    /**
     * Sets the prefix for destinationIds sent over this connection.
     * 
     * @param destinationIdPrefix the prefix
     */
    void setDestinationIdPrefix(String destinationIdPrefix);

    /**
     * @return The prefix for destinationIds sent over this connection.
     */
    String getDestinationIdPrefix();

    /**
     * @return if this instance should act as gateway.
     */
    boolean isGateway();

    /**
     * Sets the display name for this connection.
     * 
     * @param displayName the display name
     */
    void setDisplayName(String displayName);

    /**
     * Gets whether the user wishes to try a reconnect.
     * 
     * @return true if the wishes to try a reconnect
     */
    boolean wantToReconnect();

}
