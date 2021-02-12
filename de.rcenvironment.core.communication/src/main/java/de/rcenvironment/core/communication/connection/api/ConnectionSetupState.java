/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.api;

/**
 * Indicates the state of a {@link ConnectionSetup}.
 * 
 * @author Robert Mischke
 */
public enum ConnectionSetupState {

    /**
     * The is connecting to a remote contact point.
     */
    CONNECTING("connecting...", false, true),

    /**
     * The connection setup has an associated ready-to-use message channel.
     */
    CONNECTED("connected", false, true),

    /**
     * A disconnect has been triggered; see {@link DisconnectReason} for information about the cause.
     */
    DISCONNECTING("disconnecting...", false, false),

    /**
     * The channel has been closed/terminated; also the initial state of a {@link ConnectionSetup}. See {@link DisconnectReason} for
     * information about the cause.
     */
    DISCONNECTED("disconnected", true, false),

    /**
     * A connection attempt failed, the setup is configured to retry automatically, but the delay until the next attempt is not over yet.
     */
    WAITING_TO_RECONNECT("connection failed, waiting to reconnect...", true, true);

    private final String displayText;

    private final boolean reasonableToAllowStart;

    private final boolean reasonableToAllowStop;

    ConnectionSetupState(String displayText, boolean reasonableToStart, boolean reasonableToStop) {
        this.displayText = displayText;
        this.reasonableToAllowStart = reasonableToStart;
        this.reasonableToAllowStop = reasonableToStop;
    }

    public String getDisplayText() {
        return displayText;
    }

    public boolean isReasonableToAllowStart() {
        return reasonableToAllowStart;
    }

    public boolean isReasonableToAllowStop() {
        return reasonableToAllowStop;
    }

}
