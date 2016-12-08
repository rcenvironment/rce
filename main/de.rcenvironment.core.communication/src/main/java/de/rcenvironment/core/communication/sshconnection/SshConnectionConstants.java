/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
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
     * Name of the node for SSH passwords in the secure store.
     */
    public static final String SSH_CONNECTIONS_PASSWORDS_NODE = "SSHConnectionsPasswords";
    
    /**
     * Required protocol version, for compatibility checking.
     */
    public static final String REQUIRED_PROTOCOL_VERSION = "8.0.0";

    private SshConnectionConstants(){}
}
