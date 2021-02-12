/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection;

import java.util.Collection;
import java.util.Map;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;

/**
 * Service managing the current SSH connections.
 *
 * @author Brigitte Boden
 */
public interface SshConnectionService {

    /**
     * Return the connection with the given Id.
     * 
     * @param connnectionId The id of the required connection.
     * @return The connection with the given Id, if existing, and null, else.
     */
    SshConnectionSetup getConnectionSetup(String connnectionId);

    /**
     * Returns the SSH session for the given Id, if connected.
     * 
     * @param connnectionId The id of the required connection.
     * @return The connection with the given Id, if existing, and null, else.
     * @throws SshParameterException
     * @throws JSchException
     */
    Session getAvtiveSshSession(String connnectionId);

    /**
     * Adds a new ssh connection config to the service (does not store the passphrase). For adding keyfile and/or passphrase, call the
     * "setAuthDataForSshConnection" method after adding the connection.
     *
     * @param context Parameters for the connection.
     * @return connection Id for the new connection
     */
    String addSshConnection(SshConnectionContext context);

    /**
     * Edits the ssh connection config with the given id (does not store the passphrase).
     * @param context Parameters for the connection.
     */
    void editSshConnection(SshConnectionContext context);

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
    Collection<SshConnectionSetup> getAllSshConnectionSetups();

    /**
     * Return all available connections that are currently connected.
     * 
     * @return List of all connections.
     */
    Map<String, SshConnectionSetup> getAllActiveSshConnectionSetups();

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
     * @return The session, if the connection attempt succeeded, and null, else.
     * @throws SshParameterException
     * @throws JSchException
     */
    Session connectSession(String connectionId);

    /**
     * Connects the SSH session for the given Id.
     * 
     * @param connectionId Id of the connection to be connected.
     * @param passphrase Passphrase for the SSH connection
     * @return The session, if the connection attempt succeeded, and null, else.
     * @throws SshParameterException
     * @throws JSchException
     */
    Session connectSession(String connectionId, String passphrase);

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
    String retrieveSshConnectionPassword(String connectionId);
    
    /**
     * @param context the {@link SshConnectionContext}
     * @return if a connection with this host and port already exists
     */
    boolean sshConnectionAlreadyExists(SshConnectionContext context);
}
