/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
     * Adds a new ssh connection config to the service and connects it, if connectOnStartup is set.
     * 
     * @param displayName Name to be shown in network view.
     * @param destinationHost The SSH host.
     * @param port The port of the SSH server.
     * @param sshAuthUser User name.
     * @param sshAuthPassPhrase Password.
     * @param storePassphrase if the passphrase should be stored in the secure store
     * @param connectOnStartup if the connection should be immediately connected.
     * @return connection Id for the new connection
     */
    String addSshConnectionWithAuthPhrase(String displayName, String destinationHost, int port, String sshAuthUser,
        String sshAuthPassPhrase, boolean storePassphrase, boolean connectOnStartup);

    /**
     * Edits the ssh connection config with the given id (does not store the passphrase!).
     * 
     * @param id The id of the configuration to edit.
     * @param displayName Name to be shown in network view.
     * @param destinationHost The SSH host.
     * @param port The port of the SSH server.
     * @param sshAuthUser User name.
     */
    void editSshConnection(String id, String displayName, String destinationHost, int port, String sshAuthUser);

    /**
     * Edits the passphrase for the ssh connection with the given id, stores it in the secure store, if storePassphrase is set, and connects
     * it, if connectOnStartup is set.
     * 
     * @param id The id of the configuration to edit.
     * @param sshAuthPassPhrase Password.
     * @param storePassphrase if the passphrase should be stored in the secure store
     * @param connectOnStartup if the connection should be immediately connected.
     */
    void editAuthPhraseForSshConnection(String id, String sshAuthPassPhrase, boolean storePassphrase, boolean connectOnStartup);

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
     * Retreives the password for a connection id from the secure store.
     * 
     * @param connectionId the connection id.
     * @return the password
     */
    String retreiveSshConnectionPassword(String connectionId);
}
