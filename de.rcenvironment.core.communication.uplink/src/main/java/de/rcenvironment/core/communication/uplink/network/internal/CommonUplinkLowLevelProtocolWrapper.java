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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.common.internal.UplinkProtocolMessageConverter;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Encapsulates common aspects of the low-level transmission protocol, comprised of the initial client-server protocol handshake,
 * bidirectional transmission of message blocks, error message handling, and session teardown for an uplink connection.
 * <p>
 * Threading behavior: All operations of this class (and its known subclasses) are <em>blocking</em>.
 * <ul>
 * <li>Incoming messages are dispatched by a single loop run from {@link #runSession()}
 * <li>All provided message sending methods are blocking and internally synchronized.
 * </ul>
 * All asynchronous behavior is supposed to happen in the session classes layered above these protocol wrappers.
 * 
 * @author Robert Mischke
 */
public abstract class CommonUplinkLowLevelProtocolWrapper {

    // TODO (p3) review: separate values needed?
    private static final int HANDSHAKE_MESSAGE_TIMEOUT = UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC;

    private static final int HANDSHAKE_MESSAGE_WAIT_CHECK_INTERVAL = 100;

    protected DataInputStream dataInputStream;

    protected DataOutputStream dataOutputStream;

    protected final UplinkConnectionLowLevelEventHandler eventHandler;

    protected final ObjectMapper jsonMapper;

    protected final UplinkProtocolMessageConverter messageConverter;

    protected final boolean verboseLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("uplink.lowlevel");

    protected final Log log = LogFactory.getLog(getClass());

    private boolean outgoingStreamClosed;

    public CommonUplinkLowLevelProtocolWrapper(UplinkConnectionLowLevelEventHandler eventHandler, String logIdentity) {
        this.eventHandler = eventHandler;
        this.jsonMapper = JsonUtils.getDefaultObjectMapper();
        this.messageConverter = new UplinkProtocolMessageConverter(logIdentity);
    }

    /**
     * A blocking call that performs the initial protocol handshake and then runs the message dispatch loop.
     * 
     * @throws IOException on I/O exceptions, e.g. a breakdown of the underlying connection
     * @throws ProtocolException on unexpected protocol behavior, e.g. a protocol version mismatch with the remote side, or malformed
     *         traffic
     */
    public abstract void runSession() throws IOException;

    /**
     * Transmits the given message block to the other end of the connection.
     * 
     * @param channelId the id of the virtual channel to send this {@link MessageBlock} to
     * @param messageBlock the message block block to send
     * @throws IOException on failure to send, typically because the connection was closed or has broken down in the meantime
     */
    public final void sendMessageBlock(long channelId, MessageBlock messageBlock) throws IOException {
        final byte[] data = messageBlock.getData();
        synchronized (dataOutputStream) {
            if (outgoingStreamClosed) {
                log.debug("Ignoring message send request as the connection has been shut down");
                return;
            }
            if (verboseLoggingEnabled) {
                log.debug(
                    StringUtils.format("Sending a message of type %s to channel %d, payload size %d bytes",
                        messageBlock.getType(), channelId, data.length));
            }
            dataOutputStream.writeLong(channelId); // 8 bytes of channel id
            dataOutputStream.writeInt(data.length); // 4 bytes of size data
            dataOutputStream.writeByte(messageBlock.getType().getCode()); // 1 byte of message type
            dataOutputStream.write(data);
            dataOutputStream.flush();
        }
    }

    /**
     * Transmits a message block created with the given parameters to the other end of the connection.
     * 
     * @param channelId the id of the channel to route this message to
     * @param type the message type to send
     * @param data the message data to send
     * @throws IOException on failure to send, typically because the connection was closed or has broken down in the meantime
     * @throws ProtocolException if the parameters are not valid for creating a message block
     */
    public final void sendMessageBlock(long channelId, int type, byte[] data) throws IOException {
        sendMessageBlock(channelId, new MessageBlock(type, data));
    }

    /**
     * Makes a best-effort attempt to send a goodbye message, and then closes the session and its underlying connection.
     */
    public abstract void closeOutgoingMessageStream();

    protected byte[] readExpectedBytesWithTimeout(final int expectedLength, int timeoutMsec, int recheckInterval)
        throws IOException {
        byte[] expectedBytes = new byte[expectedLength];
        long startTime = System.currentTimeMillis();
        while (dataInputStream.available() < expectedLength) {
            if (System.currentTimeMillis() < startTime + timeoutMsec) {
                try {
                    Thread.sleep(recheckInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ProtocolException("Interrupted while waiting for " + expectedLength + " bytes of data");
                }
            } else {
                throw new ProtocolException(
                    "Expected " + expectedLength + " bytes of data, but did not receive them within " + timeoutMsec + " msec");
            }
        }
        dataInputStream.readFully(expectedBytes);
        return expectedBytes;
    }

    protected void sendHandshakeInit() throws IOException {
        final byte[] initBytes = UplinkProtocolConstants.HANDSHAKE_HEADER_STRING.getBytes(UplinkProtocolConstants.DEFAULT_CHARSET);
        if (initBytes.length != UplinkProtocolConstants.HANDSHAKE_INIT_STRING_BYTE_LENGTH) {
            throw new ProtocolException("Handshake array length does not match the expected byte count");
        }
        dataOutputStream.write(initBytes);
        if (verboseLoggingEnabled) {
            log.debug("Sent handshake init (" + initBytes.length + " bytes)");
        }
    }

    protected void expectHandshakeInit() throws IOException {
        byte[] expectedHandshakeInitBytes = readExpectedBytesWithTimeout(UplinkProtocolConstants.HANDSHAKE_INIT_STRING_BYTE_LENGTH,
            HANDSHAKE_MESSAGE_TIMEOUT, HANDSHAKE_MESSAGE_WAIT_CHECK_INTERVAL);
        final String reveivedHeader = new String(expectedHandshakeInitBytes, UplinkProtocolConstants.DEFAULT_CHARSET);
        if (!reveivedHeader.equals(UplinkProtocolConstants.HANDSHAKE_HEADER_STRING)) {
            throw new ProtocolException("Received invalid handshake init: " + reveivedHeader);
        }
        if (verboseLoggingEnabled) {
            log.debug("Received expected handshake init (" + expectedHandshakeInitBytes.length + " bytes)");
        }
    }

    protected void sendHandshakeData(MessageBlock responseData) throws IOException {
        sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, responseData);
        dataOutputStream.flush();
        if (verboseLoggingEnabled) {
            log.debug("Sent handshake data");
        }
    }

    protected void sendHandshakeConfirmation() throws IOException {
        sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, new MessageBlock(MessageType.HANDSHAKE));
        dataOutputStream.flush();
        if (verboseLoggingEnabled) {
            log.debug("Sent handshake confirmation");
        }
    }

    protected MessageBlock expectHandshakeData() throws IOException, UplinkConnectionRefusedException {
        final long channelId = readChannelId(); // TODO could be omitted; left in for regularity for now
        if (channelId != UplinkProtocolConstants.DEFAULT_CHANNEL_ID) {
            throw new ProtocolException("Unexpected handshake channel id: " + channelId);
        }
        MessageBlock messageBlock = readMessageBlockWithTimeout(UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC);

        if (messageBlock.getType() == MessageType.GOODBYE) {
            // note: this error message may be slightly confusing if this is actually sent by a client;
            // as this should not normally happen, this is not handled separately
            final String errorMessage = extractGoodbyeErrorMessage(messageBlock, true);
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.typeOfWrappedErrorMessage(errorMessage),
                UplinkProtocolErrorType.unwrapErrorMessage(errorMessage));
        }
        if (messageBlock.getType() != MessageType.HANDSHAKE) {
            throw new ProtocolException(
                "Expected handshake data, but received message type " + messageBlock.getType() + " instead");
        }
        if (verboseLoggingEnabled) {
            log.debug(
                "Received handshake data: " + new String(messageBlock.getData(), UplinkProtocolConstants.DEFAULT_CHARSET));
        }
        return messageBlock;
    }

    protected final long readChannelId() throws IOException {
        synchronized (dataInputStream) {
            return dataInputStream.readLong();
        }
    }

    /**
     * Blocks until the next message block could be read, or the incoming stream has been closed cleanly, or an error occurred.
     * 
     * @return the received message/data block if it could be read, or {@link Optional#empty()} on clean stream shutdown
     * @throws IOException on a read error
     * @throws ProtocolException if the received message block violates value constraints, e.g. an invalid message type
     */
    protected final MessageBlock readMessageBlock() throws IOException {
        synchronized (dataInputStream) {
            int blockSize = dataInputStream.readInt();
            // sanity check on announced size to detect protocol errors and prevent heap exhaustion
            if (blockSize < 0 || blockSize > UplinkProtocolConstants.MAX_MESSAGE_BLOCK_DATA_LENGTH) {
                throw new ProtocolException(StringUtils.format(
                    "Incoming message block announced a size of %d (valid range: 0-%d)", blockSize,
                    UplinkProtocolConstants.MAX_MESSAGE_BLOCK_DATA_LENGTH));
            }
            byte type = dataInputStream.readByte();
            // TODO timeout handling
            byte[] data = new byte[blockSize];
            dataInputStream.readFully(data);
            return new MessageBlock(type, data);
        }
    }

    /**
     * Blocks until the next message block could be read, or the incoming stream has been closed cleanly, or an error occurred, or the given
     * timeout is reached.
     * 
     * @param timeoutMsec the timeout in msec (surprising, I know...)
     * @return the received message/data block if it could be read, or {@link Optional#empty()} on clean stream shutdown
     * @throws IOException on a read error
     * @throws ProtocolException if the received message block violates value constraints, e.g. an invalid message type
     */
    protected final MessageBlock readMessageBlockWithTimeout(int timeoutMsec) throws IOException {
        final CompletableFuture<MessageBlock> messageFuture = new CompletableFuture<>();

        ConcurrencyUtils.getAsyncTaskService().execute("Uplink: Read message block with timeout", () -> {
            try {
                messageFuture.complete(readMessageBlock());
            } catch (IOException e) {
                messageFuture.completeExceptionally(e);
            }
        });
        try {
            return messageFuture.get(timeoutMsec, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IOException("Error while waiting for an incoming message: " + e.toString());
        }
    }

    /**
     * Sends a final shutdown message into the outgoing stream and then closes it.
     * 
     * @throws IOException on failure to send the shutdown message
     */
    protected final void closeOutgoingDataStream() {
        synchronized (dataOutputStream) {
            if (outgoingStreamClosed) {
                return; // ignore duplicate calls
            }
            try {
                sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, UplinkProtocolConstants.MESSAGE_TYPE_GOODBYE, new byte[0]);
            } catch (ProtocolException e) {
                throw new RuntimeException("Internal error: Failed to construct shutdown message", e);
            } catch (IOException e) {
                log.debug("Failed to send goodbye message; most likely, the connection has already failed");
            }
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                log.debug("Failed to actively close the output stream; most likely, the connection has already failed");
            }
            outgoingStreamClosed = true;
        }
    }

    protected final void runMessageReceiveLoop() {
        if (verboseLoggingEnabled) {
            log.debug("Running message dispatch loop");
        }
        boolean proceed = true;
        while (proceed) {
            try {
                if (!receiveNextMessage()) {
                    proceed = false;
                }
            } catch (IOException e) {
                final String errorMarker = LogUtils.logExceptionAsSingleLineAndAssignUniqueMarker(log,
                    "Error while receiving a message, closing the connection", e);
                // note: not integrated with the client-side "only report first error" mechanism; this should be ok, though
                eventHandler.onNonProtocolError(e);
                attemptToSendErrorGoodbyeMessage(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR,
                    "Closing the connection after an error (internal error log marker " + errorMarker + ")");
                proceed = false;
            }
        }
    }

    private boolean receiveNextMessage() throws IOException {
        long channelId = readChannelId();
        final MessageBlock message = readMessageBlock();
        if (message.getType() == MessageType.GOODBYE) {
            log.debug("Received 'goodbye' message, stopping message listener");
            if (message.getDataLength() == 0) {
                eventHandler.onRegularGoodbyeMessage();
            } else {
                String errorMessage = extractGoodbyeErrorMessage(message, false);
                // note: not integrated with the client-side "only report first error" mechanism; this should be ok, though
                eventHandler.onErrorGoodbyeMessage(UplinkProtocolErrorType.typeOfWrappedErrorMessage(errorMessage),
                    UplinkProtocolErrorType.unwrapErrorMessage(errorMessage));
            }
            return false; // do not continue
        }
        if (verboseLoggingEnabled) {
            log.debug(
                StringUtils.format("Received message of type %s for channel %d, payload size %d bytes", message.getType(), channelId,
                    message.getDataLength()));
        }
        eventHandler.onMessageBlock(channelId, message);
        return true; // continue
    }

    private String extractGoodbyeErrorMessage(final MessageBlock message, boolean replaceEmptyOrMissingMessage) {
        if (message.getDataLength() == 0 && replaceEmptyOrMissingMessage) {
            return "E99: <no error message available>";
        }
        String errorMessage;
        try {
            errorMessage = new String(message.getData());
        } catch (RuntimeException e) {
            // TODO if this ever occurs in practice, consider logging the raw byte array, too
            errorMessage = "Failed to decode the error string attached to a 'goodbye' message; "
                + "byte length: " + message.getDataLength();
        }
        return errorMessage;
    }

    protected void attemptToSendErrorGoodbyeMessage(final UplinkProtocolErrorType type, final String rawMessage) {
        final String wrappedMessage = type.wrapErrorMessage(rawMessage);
        try {
            sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, messageConverter.encodeErrorGoodbyeMessage(wrappedMessage));
        } catch (IOException e) {
            log.debug(StringUtils.format("Failed to send a 'goodbye' error message; this is often a best-effort attempt, "
                + "so this can typically be ignored (message body: %s; error while sending: %s)", wrappedMessage, e.toString()));
        }
    }

    protected boolean isOutgoingStreamClosed() {
        return outgoingStreamClosed;
    }

}
