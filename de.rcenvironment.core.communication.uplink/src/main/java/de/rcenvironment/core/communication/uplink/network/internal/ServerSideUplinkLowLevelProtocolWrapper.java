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

import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Encapsulates server-side handshake handling, low-level message sending and receiving, and session teardown for an uplink connection.
 *
 * @author Robert Mischke
 */
public class ServerSideUplinkLowLevelProtocolWrapper extends CommonUplinkLowLevelProtocolWrapper {

    /**
     * End-user error message part if an internal error occurs during handshake; public for unit testing.
     */
    public static final String ERROR_MESSAGE_CONNECTION_SETUP_FAILED = "Error during connection setup";

    public ServerSideUplinkLowLevelProtocolWrapper(StreamConnectionEndpoint connectionEndpoint,
        UplinkConnectionLowLevelEventHandler eventHandler, String logIdentity) {
        super(connectionEndpoint, eventHandler, logIdentity);
    }

    @Override
    protected void runHandshakeSequence() throws UplinkConnectionRefusedException {
        // TODO ensure proper session teardown on handshake failure
        try {
            if (verboseLoggingEnabled) {
                log.debug(logPrefix + "Expecting handshake init");
            }
            expectHandshakeInit();
        } catch (ProtocolException e) {
            // internal exception
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.PROTOCOL_VERSION_MISMATCH,
                e.getMessage(), false);
        } catch (IOException e) {
            // internal exception
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Error while receiving remote handshake initialization" + e.toString(), false);
        } catch (TimeoutException e) {
            // internal exception
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Timeout while receiving remote handshake initialization", false);
        }

        // not strictly required yet, as the protocol expects the client to send its handshake data right away,
        // but this allows more flexibility on the client side
        try {
            sendHandshakeInit();
        } catch (IOException e) {
            // internal exception
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Error while trying to send initial handshake reponse: " + e.toString(), false);
        }

        MessageBlock handshakeData;
        try {
            handshakeData = expectHandshakeData();
        } catch (UplinkConnectionRefusedException e) {
            // rethrow
            throw e;
        } catch (TimeoutException e) {
            // remote error message
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.PROTOCOL_VIOLATION,
                "Failed to receive client data within " + UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC
                    + " msec, closing the connection",
                true);
        } catch (IOException e) {
            // no point in sending a goodbye if the confirmation could not be read -> internal exception
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Error receiving client handshake data: " + e.toString(), false);
        }

        MessageBlock responseData;
        try {
            responseData = processHandshakeDataAndGenerateResponse(handshakeData);
        } catch (ProtocolException e) {
            // generate a sanitized message that can be sent back to the client
            final String errorMarker = LogUtils.logExceptionAsSingleLineAndAssignUniqueMarker(log,
                logPrefix + "Error while processing handshake data, closing incoming connection", e);
            // generate an exception that causes an error message to be sent
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR,
                ERROR_MESSAGE_CONNECTION_SETUP_FAILED + " (internal error log marker " + errorMarker + ")", true);
        } catch (UplinkConnectionRefusedException e) {
            // rethrow
            throw e;
        }

        try {
            sendHandshakeData(responseData);
        } catch (IOException e) {
            // if we can't send the handshake data, don't try to send an error goodbye -> internal exception
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Error while trying to send handshake reponse data: " + e.toString(), false);
        }

        // expect handshake confirmation from the client
        try {
            handshakeData = expectHandshakeData();
        } catch (IOException e) {
            // no point in sending a goodbye if the confirmation could not be read -> internal exception
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.LOW_LEVEL_CONNECTION_ERROR,
                "Error while waiting for the client's handshake confirmation: " + e.getMessage(), false);
        } catch (UplinkConnectionRefusedException e) {
            // rethrow
            throw e;
        } catch (TimeoutException e) {
            // remote error message
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.PROTOCOL_VIOLATION,
                "Failed to receive the client's handshake confirmation within " + UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC
                    + " msec, closing the connection",
                true);
        } // contains no data, just an empty "handshake" message block
    }

    private MessageBlock processHandshakeDataAndGenerateResponse(MessageBlock handshakeData)
        throws ProtocolException, UplinkConnectionRefusedException {
        if (verboseLoggingEnabled) {
            log.debug(logPrefix + "Processing handshake data: " + new String(handshakeData.getData()));
        }
        // parse received JSON
        final Map<String, String> incomingData = messageConverter.decodeHandshakeData(handshakeData);

        Map<String, String> responseMap = new HashMap<>();
        // build response
        eventHandler.provideOrProcessHandshakeData(incomingData, responseMap);

        // encode to JSON message
        return messageConverter.encodeHandshakeData(responseMap);
    }

}
