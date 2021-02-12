/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Encapsulates client-side handshake handling, low-level message sending and receiving, and session teardown for an uplink connection.
 *
 * @author Robert Mischke
 */
public class ClientSideUplinkLowLevelProtocolWrapper extends CommonUplinkLowLevelProtocolWrapper {

    private boolean connectionClosedWithError; // used to prevent duplicate error events sent to the caller; synchronized on "this"

    /**
     * @param eventHandler the callback interface for incoming events, including received {@link MessageBlock}s
     * @param logIdentity a name that can be optionally attached to log messages for identification
     */
    public ClientSideUplinkLowLevelProtocolWrapper(StreamConnectionEndpoint connectionEndpoint,
        UplinkConnectionLowLevelEventHandler eventHandler, String logIdentity) {
        super(connectionEndpoint, eventHandler, logIdentity);
    }

    @Override
    protected void runHandshakeSequence() throws UplinkConnectionRefusedException {

        final MessageBlock handshakeData;
        try {
            handshakeData = generateHandshakeData();
        } catch (ProtocolException e) {
            log.error("Unexpected error during handshake data generation: " + e.toString());
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.INTERNAL_CLIENT_ERROR,
                "Error generating the local data to send to the server", false);
        }

        try {
            sendHandshakeInit();
            sendHandshakeData(handshakeData);
        } catch (IOException e) {
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Error sending the initial data to the server", false);
        }

        try {
            expectHandshakeInit();
        } catch (IOException e1) {
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Error receiving the server's initial response; most likely, the connection has been closed by the server. "
                    + "Make sure that you connecting to an Uplink server, and that the server's version is generally compatible.",
                false);
        } catch (TimeoutException e) {
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "No initial response received from the server within the expected time. This may be caused by a firewall "
                    + "blocking the connection attempt, or because you accidentally connected to a different kind of server.",
                false);
        }

        MessageBlock responseMessageBlock;
        try {
            responseMessageBlock = expectHandshakeData();
        } catch (IOException e) {
            // unusual case, so embed the exception info
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Error reading the connection response from the server: " + e.toString(), false);
        } catch (UplinkConnectionRefusedException e) {
            // rethrow
            throw e;
        } catch (TimeoutException e) {
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR,
                "Received an initial response from the server, but reached the timeout while waiting for further data - "
                    + "assuming an internal server error",
                false);
        }
        try {
            processHandshakeResponse(responseMessageBlock);
        } catch (IOException e) {
            // unusual case, so embed the exception info
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.INVALID_HANDSHAKE_DATA,
                "Failed to process the response received from the server: " + e.toString(), false);
        }

        try {
            sendHandshakeConfirmation();
        } catch (IOException e) {
            // unusual case, so embed the exception info
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Failed to send the final confirmation to the server after a successful message exchange: " + e.toString(), false);
        }

    }

    private MessageBlock generateHandshakeData() throws ProtocolException {
        final Map<String, String> dataMap = new HashMap<>();
        try {
            eventHandler.provideOrProcessHandshakeData(null, dataMap); // generate initial data
        } catch (UplinkConnectionRefusedException e) {
            throw new IllegalStateException("Unexpected internal error: The client should never fail to produce its handshake data");
        }
        return messageConverter.encodeHandshakeData(dataMap);
    }

    private void processHandshakeResponse(MessageBlock responseMessageBlock) throws IOException {
        final Map<String, String> handshakeResponseData = messageConverter.decodeHandshakeData(responseMessageBlock);

        try {
            eventHandler.provideOrProcessHandshakeData(handshakeResponseData, null); // process the relay's response data
        } catch (UplinkConnectionRefusedException e) {
            throw new IOException("Unexpected error while processing the relay's handshake response: " + e.getMessage());
        }
    }

    private synchronized boolean registerAsFirstCriticalError() {
        final boolean result = !connectionClosedWithError;
        connectionClosedWithError = true;
        return result;
    }
}
