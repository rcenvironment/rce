/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
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
import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Encapsulates common aspects of the low-level transmission protocol, comprised of the initial client-server protocol handshake,
 * bidirectional transmission of message blocks, error message handling, and connection teardown.
 * <p>
 * This class and its subclasses are supposed to be <b>as stateless as possible</b>, except for the state of the low-level connection and
 * its input/output streams (EOF, read/write error, closed, ...). All other state handling should occur in higher layers, especially the
 * Uplink session classes.
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

    // TODO (p3) 10.x/11.0: review: separate values needed?
    protected static final int HANDSHAKE_MESSAGE_TIMEOUT = UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC;

    private static final int HANDSHAKE_MESSAGE_WAIT_CHECK_INTERVAL = 100;

    protected final StreamConnectionEndpoint connectionEndpoint;

    protected final DataInputStream dataInputStream;

    protected final DataOutputStream dataOutputStream;

    protected final UplinkConnectionLowLevelEventHandler eventHandler;

    protected final ObjectMapper jsonMapper;

    protected final UplinkProtocolMessageConverter messageConverter;

    protected final String logPrefix;

    protected final boolean verboseLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("uplink.lowlevel");

    protected final Log log = LogFactory.getLog(getClass());

    public CommonUplinkLowLevelProtocolWrapper(StreamConnectionEndpoint connectionEndpoint,
        UplinkConnectionLowLevelEventHandler eventHandler, String logIdentity) {
        this.connectionEndpoint = connectionEndpoint;
        this.dataInputStream = new DataInputStream(connectionEndpoint.getInputStream());
        this.dataOutputStream = new DataOutputStream(connectionEndpoint.getOutputStream());
        this.eventHandler = eventHandler;
        this.jsonMapper = JsonUtils.getDefaultObjectMapper();
        this.messageConverter = new UplinkProtocolMessageConverter(logIdentity);
        this.logPrefix = "[" + logIdentity + "] ";
    }

    /**
     * A blocking call that performs the initial protocol handshake and then runs the message dispatch loop. Any errors are handled
     * internally or reported via callbacks.
     * 
     * TODO >10.2 (p2): pull this up to the session layer
     */
    public void runSession() {
        try {
            runHandshakeSequence();
            eventHandler.onHandshakeComplete();
            runMessageReceiveLoop();
        } catch (UplinkConnectionRefusedException e) {
            if (e.shouldAttemptToSendErrorGoodbye()) {
                log.debug(logPrefix + "Uplink handshake failed or connection refused; attempting to send error message \""
                    + e.getRawMessage() + "\"");
                attemptToSendErrorGoodbyeMessage(e.getType(), e.getRawMessage());
            } else {
                log.debug(logPrefix + "Uplink handshake failed or connection refused: " + e.getRawMessage());
            }
            eventHandler.onHandshakeFailedOrConnectionRefused(e);
        }
        // no matter the outcome, ensure that the underlying connection or low-level session is always closed.
        // this is redundant in some cases, but all implementations should be tolerant against repeated calls.
        terminateSession();
    }

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
            if (verboseLoggingEnabled) {
                log.debug(
                    StringUtils.format("%sSending a message of type %s to channel %d, payload size %d bytes",
                        logPrefix, messageBlock.getType(), channelId, data.length));
            }
            dataOutputStream.writeLong(channelId); // 8 bytes of channel id
            dataOutputStream.writeInt(data.length); // 4 bytes of size data
            dataOutputStream.writeByte(messageBlock.getType().getCode()); // 1 byte of message type
            dataOutputStream.write(data);
            dataOutputStream.flush(); // TODO potential optimization: avoid flushing after every block?
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

    protected byte[] readExpectedBytesWithTimeout(final int expectedLength, int timeoutMsec, int recheckInterval)
        throws IOException, TimeoutException {
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
                throw new TimeoutException(
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
            log.debug(logPrefix + "Sent handshake init (" + initBytes.length + " bytes)");
        }
    }

    protected void expectHandshakeInit() throws IOException, TimeoutException {
        byte[] expectedHandshakeInitBytes = readExpectedBytesWithTimeout(UplinkProtocolConstants.HANDSHAKE_INIT_STRING_BYTE_LENGTH,
            HANDSHAKE_MESSAGE_TIMEOUT, HANDSHAKE_MESSAGE_WAIT_CHECK_INTERVAL);
        final String reveivedHeader = new String(expectedHandshakeInitBytes, UplinkProtocolConstants.DEFAULT_CHARSET);
        if (!reveivedHeader.equals(UplinkProtocolConstants.HANDSHAKE_HEADER_STRING)) {
            throw new ProtocolException("Received invalid handshake init: " + reveivedHeader);
        }
        if (verboseLoggingEnabled) {
            log.debug(logPrefix + "Received expected handshake init (" + expectedHandshakeInitBytes.length + " bytes)");
        }
    }

    protected void sendHandshakeData(MessageBlock responseData) throws IOException {
        sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, responseData);
        dataOutputStream.flush();
        if (verboseLoggingEnabled) {
            log.debug(
                logPrefix + "Sent handshake data '" + new String(responseData.getData(), UplinkProtocolConstants.DEFAULT_CHARSET) + "'");
        }
    }

    protected void sendHandshakeConfirmation() throws IOException {
        sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, new MessageBlock(MessageType.HANDSHAKE));
        dataOutputStream.flush();
        if (verboseLoggingEnabled) {
            log.debug(logPrefix + "Sent handshake confirmation");
        }
    }

    protected MessageBlock expectHandshakeData() throws UplinkConnectionRefusedException, TimeoutException, IOException {
        MessageBlockWithChannelId messageBlock;
        try {
            messageBlock = readChannelIdAndMessageBlockWithTimeout(UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC);
        } catch (TimeoutException e) {
            // improve timeout message
            throw new TimeoutException("The remote side did not send their Uplink handshake response within "
                + UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC + " msec");
        } catch (IOException e) {
            // improve error message
            throw new IOException("Error receiving the remote side's handshake response: " + e.getMessage());
        }

        // check assumption (default channel)
        if (messageBlock.getChannelId() != UplinkProtocolConstants.DEFAULT_CHANNEL_ID) {
            throw new ProtocolException("Unexpected handshake channel id: " + messageBlock.getChannelId());
        }

        if (messageBlock.getType() == MessageType.GOODBYE) {
            // this should only be sent by the server side at this point, so make this a local exception
            final String errorMessage = extractGoodbyeErrorMessage(messageBlock, true);
            // (re)construct an exception from the remote error message
            // TODO instead of the two separate calls, provide a factory method?
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.typeOfWrappedErrorMessage(errorMessage),
                UplinkProtocolErrorType.unwrapErrorMessage(errorMessage), false);
        }
        if (messageBlock.getType() != MessageType.HANDSHAKE) {
            // not confidential, and the connection has not broken down -> attempt to send a reply
            throw new UplinkConnectionRefusedException(UplinkProtocolErrorType.PROTOCOL_VIOLATION,
                "Expected handshake data, but received message type " + messageBlock.getType() + " instead", true);
        }
        if (verboseLoggingEnabled) {
            if (messageBlock.getDataLength() == 0) {
                log.debug(
                    logPrefix + "Received handshake confirmation");
            } else {
                log.debug(
                    logPrefix + "Received handshake data '" + new String(messageBlock.getData(), UplinkProtocolConstants.DEFAULT_CHARSET)
                        + "'");
            }
        }
        return messageBlock;
    }

    /**
     * Blocks until the next message block could be read, or the incoming stream has been closed cleanly, or an error occurred. Any timeout
     * handling should be done by running this in a worker thread, and checking for completion from another thread.
     * 
     * @return the received message/data block if it could be read, or {@link Optional#empty()} on clean stream shutdown
     * @throws IOException on a read error
     * @throws ProtocolException if the received message block violates value constraints, e.g. an invalid message type
     */
    protected final MessageBlockWithChannelId readMessageBlock() throws IOException {
        synchronized (dataInputStream) {
            long channelId = dataInputStream.readLong();
            int blockSize = dataInputStream.readInt();
            // sanity check on announced size to detect protocol errors and prevent heap exhaustion
            if (blockSize < 0 || blockSize > UplinkProtocolConstants.MAX_MESSAGE_BLOCK_DATA_LENGTH) {
                throw new ProtocolException(StringUtils.format(
                    "Incoming message block announced a size of %d (valid range: 0-%d)", blockSize,
                    UplinkProtocolConstants.MAX_MESSAGE_BLOCK_DATA_LENGTH));
            }
            byte type = dataInputStream.readByte();
            byte[] data = new byte[blockSize];
            dataInputStream.readFully(data);
            return new MessageBlockWithChannelId(type, data, channelId);
        }
    }

    /**
     * Blocks until the next message block could be read, or the incoming stream has been closed cleanly, or an error occurred, or the given
     * timeout is reached.
     * 
     * @param timeoutMsec the timeout in msec (surprising, I know...)
     * @return the received message/data block if it could be read, or {@link Optional#empty()} on clean stream shutdown
     * @throws IOException on a read error
     * @throws TimeoutException on timeout
     * @throws ProtocolException if the received message block violates value constraints, e.g. an invalid message type
     */
    protected final MessageBlockWithChannelId readChannelIdAndMessageBlockWithTimeout(int timeoutMsec)
        throws IOException, TimeoutException {
        final CompletableFuture<MessageBlockWithChannelId> messageFuture = new CompletableFuture<>();
        ConcurrencyUtils.getAsyncTaskService().execute("Uplink: Read message block with timeout", () -> {
            try {
                messageFuture.complete(readMessageBlock());
            } catch (IOException e) {
                messageFuture.completeExceptionally(e);
            }
        });
        try {
            return messageFuture.get(timeoutMsec, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while waiting for an incoming message");
        } catch (ExecutionException e) {
            throw new IOException("Error while waiting for an incoming message: " + e.getMessage());
        }
    }

    /**
     * Ensures that the outgoing connection is closed.
     */
    public final void terminateSession() {
        connectionEndpoint.close();
    }

    public final boolean attemptToSendRegularGoodbyeMessage() {
        try {
            sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, UplinkProtocolConstants.MESSAGE_TYPE_GOODBYE, new byte[0]);
            return true;
        } catch (ProtocolException e) {
            throw new RuntimeException("Internal error: Failed to construct shutdown message", e);
        } catch (IOException e) {
            log.debug("Failed to send regular 'goodbye' message; most likely, the connection has already failed");
            return false;
        }
    }

    public final void attemptToSendErrorGoodbyeMessage(final UplinkProtocolErrorType type, final String rawMessage) {
        final String wrappedMessage = type.wrapErrorMessage(rawMessage);
        try {
            sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, messageConverter.encodeErrorGoodbyeMessage(wrappedMessage));
        } catch (IOException e) {
            log.debug(StringUtils.format("%sFailed to send a 'goodbye' error message; this is often a best-effort attempt, "
                + "so this can typically be ignored (message body: %s; error while sending: %s)", logPrefix, wrappedMessage, e.toString()));
        }
    }

    protected abstract void runHandshakeSequence() throws UplinkConnectionRefusedException;

    protected final void runMessageReceiveLoop() {
        if (verboseLoggingEnabled) {
            log.debug(logPrefix + "Running message dispatch loop");
        }
        boolean expectingFurtherMessages = true;
        while (true) {
            try {
                if (!receiveNextMessage()) {
                    expectingFurtherMessages = false;
                }
            } catch (IOException e) {
                boolean exceptionMatchesClosedConnection = e instanceof EOFException || e instanceof SocketException;
                if (exceptionMatchesClosedConnection) {
                    if (e.getClass() != EOFException.class && e.getMessage() == null) {
                        // unless this is the cleanest case possible, log the semantic mapping
                        log.debug(StringUtils.format("%sCategorizing stream read exception as 'end of stream' event: %s", logPrefix,
                            e.toString()));
                    }
                    eventHandler.onIncomingStreamClosedOrEOF();
                    break;
                } else {
                    if (expectingFurtherMessages) {
                        final String errorMarker = LogUtils.logExceptionAsSingleLineAndAssignUniqueMarker(log,
                            logPrefix + "Error while receiving a message, closing the connection", e);
                        // report unexpected errors
                        eventHandler.onStreamReadError(e);
                        attemptToSendErrorGoodbyeMessage(UplinkProtocolErrorType.INTERNAL_SERVER_ERROR,
                            "Closing the connection after an error (internal error log marker " + errorMarker + ")");
                    } else {
                        log.error(logPrefix + "Not expecting further messages, but encountered a non-EOF exception; "
                            + "still considering the stream as closed/broken as a fallback: " + e.toString());
                        eventHandler.onIncomingStreamClosedOrEOF();
                        break;
                    }
                }
            }
        }
    }

    private boolean receiveNextMessage() throws IOException {
        final MessageBlockWithChannelId message = readMessageBlock();
        long channelId = message.getChannelId();
        if (message.getType() == MessageType.GOODBYE) {
            if (message.getDataLength() == 0) {
                log.debug(logPrefix + "Received regular 'goodbye' message, expecting end of stream next");
                eventHandler.onRegularGoodbyeMessage();
            } else {
                log.debug(logPrefix + "Received error 'goodbye' message, expecting end of stream next");
                String errorMessage = extractGoodbyeErrorMessage(message, false);
                eventHandler.onErrorGoodbyeMessage(UplinkProtocolErrorType.typeOfWrappedErrorMessage(errorMessage),
                    UplinkProtocolErrorType.unwrapErrorMessage(errorMessage));
            }
            return false; // continue, but do not expect further messages
        }
        if (verboseLoggingEnabled) {
            log.debug(
                StringUtils.format("%sReceived message of type %s for channel %d, payload size %d bytes", logPrefix, message.getType(),
                    channelId, message.getDataLength()));
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

}
