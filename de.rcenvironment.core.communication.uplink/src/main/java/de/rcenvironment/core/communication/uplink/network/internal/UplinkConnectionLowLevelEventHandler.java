/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import java.io.IOException;
import java.util.Map;

import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Callback interface for low-level communication and session events.
 *
 * @author Robert Mischke
 */
public interface UplinkConnectionLowLevelEventHandler {

    /**
     * Requests the data to send as part of the initial protocol handshake. On the client side, this data is created without any remote
     * input; on the server side, this is created in reaction to the data that a client has sent.
     * 
     * @param incomingData the handshake data received from the other side; always null on the client side
     * @param outgoingData the map to insert the response data into
     * @throws ProtocolException on handshake failure, e.g. protocol version mismatch or invalid client handshake data
     * @throws UplinkConnectionRefusedException when the server side refuses the connection for some other reason, e.g. when it is about to
     *         shut down, or because of account-based connection limits
     */
    void provideOrProcessHandshakeData(Map<String, String> incomingData, Map<String, String> outgoingData)
        throws ProtocolException, UplinkConnectionRefusedException;

    /**
     * Called when the initial protocol handshake is complete, and bidirectional exchange of {@link MessageBlock}s commences.
     */
    void onHandshakeComplete();

    /**
     * Called when the remote side has closed the session by sending a "goodbye" message without any error information, which indicates a
     * regular shutdown.
     */
    void onRegularGoodbyeMessage();

    /**
     * Called when the remote side has closed the session by sending a "goodbye" message with an error message. This can occur on a variety
     * of errors, from recoverable protocol version mismatches, to handshake failures, to the other side closing the connection because it
     * is shutting down. This may also be sent optimistically by the other side if something irregular happened (e.g. a network protocol
     * violation).
     * 
     * @param type the parsed type of the error message, or {@link UplinkProtocolErrorType#UNKNOWN_ERROR} if it could not be identified
     * @param errorMessage the error message (unwrapped, ie without the encoded error type) provided by the other side
     */
    void onErrorGoodbyeMessage(UplinkProtocolErrorType type, String errorMessage);

    /**
     * Called when an error occurred outside of the protocol's messaging, e.g. an unexpected connection breakdown or an internal error
     * 
     * @param exception the exception representing the error
     */
    void onNonProtocolError(IOException exception);

    /**
     * Called sequentially for each received {@link MessageBlock}.
     * 
     * @param channelId the id of the virtual channel that this {@link MessageBlock} was marked for
     * @param message the received {@link MessageBlock}
     */
    void onMessageBlock(long channelId, MessageBlock message);

}
