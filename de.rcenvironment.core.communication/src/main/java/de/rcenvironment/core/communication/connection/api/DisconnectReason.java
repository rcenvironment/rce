/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.api;

/**
 * Indicates why a {@link ConnectionSetup} is terminating, was terminated, or why it failed to connect.
 * 
 * @author Robert Mischke
 */
public enum DisconnectReason {

    /**
     * The connection could not be established during a normal connection attempt (no auto-retry).
     */
    FAILED_TO_CONNECT("failed to establish connection"),

    /**
     * The connection could not be established during an attempt to auto-reconnect.
     */
    FAILED_TO_AUTO_RECONNECT("failed to auto-reconnect"),

    /**
     * A local action triggered the disconnect, e.g. a user command or the local node is shutting down.
     */
    // TODO >5.1.0: rename to match description
    ACTIVE_SHUTDOWN("active disconnect"),

    /**
     * The remote node is/was shutting down.
     */
    REMOTE_SHUTDOWN("the remote node has closed the connection"),

    /**
     * There has been a network or messaging error after the connection was established.
     */
    ERROR("connection error");

    private final String displayText;

    DisconnectReason(String displayText) {
        this.displayText = displayText;
    }

    public String getDisplayText() {
        return displayText;
    }
}
