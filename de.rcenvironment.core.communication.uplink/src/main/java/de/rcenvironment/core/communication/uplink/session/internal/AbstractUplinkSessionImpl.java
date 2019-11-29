/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.session.internal;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.network.internal.CommonUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSession;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSessionState;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * Common base class for {@link UplinkSession} implementations.
 * <p>
 * Threading behavior of all subclasses:
 * <ul>
 * <li>Incoming messages are supposed to be read in a blocking loop within {@link #runSession()}, and then queued into an internal
 * {@link AsyncOrderedExecutionQueue} which calls {@link #processIncomingMessageBlock(MessageBlock)}, which in turn calls
 * synchronous/blocking channel methods.
 * <li>All outgoing messages are supposed to be enqueued into an internal {@link AsyncOrderedExecutionQueue}, which dispatches them to the
 * synchronous/blocking {@link CommonUplinkLowLevelProtocolWrapper} methods.
 * </ul>
 *
 * @author Robert Mischke
 */
public abstract class AbstractUplinkSessionImpl implements UplinkSession {

    // the maximum time to wait for the namespace id's Future; not intended for actual waiting, but only to prevent minor race conditions
    private static final int VERY_SHORT_WAIT_MSEC = 50;

    /**
     * The maximum number of unsent messages to queue; especially important during file uploads.
     */
    private static final int OUTGOING_MESSAGE_QUEUE_SIZE = 5; // TODO arbitrary; adjust as necessary

    private static final boolean DEBUG_OUTPUT_ENABLED = DebugSettings.getVerboseLoggingEnabled("uplink.sessions");

    protected final AsyncOrderedExecutionQueue incomingMessageQueue;

    protected final AsyncOrderedExecutionQueue outgoingMessageQueue;

    protected final Log log = LogFactory.getLog(getClass());

    private UplinkSessionState state = UplinkSessionState.INITIAL; // synchronized on "this"

    private String logDescriptor;

    private final CompletableFuture<String> assignedNamespaceIdFuture = new CompletableFuture<String>();

    private final Semaphore outgoingMessageQueueLimit = new Semaphore(OUTGOING_MESSAGE_QUEUE_SIZE);

    protected AbstractUplinkSessionImpl(ConcurrencyUtilsFactory concurrencyUtilsFactory) {
        Objects.requireNonNull(concurrencyUtilsFactory);
        this.incomingMessageQueue =
            concurrencyUtilsFactory.createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
        this.outgoingMessageQueue =
            concurrencyUtilsFactory.createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
    }

    @Override
    public synchronized UplinkSessionState getState() {
        return state;
    }

    @Override
    public final void enqueueMessageBlockForSending(long channelId, MessageBlock messageBlock) throws IOException {
        try {
            outgoingMessageQueueLimit.acquire();
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting to enqueue a message of type " + messageBlock.getType());
        }
        if (DEBUG_OUTPUT_ENABLED) {
            log.debug(StringUtils.format("Enqueueing message of type %s for sending to channel %d, payload size %d bytes",
                messageBlock.getType(), channelId, messageBlock.getDataLength()));
        }
        outgoingMessageQueue.enqueue(() -> {
            try {
                getProtocolWrapper().sendMessageBlock(channelId, messageBlock);
            } catch (IOException e) {
                // TODO close the session
                log.error("Error during asynchronous sending of message with type " + messageBlock.getType());
            } finally {
                outgoingMessageQueueLimit.release();
            }
        });
    }

    @Override
    public final synchronized boolean isActive() {
        return state == UplinkSessionState.ACTIVE;
    }

    @Override
    public final String getAssignedNamespaceId() {
        final String currentValue = assignedNamespaceIdFuture.getNow(null);
        if (currentValue != null) {
            return currentValue;
        } else {
            throw new IllegalStateException("Namespace id requested before it was available");
        }
    }

    @Override
    public final Optional<String> getAssignedNamespaceIdIfAvailable() {
        return Optional.ofNullable(assignedNamespaceIdFuture.getNow(null));
    }

    @Override
    public final String getLogDescriptor() {
        return logDescriptor;
    }

    @Override
    public final String getDestinationIdPrefix() {
        return getAssignedNamespaceId();
    }

    @Override
    public final String toString() {
        return logDescriptor;
    }

    protected final synchronized void setSessionState(UplinkSessionState newState) {
        switch (newState) {
        case PARTIALLY_CLOSED_BY_LOCAL:
        case PARTIALLY_CLOSED_BY_REMOTE:
        case CLIENT_HANDSHAKE_REQUEST_READY:
        case SERVER_HANDSHAKE_RESPONSE_READY:
            throw new IllegalArgumentException(
                "The state " + newState + " is managed by the base class and should not be set explicitly; "
                    + "use the \"mark...\" methods to reach it");
        default:
            setStateInternal(newState);
        }
    }

    protected final synchronized void markClientHandshakeSentOrReceived() {
        if (state != UplinkSessionState.INITIAL) {
            log.debug("Ignoring client handshake event as the session's state is " + state);
            return;
        }
        setStateInternal(UplinkSessionState.CLIENT_HANDSHAKE_REQUEST_READY);
    }

    protected final synchronized void markServerHandshakeSentOrReceived() {
        if (state != UplinkSessionState.CLIENT_HANDSHAKE_REQUEST_READY) {
            log.debug("Ignoring server handshake event as the session's state is " + state);
            return;
        }
        setStateInternal(UplinkSessionState.SERVER_HANDSHAKE_RESPONSE_READY);
    }

    protected final synchronized void markAsCloseRequestedLocally() {
        if (state == UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR) {
            // already in terminal state; do not enter "closing" state
            return;
        }
        if (state == UplinkSessionState.PARTIALLY_CLOSED_BY_REMOTE) {
            setStateInternal(UplinkSessionState.FULLY_CLOSED);
        } else if (state == UplinkSessionState.PARTIALLY_CLOSED_BY_LOCAL || state == UplinkSessionState.FULLY_CLOSED) {
            log.debug("Ignoring redundant request to mark session " + getLogDescriptor()
                + " as closed by a local event as its state is already " + state);
        } else {
            setStateInternal(UplinkSessionState.PARTIALLY_CLOSED_BY_LOCAL);
        }
    }

    @Override
    public synchronized void markAsCloseRequestedByRemoteEvent() {
        if (state == UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR) {
            // already in terminal state; do not enter "closing" state
            return;
        }
        if (state == UplinkSessionState.PARTIALLY_CLOSED_BY_LOCAL) {
            setStateInternal(UplinkSessionState.FULLY_CLOSED);
        } else if (state == UplinkSessionState.PARTIALLY_CLOSED_BY_REMOTE) {
            log.warn("Ignoring redundant request to mark session " + getLogDescriptor()
                + " as closed by a remote event as its state is already " + state);
        } else {
            setStateInternal(UplinkSessionState.PARTIALLY_CLOSED_BY_REMOTE);
        }
    }

    private void setStateInternal(UplinkSessionState newState) {
        UplinkSessionState oldState = state;
        if (newState == oldState) {
            log.warn("Ignoring redundant request to set the state of session " + getLogDescriptor() + " to " + newState);
            return;
        }
        // TODO (p2) consider moving this to verbose logging
        log.debug("State of session " + getLogDescriptor() + " is changing from " + oldState + " to " + newState);
        this.state = newState;
        onSessionStateChanged(oldState, newState);
    }

    /**
     * Hook method for subclasses to react on state change events in a central place. Only called when the state actually changes, ie old
     * and new state will never be equal. Not called for entering the initial state.
     * 
     * @param oldState the previous state
     * @param newState the new (already set) state
     */
    protected abstract void onSessionStateChanged(UplinkSessionState oldState, UplinkSessionState newState);

    protected abstract CommonUplinkLowLevelProtocolWrapper getProtocolWrapper();

    protected final void setAssignedNamespaceId(String serverAssignedNamespaceId) {
        assignedNamespaceIdFuture.complete(serverAssignedNamespaceId);
    }

    protected final void updateLogDescriptor() {
        final String namespaceIdOrPlaceholder = assignedNamespaceIdFuture.getNow("<no namespace yet>");
        this.logDescriptor = StringUtils.format("%s [%s]", getLocalSessionId(), namespaceIdOrPlaceholder);
    }

}
