/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.relay.api;

import de.rcenvironment.core.communication.uplink.session.api.UplinkSession;

/**
 * Represents the server-side part of an uplink session. This is the counterpart to a single client-side uplink session. A single uplink
 * "relay" (which usually corresponds to a network port) typically mediates and forwards messages between multiple server-side sessions.
 *
 * @author Robert Mischke
 */
public interface ServerSideUplinkSession extends UplinkSession {

    /**
     * Performs the initial handshake and runs the message dispatch loop.
     * 
     * @return true if the connection was closed normally, false on abnormal termination (e.g. connection breakdown)
     */
    boolean runSession();

}
