/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.utils.common.LogUtils;
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

    public ServerSideUplinkLowLevelProtocolWrapper(InputStream inputStream, OutputStream outputStream,
        UplinkConnectionLowLevelEventHandler eventHandler, String logIdentity) {
        super(eventHandler, "server session protocol wrapper");
        this.dataInputStream = new DataInputStream(inputStream);
        this.dataOutputStream = new DataOutputStream(outputStream);
    }

    @Override
    public void runSession() {
        // TODO ensure proper session teardown on handshake failure
        if (verboseLoggingEnabled) {
            log.debug("Expecting handshake init");
        }
        try {
            expectHandshakeInit();
        } catch (IOException e) {
            log.warn("Handshake init failed, closing incoming connection: " + e.toString());
            // simply close the connection; there is no point in sending a response if this basic init fails
            return;
        }
        // not strictly required yet, as the protocol expects the client to send its handshake data right away,
        // but this allows more flexibility on the client side
        try {
            sendHandshakeInit();
        } catch (IOException e) {
            // if this fails, the connection has broken down, so there is no point in sending an additional response
            log.warn("Failed to send handshake init response, closing incoming connection: " + e.toString());
            return;
        }

        MessageBlock handshakeData;
        try {
            handshakeData = expectHandshakeData();
        } catch (IOException e) {
            final String errorMessage = e.toString();
            log.warn("Error while expecting handshake data, closing incoming connection: " + errorMessage);
            eventHandler.onNonProtocolError(e);
            attemptToSendErrorGoodbyeMessage(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR, errorMessage);
            return;
        } catch (UplinkConnectionRefusedException e) {
            log.warn("Unexpected behavior: The client sent an error goodbye message instead of handshake data: " + e.getMessage());
            eventHandler.onErrorGoodbyeMessage(e.getType(), e.getRawMessage());
            return;
        }

        try {
            final MessageBlock responseData = processHandshakeDataAndGenerateResponse(handshakeData);
            sendHandshakeData(responseData);
        } catch (IOException e) {
            final String errorMarker = LogUtils.logExceptionAsSingleLineAndAssignUniqueMarker(log,
                "Error while processing or responding to handshake data, closing incoming connection", e);
            attemptToSendErrorGoodbyeMessage(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR,
                ERROR_MESSAGE_CONNECTION_SETUP_FAILED + " (internal error log marker " + errorMarker + ")");
            return;
        } catch (UplinkConnectionRefusedException e) {
            // not necessarily an error; the handshake handling has decided to gracefully refuse the connection
            // TODO log with more connection information
            log.warn("Refusing connection: " + e.toString());
            attemptToSendErrorGoodbyeMessage(e.getType(), "Connection refused: " + e.getRawMessage());
            return;
        }

        // expect handshake confirmation from the client
        try {
            handshakeData = expectHandshakeData(); // contains no data, just an empty "handshake" message block
        } catch (IOException e) {
            final String errorMessage = e.toString();
            log.info("Error while expecting handshake confirmation, closing incoming connection: " + errorMessage);
            eventHandler.onNonProtocolError(e);
            attemptToSendErrorGoodbyeMessage(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR, errorMessage);
            return;
        } catch (UplinkConnectionRefusedException e) {
            log.debug("Received an error goodbye message instead of handshake data from a client: " + e.getMessage());
            eventHandler.onErrorGoodbyeMessage(e.getType(), e.getRawMessage());
            return;
        }

        eventHandler.onHandshakeComplete();

        runMessageReceiveLoop();
    }

    private MessageBlock processHandshakeDataAndGenerateResponse(MessageBlock handshakeData)
        throws ProtocolException, UplinkConnectionRefusedException {
        if (verboseLoggingEnabled) {
            log.debug("Processing handshake data: " + new String(handshakeData.getData()));
        }
        // parse received JSON
        final Map<String, String> incomingData = messageConverter.decodeHandshakeData(handshakeData);

        Map<String, String> responseMap = new HashMap<>();
        // build response
        eventHandler.provideOrProcessHandshakeData(incomingData, responseMap);

        // encode to JSON message
        return messageConverter.encodeHandshakeData(responseMap);
    }

    @Override
    public void closeOutgoingMessageStream() {
        closeOutgoingDataStream();
    }

}
