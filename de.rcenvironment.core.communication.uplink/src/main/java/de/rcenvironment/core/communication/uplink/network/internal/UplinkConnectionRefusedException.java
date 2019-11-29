/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

/**
 * An exception transporting additional reason information when a client connection is refused.
 *
 * @author Robert Mischke
 */
public final class UplinkConnectionRefusedException extends Exception {

    private static final long serialVersionUID = 4002255419745384173L;

    private final UplinkProtocolErrorType type;

    private final String rawMessage;

    public UplinkConnectionRefusedException(UplinkProtocolErrorType type, String rawMessage) {
        this.type = type;
        this.rawMessage = rawMessage;
    }

    public UplinkProtocolErrorType getType() {
        return type;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    @Override
    public String getMessage() {
        return type.wrapErrorMessage(rawMessage); // only construct when needed
    }
}
