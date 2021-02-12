/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * An exception transporting additional reason information when a client connection is refused.
 *
 * TODO consider renaming this to UplinkHandshakeFailureException (or similar) for clarity
 *
 * @author Robert Mischke
 */
public final class UplinkConnectionRefusedException extends Exception {

    private static final long serialVersionUID = 4002255419745384173L;

    private final UplinkProtocolErrorType type;

    private final String rawMessage;

    private final boolean attemptToSendErrorGoodbye;

    public UplinkConnectionRefusedException(UplinkProtocolErrorType type, String rawMessage, boolean attemptToSendErrorGoodbye) {
        this.type = type;
        this.rawMessage = rawMessage;
        this.attemptToSendErrorGoodbye = attemptToSendErrorGoodbye;
    }

    public UplinkProtocolErrorType getType() {
        return type;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public boolean shouldAttemptToSendErrorGoodbye() {
        return attemptToSendErrorGoodbye;
    }

    @Override
    public String getMessage() {
        return StringUtils.format("[%d] %s", type.getCode(), rawMessage);
    }
}
