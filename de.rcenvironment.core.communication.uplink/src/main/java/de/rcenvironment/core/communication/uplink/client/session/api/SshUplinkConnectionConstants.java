/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

/**
 * Constants for SSH UplinkConnections.
 *
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public final class SshUplinkConnectionConstants {

    /**
     * Name of the node for SSH passwords in the secure storage.
     */
    public static final String UPLINK_CONNECTIONS_PASSWORDS_NODE = "connections.uplink.ssh.passwords";

    /**
     * Delay before retrying to connect SSH connection.
     */
    public static final int DELAY_BEFORE_RETRY = 5000;

    /**
     * The pseudo command that is "executed" from the viewpoint of the SSH uplink client to attach input and output streams to. Must be
     * equal to SshConstants.SSH_UPLINK_VIRTUAL_CONSOLE_COMMAND (which is not directly referenced to avoid a technical dependency).
     */
    public static final String VIRTUAL_CONSOLE_COMMAND = "ra uplink";

    /**
     * The path within &lt;profile dir>/import to check for password files; if found, an uplink connection password entry will be created
     * (or an existing one replaced) with the filename (minus an optional .txt extension) as the connection id, and the trimmed content as
     * the password. Each imported file will be deleted.
     */
    public static final String PASSWORD_FILE_IMPORT_SUBDIRECTORY = "uplink-pws";

    private SshUplinkConnectionConstants() {}
}
