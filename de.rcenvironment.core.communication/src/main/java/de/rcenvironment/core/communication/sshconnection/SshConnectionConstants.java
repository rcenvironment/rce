/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.communication.sshconnection;


/**
 * Constants for SSH Connections.
 *
 * @author Brigitte Boden
 */
public final class SshConnectionConstants {

    /**
     * Name of the node for SSH passwords in the secure storage.
     */
    public static final String SSH_CONNECTIONS_PASSWORDS_NODE = "SSHConnectionsPasswords";
    
    /**
     * Required protocol version, for compatibility checking.
     */
    public static final String REQUIRED_PROTOCOL_VERSION = "9.0.0";
    
    /**
     * Delay before retrying to connect SSH connection.
     */
    public static final int DELAY_BEFORE_RETRY = 5000;

    private SshConnectionConstants(){}
}
