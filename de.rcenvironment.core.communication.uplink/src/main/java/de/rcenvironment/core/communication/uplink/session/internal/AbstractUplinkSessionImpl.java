/*
 * Copyright 2019-2021 DLR, Germany
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

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.network.internal.CommonUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkConnectionRefusedException;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolErrorType;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSession;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSessionState;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
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

    protected static final String AUDIT_LOG_KEY_SESSION_ID = "session_id";

    protected static final String AUDIT_LOG_KEY_ORIGINAL_LOGIN_NAME = "login_name";

    protected static final String AUDIT_LOG_KEY_EFFECTIVE_LOGIN_NAME = "login_name";

    protected static final String AUDIT_LOG_KEY_EFFECTIVE_CLIENT_ID = "client_id";

    protected static final String AUDIT_LOG_KEY_NAMESPACE = "namespace";

    /**
     * Audit log key for the session's context, e.g. the underlying connection id matching the connection's log entries for association.
     */
    protected static final String AUDIT_LOG_KEY_CONNECTION_ID = "connection_id";

    protected static final String AUDIT_LOG_KEY_PROTOCOL_VERSION = "protocol";

    protected static final String AUDIT_LOG_KEY_CLIENT_VERSION_INFO = "client_version";

    protected static final String AUDIT_LOG_KEY_REASON = "reason";

    protected static final String AUDIT_LOG_KEY_FINAL_STATE = "final_state";

    // the maximum time to wait for the namespace id's Future; not intended for actual waiting, but only to prevent minor race conditions
    private static final int VERY_SHORT_WAIT_MSEC = 50;

    /**
     * The duration to wait for a response after sending a goodbye message; the outgoing stream is closed when receiving the confirmation,
     * or when this timeout is reached.
     */
    private static final int GOODBYE_CONFIRMATION_WAIT_TIMEOUT_MSEC = 10 * 1000;

    /**
     * The maximum number of unsent messages to queue; especially important during file uploads.
     */
    private static final int OUTGOING_MESSAGE_QUEUE_SIZE = 10; // TODO arbitrary; adjust as necessary

    private static final String LOG_SLASH = "/";

    private static final boolean DEBUG_OUTPUT_ENABLED = DebugSettings.getVerboseLoggingEnabled("uplink.sessions");

    protected final AsyncOrderedExecutionQueue incomingMessageQueue;

    protected final AsyncOrderedExecutionQueue outgoingMessageQueue;

    protected final UplinkSessionStateHolder sessionState = new UplinkSessionStateHolder();

    // used without synchronization beyond the constructor, as it is used very often, and there is virtually no impact from race conditions
    protected String logPrefix;

    protected final Log log = LogFactory.getLog(getClass());

    private final Semaphore outgoingMessageQueueLimit = new Semaphore(OUTGOING_MESSAGE_QUEUE_SIZE);

    private final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();

    private Optional<Runnable> onSessionShutdownFinishedRunner = Optional.empty();

    /**
     * Encapsulates the protocol-level state of this session and its underlying connection. Also serves to keep the synchronization boundary
     * manageable - all direct access to this class' fields must be synchronized on the instance, and all state-related methods must be
     * inherently synchronized.
     *
     * @author Robert Mischke
     */
    protected final class UplinkSessionStateHolder {

        private static final String STRING_TO = " to ";

        private UplinkSessionState mainState = UplinkSessionState.INITIAL; // synchronized on "this"

        private final CompletableFuture<String> assignedNamespaceIdFuture = new CompletableFuture<String>();

        private boolean incomingStreamClosedOrEOF;

        private boolean outgoingStreamClosed;

        private boolean shuttingDown;

        // volatile instead of synchronized, as it is a likely candidate to be fetched externally (see issue 17448);
        // there is a tiny performance overhead from this, but this is currently completely irrelevant -- misc_ro
        private volatile String logDescriptor;

        private boolean remoteSideHasSentGoodbye;

        private boolean ownGoodbyeSent;

        private boolean handshakeFailed;

        private boolean namespaceIdReleased;

        private String protocolVersion;

        private String clientVersionInfo;

        private boolean heartbeatSendingEnabled;

        private String effectiveAccountName;

        private String effectiveSessionQualifier;

        private long lastHeartbeatSentTime;

        private boolean pendingHeartbeatExchange;

        public synchronized void markClientHandshakeSentOrReceived() {
            if (getMainState() != UplinkSessionState.INITIAL) {
                // TODO make this an error?
                log.debug("Ignoring client handshake event as the session's state is " + getMainState());
                return;
            }
            setMainStateInternal(UplinkSessionState.CLIENT_HANDSHAKE_REQUEST_READY);
        }

        public synchronized void markServerHandshakeSentOrReceived() {
            if (getMainState() != UplinkSessionState.CLIENT_HANDSHAKE_REQUEST_READY) {
                // TODO make this an error?
                log.debug("Ignoring server handshake event as the session's state is " + getMainState());
                return;
            }
            setMainStateInternal(UplinkSessionState.SERVER_HANDSHAKE_RESPONSE_READY);
        }

        public void markHandshakeSuccessful() {
            setMainStateInternal(UplinkSessionState.ACTIVE);
        }

        public void markHandshakeFailed() {
            shuttingDown = true;
            if (!handshakeFailed) {
                handshakeFailed = true;
                setMainStateInternal(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR);
            }
        }

        public synchronized void markRemoteSideHasSentGoodbye() {
            remoteSideHasSentGoodbye = true;
            switch (mainState) {
            case ACTIVE:
                setMainStateInternal(UplinkSessionState.GOODBYE_HANDSHAKE);
                break;
            case GOODBYE_HANDSHAKE:
                if (!ownGoodbyeSent) { // consistency check
                    throw new IllegalStateException();
                }
                setMainStateInternal(UplinkSessionState.GOODBYE_HANDSHAKE_COMPLETE);
                break;
            default:
                log.debug("Received 'goodbye' message in non-standard state " + mainState);
                break;
            }
        }

        public synchronized boolean getRemoteSideHasSentGoodbye() {
            return remoteSideHasSentGoodbye;
        }

        public synchronized void markOwnGoodbyeSent() {
            ownGoodbyeSent = true;
            switch (mainState) {
            case ACTIVE:
                setMainStateInternal(UplinkSessionState.GOODBYE_HANDSHAKE);
                break;
            case GOODBYE_HANDSHAKE:
                if (!remoteSideHasSentGoodbye) { // consistency check
                    throw new IllegalStateException();
                }
                setMainStateInternal(UplinkSessionState.GOODBYE_HANDSHAKE_COMPLETE);
                break;
            case INITIAL:
            case CLIENT_HANDSHAKE_REQUEST_READY:
            case SERVER_HANDSHAKE_RESPONSE_READY:
            case UNCLEAN_SHUTDOWN_INITIATED:
            case UNCLEAN_SHUTDOWN:
                // unusual, but valid states to send a 'goodbye' from
                // TODO verify that this can never be sent before the handshake init
                log.debug(logPrefix + "Marking own 'goodbye' as sent from unusual state " + mainState);
                return;
            default:
                log.warn(logPrefix + "Marking own 'goodbye' as sent from unexpected state " + mainState);
            }
        }

        public synchronized boolean getOwnGoodbyeSent() {
            return ownGoodbyeSent;
        }

        public synchronized void markIncomingStreamClosedOrEOF() {
            if (incomingStreamClosedOrEOF) {
                throw new IllegalStateException("Redundant call");
            }
            incomingStreamClosedOrEOF = true;

            if (mainState == UplinkSessionState.UNCLEAN_SHUTDOWN_INITIATED) {
                log.debug(logPrefix
                    + "Incoming stream closed after closing the local end of the stream; considering unclean shutdown complete");
                setMainStateInternal(UplinkSessionState.UNCLEAN_SHUTDOWN);
                return;
            }

            if (ownGoodbyeSent) {
                if (remoteSideHasSentGoodbye) {
                    setMainStateInternal(UplinkSessionState.CLEAN_SHUTDOWN);
                } else {
                    log.debug(logPrefix
                        + "Stream closed before the remote side sent 'goodbye'; the remote side may be using an outdated client");
                    initiateUncleanShutdownIfStillRunning();
                }
            } else {
                if (remoteSideHasSentGoodbye) {
                    log.debug(logPrefix
                        + "Stream closed after a remote 'goodbye' before this side could send its confirmation; "
                        + "the remote side may be using an outdated client");
                    initiateUncleanShutdownIfStillRunning();
                } else {
                    log.debug(logPrefix
                        + "Unexpected end of Uplink stream; either the remote side has abruptly closed the connection, "
                        + "or the network connection has been interrupted");
                    initiateUncleanShutdownIfStillRunning();
                }
            }
        }

        /**
         * Note: This method call does not trigger any action; should be followed up with
         * {@link AbstractUplinkSessionImpl#initiateUncleanShutdownIfStillRunning()}.
         */
        public synchronized void markFatalError() {
            log.debug(logPrefix
                + "Encountered a fatal error, terminating the session");
        }

        /**
         * Note: This method call does not trigger any action; should be followed up with
         * {@link AbstractUplinkSessionImpl#initiateUncleanShutdownIfStillRunning()}.
         */
        public synchronized void markOutgoingStreamWriteError() {
            log.debug(logPrefix
                + "Failed to write to Uplink stream - "
                + "most likely, the underlying network connection has been interrupted; terminating the session");
        }

        public synchronized void markOutgoingStreamClosed() {
            if (outgoingStreamClosed) {
                return;
            }
            log.debug(logPrefix + "Closed local end of stream");
            outgoingStreamClosed = true;
            if (mainState == UplinkSessionState.GOODBYE_HANDSHAKE || mainState == UplinkSessionState.ACTIVE) {
                if (ownGoodbyeSent && remoteSideHasSentGoodbye) {
                    if (mainState == UplinkSessionState.ACTIVE) {
                        log.warn(logPrefix + "Unexpected transition: " + mainState + "->" + UplinkSessionState.CLEAN_SHUTDOWN);
                    }
                    setMainStateInternal(UplinkSessionState.CLEAN_SHUTDOWN);
                } else {
                    setMainStateInternal(UplinkSessionState.UNCLEAN_SHUTDOWN);
                }
            }
        }

        public synchronized boolean isOutgoingStreamClosed() {
            return outgoingStreamClosed;
        }

        public synchronized void setShuttingDown() {
            shuttingDown = true;
        }

        public synchronized boolean isShuttingDownOrShutDown() {
            return shuttingDown;
        }

        public synchronized void setAssignedNamespaceId(String serverAssignedNamespaceId) {
            assignedNamespaceIdFuture.complete(serverAssignedNamespaceId);
        }

        public synchronized String getAssignedNamespaceId() {
            final String currentValue = assignedNamespaceIdFuture.getNow(null);
            if (currentValue != null) {
                return currentValue;
            } else {
                throw new IllegalStateException("Namespace id requested before it was available");
            }
        }

        public synchronized Optional<String> getAssignedNamespaceIdIfAvailable() {
            // note: still returning the value if the id has been released; use #isNamespaceIdReleased() to test this
            return Optional.ofNullable(assignedNamespaceIdFuture.getNow(null));
        }

        public synchronized String getAssignedNamespaceIdIfAvailable(String fallback) {
            // note: still returning the value if the id has been released; use #isNamespaceIdReleased() to test this
            return assignedNamespaceIdFuture.getNow(fallback);
        }

        public synchronized void setNamespaceIdReleased() {
            namespaceIdReleased = true;
        }

        public boolean isNamespaceIdReleased() {
            return namespaceIdReleased;
        }

        public synchronized void updateLogDescriptor() {
            final String namespaceIdOrPlaceholder = sessionState.getAssignedNamespaceIdIfAvailable("<no namespace>");
            final String releasedSuffix;
            if (namespaceIdReleased) {
                releasedSuffix = "(released)";
            } else {
                releasedSuffix = "";
            }
            logDescriptor = StringUtils.format("%s/%s%s", getLocalSessionId(), namespaceIdOrPlaceholder, releasedSuffix);
            // note: updating this without synchronization, as it is used often, and race conditions are irrelevant
            logPrefix = "[" + logDescriptor + "] "; // TODO preliminary
        }

        private void setMainStateInternal(UplinkSessionState newState) {
            UplinkSessionState oldState = mainState;
            if (newState == oldState) {
                // TODO >10.2: tolerate these during 10.2 hotfixes, but eliminate them in a future release
                if (newState == UplinkSessionState.CLEAN_SHUTDOWN || newState == UplinkSessionState.UNCLEAN_SHUTDOWN) {
                    log.debug("Redundant request to set the state of session " + logDescriptor + STRING_TO + newState);
                    return;
                }
                throw new IllegalStateException("Redundant request to set the state of session " + logDescriptor + STRING_TO + newState);
            }

            if (oldState.isTerminal()) {
                throw new IllegalStateException("Tried to set the state of session " + logDescriptor + STRING_TO + newState
                    + " while it is already in terminal state " + oldState);
            }

            // specific consistency checks
            if (newState == UplinkSessionState.GOODBYE_HANDSHAKE && (remoteSideHasSentGoodbye == ownGoodbyeSent)) {
                throw new IllegalStateException(
                    "Consistency violation: " + newState + LOG_SLASH + ownGoodbyeSent + LOG_SLASH + remoteSideHasSentGoodbye);
            }
            if (newState == UplinkSessionState.GOODBYE_HANDSHAKE_COMPLETE && (!remoteSideHasSentGoodbye || !ownGoodbyeSent)) {
                throw new IllegalStateException(
                    "Consistency violation: " + newState + LOG_SLASH + ownGoodbyeSent + LOG_SLASH + remoteSideHasSentGoodbye);
            }

            // TODO (p2) 11.0: consider moving this to verbose logging
            log.debug(StringUtils.format("%s%s -> %s", logPrefix, oldState, newState));
            mainState = newState;

            onSessionStateChanged(oldState, newState);

            // common "terminal state reached" handling
            if (newState.isTerminal()) {
                // probably redundant, but in that case, it is a NOP
                getLowLevelProtocolWrapper().terminateSession();
                onTerminalStateReached(newState);
            }
        }

        public synchronized UplinkSessionState getMainState() {
            return mainState;
        }

        // TODO (p3) consider moving server-side-only fields to a subclass

        // server-side only
        public synchronized void setClientVersionInfo(String clientVersionInfo) {
            if (clientVersionInfo != null) {
                this.clientVersionInfo = clientVersionInfo;
            } else {
                this.clientVersionInfo = "<undefined>";
            }
        }

        // server-side only
        public synchronized String getClientVersionInfo() {
            return clientVersionInfo;
        }

        // currently server-side only
        public synchronized void setProtocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            this.heartbeatSendingEnabled = !(UplinkProtocolConstants.LEGACY_PROTOCOL_VERSION_0_1.equals(protocolVersion));
        }

        // currently server-side only
        public synchronized String getProtocolVersion() {
            return protocolVersion;
        }

        // currently server-side only
        public synchronized boolean isHeartbeatSendingEnabled() {
            return heartbeatSendingEnabled;
        }

        // currently server-side only
        public synchronized void setEffectiveAccountName(String effectiveAccountName) {
            this.effectiveAccountName = effectiveAccountName;
        }

        // currently server-side only
        public synchronized String getEffectiveAccountName() {
            return effectiveAccountName;
        }

        // currently server-side only
        public synchronized void setEffectiveSessionQualifier(String effectiveSessionQualifier) {
            this.effectiveSessionQualifier = effectiveSessionQualifier;
        }

        // currently server-side only
        public synchronized String getEffectiveSessionQualifier() {
            return effectiveSessionQualifier;
        }

        public synchronized void markHeartbeatSent() {
            this.lastHeartbeatSentTime = System.currentTimeMillis();
            this.pendingHeartbeatExchange = true;
        }

        public synchronized void markHeartbeatResponseReceived() {
            if (pendingHeartbeatExchange) {
                long duration = System.currentTimeMillis() - lastHeartbeatSentTime;
                if (duration > UplinkProtocolConstants.HEARTBEAT_RESPONSE_TIME_WARNING_THRESHOLD) {
                    log.warn(logPrefix + "Observed long round-trip response time of " + duration + " msec");
                }
                pendingHeartbeatExchange = false;
            } else {
                log.warn(logPrefix + "Received a " + MessageType.HEARTBEAT_RESPONSE + " message without expecting one");
            }
        }

        public synchronized boolean validateHeartbeatResponseIfExpected() {
            if (pendingHeartbeatExchange) {
                long duration = System.currentTimeMillis() - lastHeartbeatSentTime;
                log.debug(logPrefix + "No heartbeat response received within " + duration + " msec, assuming broken connection or client");
                initiateUncleanShutdownIfStillRunning();
                return false;
            } else {
                return true;
            }
        }
    }

    protected AbstractUplinkSessionImpl(ConcurrencyUtilsFactory concurrencyUtilsFactory) {
        Objects.requireNonNull(concurrencyUtilsFactory);
        this.incomingMessageQueue =
            concurrencyUtilsFactory.createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
        this.outgoingMessageQueue =
            concurrencyUtilsFactory.createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
    }

    protected abstract void onTerminalStateReached(UplinkSessionState newState);

    @Override
    public UplinkSessionState getState() {
        return sessionState.getMainState();
    }

    @Override
    public boolean isShuttingDownOrShutDown() {
        return sessionState.isShuttingDownOrShutDown();
    }

    @Override
    public final void enqueueMessageBlockForSending(long channelId, MessageBlock messageBlock) throws IOException {
        try {
            if (!outgoingMessageQueueLimit.tryAcquire()) {
                // the outgoing message queue is full, so no permit could be fetched immediately -> log this and wait for it instead
                log.debug(logPrefix + "Stalling new additions to the send queue as there are already "
                    + OUTGOING_MESSAGE_QUEUE_SIZE + " enqueued messages");
                // TODO in normal behavior, this should not pose any deadlock potential; check if it does in extreme situations
                outgoingMessageQueueLimit.acquire();
            }
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            log.warn(logPrefix + "Interrupted while waiting for rate limiting to enqueue a message of type " + messageBlock.getType());
            return;
        }
        if (DEBUG_OUTPUT_ENABLED) {
            log.debug(StringUtils.format("%sEnqueueing message of type %s for sending to channel %d, payload size %d bytes",
                logPrefix, messageBlock.getType(), channelId, messageBlock.getDataLength()));
        }
        outgoingMessageQueue.enqueue(() -> {
            try {
                sendEnqueuedMessage(channelId, messageBlock);
            } finally {
                outgoingMessageQueueLimit.release();
            }
        });
    }

    @Override
    public final void initiateCleanShutdownIfRunning() {
        synchronized (sessionState) {
            if (!sessionState.isShuttingDownOrShutDown()) {
                initiateCleanShutdown();
            }
        }
    }

    protected final void handleRegularRemoteGoodbyeMessage() {
        synchronized (sessionState) {
            // protocol check: no more than one goodbye message
            if (sessionState.getRemoteSideHasSentGoodbye()) {
                log.error(logPrefix + "Protocol error: Received more than one 'goodbye' message from remote side");
                return;
            }
            sessionState.markRemoteSideHasSentGoodbye(); // may switch to a new state
            switch (sessionState.getMainState()) {
            case GOODBYE_HANDSHAKE:
                // the other side has initiated a goodbye handshake
                log.debug(logPrefix + "Received 'goodbye' message from remote side, initiating clean shutdown");
                initiateCleanShutdown();
                break;
            case GOODBYE_HANDSHAKE_COMPLETE:
                // the other side has confirmed a goodbye handshake initiated by this side
                log.debug(logPrefix + "Received 'goodbye' confirmation from remote side, closing stream");
                asyncTaskService.execute("Close outgoing Uplink stream after goodbye handshake", () -> {
                    getLowLevelProtocolWrapper().terminateSession();
                    sessionState.markOutgoingStreamClosed();
                });
                break;
            case CLEAN_SHUTDOWN:
            case UNCLEAN_SHUTDOWN_INITIATED:
            case UNCLEAN_SHUTDOWN:
                log.debug(logPrefix
                    + "Ignoring redundant 'goodbye' message as the session is already in state " + sessionState.getMainState());
                break;
            default:
                log.debug(logPrefix + "Unhandled state after receiving a 'goodbye' message: " + sessionState.getMainState());
            }
        }
    }

    protected final void handleIncomingStreamClosedOrEOF() {
        synchronized (sessionState) {
            sessionState.markIncomingStreamClosedOrEOF();
        }
    }

    @Override
    public final boolean isActive() {
        return sessionState.getMainState() == UplinkSessionState.ACTIVE;
    }

    @Override
    public final String getAssignedNamespaceId() {
        return sessionState.getAssignedNamespaceId();
    }

    @Override
    public final Optional<String> getAssignedNamespaceIdIfAvailable() {
        return sessionState.getAssignedNamespaceIdIfAvailable();
    }

    @Override
    public final String getLogDescriptor() {
        return sessionState.logDescriptor; // volatile; do NOT synchronize this on "this"! (see issue 17448)
    }

    @Override
    public final String getDestinationIdPrefix() {
        return getAssignedNamespaceId();
    }

    @Override
    public final String toString() {
        return getLogDescriptor();
    }

    protected final void markClientHandshakeSentOrReceived() {
        sessionState.markClientHandshakeSentOrReceived();
    }

    protected final void markServerHandshakeSentOrReceived() {
        sessionState.markServerHandshakeSentOrReceived();
    }

    public void markHandshakeFailed(UplinkConnectionRefusedException e) {
        log.debug(logPrefix + "Uplink connection failed or refused: " + e.getMessage());
        sessionState.markHandshakeFailed();
        // TODO >10.2: somewhat unclean to do it here; rework
        getLowLevelProtocolWrapper().terminateSession();
        sessionState.markOutgoingStreamClosed();
    }

    public void markHandshakeSuccessful() {
        sessionState.markHandshakeSuccessful();
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

    private void initiateCleanShutdown() {
        final UplinkSessionState startingState = getState();
        if (startingState != UplinkSessionState.ACTIVE && startingState != UplinkSessionState.GOODBYE_HANDSHAKE) {
            log.warn(logPrefix + "Initiatiating clean shutdown from non-ACTIVE state " + getState());
        }
        sessionState.setShuttingDown(); // prevents enqueueing further outgoing messages
        outgoingMessageQueue.enqueue(() -> {
            if (DEBUG_OUTPUT_ENABLED) {
                if (startingState == UplinkSessionState.ACTIVE) {
                    log.debug(logPrefix + "Sending 'goodbye' message to initiate clean shutdown");
                } else {
                    log.debug(logPrefix + "Sending 'goodbye' message to confirm remote-initiated clean shutdown");
                }
            }
            if (getLowLevelProtocolWrapper().attemptToSendRegularGoodbyeMessage()) {
                sessionState.markOwnGoodbyeSent();
                if (sessionState.getRemoteSideHasSentGoodbye()) {
                    // remote side has already sent goodbye -> close immediately
                    getLowLevelProtocolWrapper().terminateSession();
                    sessionState.markOutgoingStreamClosed();
                    if (onSessionShutdownFinishedRunner.isPresent()) {
                        onSessionShutdownFinishedRunner.get().run();
                    }
                } else {
                    // self-initiated goodbye -> wait before until closing on timeout
                    asyncTaskService.scheduleAfterDelay("Close local end of Uplink stream", () -> {
                        if (!sessionState.isOutgoingStreamClosed()) {
                            getLowLevelProtocolWrapper().terminateSession();
                            sessionState.markOutgoingStreamClosed();
                        }
                        if (onSessionShutdownFinishedRunner.isPresent()) {
                            onSessionShutdownFinishedRunner.get().run();
                        }
                    }, GOODBYE_CONFIRMATION_WAIT_TIMEOUT_MSEC);
                }
            } else {
                // failed to send goodbye message; close the connection without waiting
                handleStreamWriteError(null);
                // potentially redundant, but in that case, it is a NOP
                getLowLevelProtocolWrapper().terminateSession();
                sessionState.markOutgoingStreamClosed();
                if (onSessionShutdownFinishedRunner.isPresent()) {
                    onSessionShutdownFinishedRunner.get().run();
                }
            }
        });
    }

    protected void handleFatalError(UplinkProtocolErrorType errorType, String errorMessage) {
        log.warn(StringUtils.format("%sFatal error in Uplink session for %s, closing the session: %s [type %s]",
            logPrefix, getRemoteSideInformationString(), errorMessage, errorType.name()));
        synchronized (sessionState) {
            sessionState.markFatalError();
            initiateUncleanShutdownIfStillRunning();
        }
    }

    protected final void handleStreamWriteError(IOException e) {
        synchronized (sessionState) {
            sessionState.markOutgoingStreamWriteError();
            initiateUncleanShutdownIfStillRunning();
        }
    }

    private void initiateUncleanShutdownIfStillRunning() {
        synchronized (sessionState) {
            if (sessionState.isShuttingDownOrShutDown()) {
                log.debug("Ignoring redundant call to initiate an unclean shutdown");
                return;
            }
            if (sessionState.getMainState() == UplinkSessionState.ACTIVE) {
                sessionState.setMainStateInternal(UplinkSessionState.UNCLEAN_SHUTDOWN_INITIATED);
            }
            // on a broken connection, do not send a goodbye message; just close the stream
            sessionState.setShuttingDown(); // prevents enqueueing further outgoing messages
            outgoingMessageQueue.enqueue(() -> {
                getLowLevelProtocolWrapper().terminateSession();
                sessionState.markOutgoingStreamClosed();
            });
        }
    }

    // TODO move this in to the synchronized scope as part of the planned 10.3+ rework
    private void sendEnqueuedMessage(long channelId, MessageBlock messageBlock) {
        // do not send anything except goodbye messages when shutting down
        // note: isShuttingDownOrShutDown() requires the session's state lock, so this method must not be called with any locks held
        if (isShuttingDownOrShutDown() && messageBlock.getType() != MessageType.GOODBYE) {
            log.debug(logPrefix + "Discarding enqueued message of type " + messageBlock.getType() + " as the session is shutting down");
            return;
        }
        try {
            getProtocolWrapper().sendMessageBlock(channelId, messageBlock);
            if (DEBUG_OUTPUT_ENABLED) {
                log.debug(logPrefix + "Successfully sent message of type " + messageBlock.getType());
            }
        } catch (IOException e) {
            log.error("Error during asynchronous sending of message with type " + messageBlock.getType());
            handleStreamWriteError(e);
        }
    }

    protected final void setAssignedNamespaceId(String serverAssignedNamespaceId) {
        sessionState.setAssignedNamespaceId(serverAssignedNamespaceId);
    }

    protected final void setNamespaceIdReleased() {
        sessionState.setNamespaceIdReleased();
    }

    protected final void updateLogDescriptor() {
        sessionState.updateLogDescriptor();
    }

    protected abstract CommonUplinkLowLevelProtocolWrapper getLowLevelProtocolWrapper();

    protected abstract String getRemoteSideInformationString();

    @Override
    public void registerOnShutdownFinishedListener(Runnable run) {
        this.onSessionShutdownFinishedRunner = Optional.of(run);
    }

}
