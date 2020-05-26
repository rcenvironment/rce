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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Encapsulates client-side handshake handling, low-level message sending and receiving, and session teardown for an uplink connection.
 *
 * @author Robert Mischke
 */
public class ClientSideUplinkLowLevelProtocolWrapper extends CommonUplinkLowLevelProtocolWrapper {

    private final UplinkConnection connection;

    private CompletableFuture<MessageBlock> handshakeResponseFuture;

    private boolean connectionClosedWithError; // used to prevent duplicate error events sent to the caller; synchronized on "this"

    /**
     * @param connection a pluggable abstraction for the bidirectional data streams to send low-level protocol data over; note that this
     *        protocol wrapper does *not* manage the underlying connection's life cycle!
     * @param eventHandler the callback interface for incoming events, including received {@link MessageBlock}s
     */
    public ClientSideUplinkLowLevelProtocolWrapper(UplinkConnection connection, UplinkConnectionLowLevelEventHandler eventHandler) {
        super(eventHandler, "client session protocol wrapper");
        this.connection = connection;
    }

    @Override
    public void runSession() {

        handshakeResponseFuture = new CompletableFuture<>();

        try {
            this.dataOutputStream = new DataOutputStream(connection.open(this::onIncomingStreamAvailable, this::onRemoteErrorMessage));

            sendHandshakeInit();
            sendHandshakeData(generateHandshakeData());
            MessageBlock responseMessageBlock;
            try {
                responseMessageBlock = awaitHandshakeResponseDataFromInputThread(UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC);
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error while waiting for the server's handshake response: " + e.toString());
            } catch (TimeoutException e) {
                throw new IOException("The server did not send a handshake response within "
                    + UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC + " msec");
            }
            processHandshakeResponse(responseMessageBlock);
            
            sendHandshakeConfirmation();

            eventHandler.onHandshakeComplete();

            runMessageReceiveLoop();
        } catch (IOException e) {
            if (registerAsFirstCriticalError()) {
                eventHandler.onNonProtocolError(e);
            }
        }

    }

    @Override
    public void closeOutgoingMessageStream() {
        closeOutgoingDataStream();
    }

    private void onIncomingStreamAvailable(InputStream incomingStream) {
        try {
            dataInputStream = new DataInputStream(incomingStream);
            expectHandshakeInit();
            final MessageBlock response = expectHandshakeData();
            handshakeResponseFuture.complete(response);
        } catch (IOException e) {
            if (registerAsFirstCriticalError()) {
                eventHandler.onNonProtocolError(e);
            }
        } catch (UplinkConnectionRefusedException e) {
            if (registerAsFirstCriticalError()) {
                eventHandler.onErrorGoodbyeMessage(e.getType(), e.getRawMessage());
            }
        }
    }

    private void onRemoteErrorMessage(String errorMessage) {
        log.warn("Uplink connection error: " + errorMessage);
    }

    private MessageBlock awaitHandshakeResponseDataFromInputThread(long timeoutMsec)
        throws InterruptedException, TimeoutException, ExecutionException {
        MessageBlock responseBytes = handshakeResponseFuture.get(timeoutMsec, TimeUnit.MILLISECONDS);
        handshakeResponseFuture = null;
        return responseBytes;
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
