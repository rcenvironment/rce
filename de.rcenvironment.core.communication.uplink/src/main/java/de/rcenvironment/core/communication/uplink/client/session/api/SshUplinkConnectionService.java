/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;


/**
 * Service managing the current SSH uplink connections.
 *
 * @author Brigitte Boden
 */
public interface SshUplinkConnectionService {

    /**
     * Return the connection with the given Id.
     * 
     * @param connnectionId The id of the required connection.
     * @return The connection with the given Id, if existing, and null, else.
     */
    SshUplinkConnectionSetup getConnectionSetup(String connnectionId);

    /**
     * Returns the SSH session for the given Id, if connected.
     * 
     * @param connnectionId The id of the required connection.
     * @return The connection with the given Id, if existing, and null, else.
     * @throws SshParameterException
     * @throws JSchException
     */
    ClientSideUplinkSession getAvtiveSshUplinkSession(String connnectionId);

    /**
     * Adds a new ssh connection config to the service (does not store the passphrase). For adding keyfile and/or passphrase, call the
     * "setAuthDataForSshConnection" method after adding the connection.
     *
     * @param context Parameters for the connection.
     * @return connection Id for the new connection
     */
    String addSshUplinkConnection(SshConnectionContext context);

    /**
     * Edits the ssh connection config with the given id (does not store the passphrase).
     * @param context Parameters for the connection.
     */
    void editSshUplinkConnection(SshConnectionContext context);

    /**
     * Edits the keyfile/passphrase for the ssh connection with the given id, stores it in the secure storage, if storePassphrase is set.
     * 
     * @param id The id of the configuration to edit.
     * @param sshAuthPassPhrase passphrase (for password authentication or for the keyfile)
     * @param storePassphrase if the passphrase should be stored in the secure storage
     */
    void setAuthPhraseForSshConnection(String id, String sshAuthPassPhrase, boolean storePassphrase);

    /**
     * Return all available connections (connected or not).
     * 
     * @return List of all connections.
     */
    Collection<SshUplinkConnectionSetup> getAllSshConnectionSetups();

    /**
     * Return all available connections that are currently connected.
     * 
     * @return List of all connections.
     */
    Map<String, SshUplinkConnectionSetup> getAllActiveSshConnectionSetups();

    /**
     * Return the ids of all available connections that are currently connected.
     * 
     * @return List of all connections.
     */
    Collection<String> getAllActiveSshConnectionSetupIds();

    /**
     * Returns true, if there is and active ssh session with the given connection id.
     * 
     * @param connectionId The connection id.
     * @return true, if there is and active ssh session with the given connection id, false else.
     */
    boolean isConnected(String connectionId);
    
    /**
     * Returns true, if the connection will be automatically retried.
     * 
     * @param connectionId The connection id.
     * @return true, if there is and active ssh session with the given connection id, false else.
     */
    boolean isWaitingForRetry(String connectionId);

    /**
     * Connects the SSH session for the given Id, assuming the passphrase is stored in the secure storage.
     * 
     * @param connectionId Id of the connection to be connected.
     * @throws SshParameterException
     * @throws JSchException
     */
    void connectSession(String connectionId);

    /**
     * Connects the SSH session for the given Id.
     * 
     * @param connectionId Id of the connection to be connected.
     * @param passphrase Passphrase for the SSH connection
     * @throws SshParameterException
     * @throws JSchException
     */
    void connectSession(String connectionId, String passphrase);

    /**
     * Disconnects the SSH session for the given Id.
     * 
     * @param connectionId Id of the session to be connected.
     * @throws SshParameterException
     * @throws JSchException
     */
    void disconnectSession(String connectionId);

    /**
     * Disposes the SSH session configuration for the given Id.
     * 
     * @param connectionId Id of the session to be disposed.
     * @throws SshParameterException
     * @throws JSchException
     */
    void disposeConnection(String connectionId);

    /**
     * Retreives the password for a connection id from the secure storage.
     * 
     * @param connectionId the connection id.
     * @return the password
     */
    String retrieveUplinkConnectionPassword(String connectionId);

    /**
     * Add a {@link SshUplinkConnectionListener}.
     * 
     * @param listener the listener to add.
     */
    void addListener(SshUplinkConnectionListener listener);
    
    /**
     * Check if that connection already exists.
     * 
     * @param context the {@link SshConnectionContext}.
     * @return returns true if a connection with the same host and port already exists. Otherwise returns false.
     */
    boolean sshUplinkConnectionAlreadyExists(SshConnectionContext context);
}
