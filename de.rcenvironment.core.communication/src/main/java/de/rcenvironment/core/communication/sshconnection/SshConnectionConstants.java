/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection;

/**
 * Constants for SSH Connections.
 *
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public final class SshConnectionConstants {

    /**
     * Name of the node for SSH passwords in the secure storage.
     */
    @Deprecated // planned to be removed in RCE 11
    public static final String SSH_CONNECTIONS_PASSWORDS_NODE = "SSHConnectionsPasswords";

    /**
     * Required protocol version, for compatibility checking.
     */
    public static final String REQUIRED_PROTOCOL_VERSION = "10.0.0";

    /**
     * Delay before retrying to connect SSH connection.
     */
    public static final int DELAY_BEFORE_RETRY = 5000;

    /**
     * The path within &lt;profile dir>/import to check for password files; if found, an uplink connection password entry will be created
     * (or an existing one replaced) with the filename (minus an optional .txt extension) as the connection id, and the trimmed content as
     * the password. Each imported file will be deleted.
     */
    public static final String PASSWORD_FILE_IMPORT_SUBDIRECTORY = "ra-pws";

    private SshConnectionConstants() {}
}
