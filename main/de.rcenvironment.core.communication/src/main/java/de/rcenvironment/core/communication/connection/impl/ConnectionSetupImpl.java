/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.impl;

import static de.rcenvironment.core.communication.channel.MessageChannelState.MARKED_AS_BROKEN;
import static de.rcenvironment.core.communication.connection.api.ConnectionSetupState.CONNECTED;
import static de.rcenvironment.core.communication.connection.api.ConnectionSetupState.CONNECTING;
import static de.rcenvironment.core.communication.connection.api.ConnectionSetupState.DISCONNECTED;
import static de.rcenvironment.core.communication.connection.api.ConnectionSetupState.DISCONNECTING;
import static de.rcenvironment.core.communication.connection.api.ConnectionSetupState.WAITING_TO_RECONNECT;
import static de.rcenvironment.core.communication.connection.api.DisconnectReason.ACTIVE_SHUTDOWN;
import static de.rcenvironment.core.communication.connection.api.DisconnectReason.ERROR;
import static de.rcenvironment.core.communication.connection.api.DisconnectReason.FAILED_TO_AUTO_RECONNECT;
import static de.rcenvironment.core.communication.connection.api.DisconnectReason.FAILED_TO_CONNECT;
import static de.rcenvironment.core.communication.connection.api.DisconnectReason.REMOTE_SHUTDOWN;
import static de.rcenvironment.core.communication.connection.impl.ConnectionSetupImpl.StateMachineEventType.AUTO_RETRY_DELAY_EXPIRED;
import static de.rcenvironment.core.communication.connection.impl.ConnectionSetupImpl.StateMachineEventType.CHANNEL_BROKEN;
import static de.rcenvironment.core.communication.connection.impl.ConnectionSetupImpl.StateMachineEventType.CHANNEL_CLOSED_BY_OWN_REQUEST;
import static de.rcenvironment.core.communication.connection.impl.ConnectionSetupImpl.StateMachineEventType.CHANNEL_CLOSED_BY_REMOTE;
import static de.rcenvironment.core.communication.connection.impl.ConnectionSetupImpl.StateMachineEventType.CONNECT_ATTEMPT_FAILED;
import static de.rcenvironment.core.communication.connection.impl.ConnectionSetupImpl.StateMachineEventType.CONNECT_ATTEMPT_SUCCESSFUL;
import static de.rcenvironment.core.communication.connection.impl.ConnectionSetupImpl.StateMachineEventType.START_REQUESTED;
import static de.rcenvironment.core.communication.connection.impl.ConnectionSetupImpl.StateMachineEventType.STOP_REQUESTED;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupListener;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;
import de.rcenvironment.core.communication.connection.api.DisconnectReason;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;
import de.rcenvironment.core.utils.incubator.AbstractFixedTransitionsStateMachine;
import de.rcenvironment.core.utils.incubator.StateChangeException;

/**
 * Default {@link ConnectionSetup} implementation.
 * 
 * @author Robert Mischke
 */
// TODO >5.0.0: improve threading/locking model after reworking state machine base class - misc_ro
public class ConnectionSetupImpl implements ConnectionSetup {

    private static final int MINIMUM_INITIAL_DELAY_MSEC = 5000;

    private static final int SEC_TO_MSEC_FACTOR = 1000;

    private static final int NO_MAXIMUM_AUTO_RETRY_DELAY = 0; // marker value

    private static final ConnectionSetupState[][] VALID_STATE_TRANSITIONS = new ConnectionSetupState[][] {

        // standard lifecycle
        { DISCONNECTED, CONNECTING },
        { CONNECTING, CONNECTED },
        { CONNECTED, DISCONNECTING },
        { DISCONNECTING, DISCONNECTED },
        // connection failure
        { CONNECTING, DISCONNECTED },
        // follow-up to connection failure with automatic reconnect enabled
        { DISCONNECTED, WAITING_TO_RECONNECT },
        // automatic reconnect delay has passed, reconnecting
        { WAITING_TO_RECONNECT, CONNECTING },
        // actively disconnected/stopped by user while waiting for automatic reconnect
        { WAITING_TO_RECONNECT, DISCONNECTED },
        // message channel closed without active disconnect
        { CONNECTED, DISCONNECTED }
    };

    private static final int STATE_WAITING_POLLING_INTERVAL = 25;

    private NetworkContactPoint ncp;

    private String displayName;

    private MessageChannelService channelService;

    private final StateMachine stateMachine = new StateMachine();

    private final ThreadPool threadPool = SharedThreadPool.getInstance();

    private final Log log = LogFactory.getLog(getClass());

    private ConnectionSetupListener listener;

    private long id;

    private boolean connnectOnStartup;

    private boolean autoRetryEnabled;

    private int autoRetryInitialDelayMsec;

    private int autoRetryMaximumDelayMsec;

    private float autoRetryDelayMultiplier;

    /**
     * The event types posted to the connection setup's state machine.
     * 
     * @author Robert Mischke
     */
    protected enum StateMachineEventType {
        START_REQUESTED,
        STOP_REQUESTED,
        CONNECT_ATTEMPT_SUCCESSFUL, // context: attempt id, message channel
        CONNECT_ATTEMPT_FAILED, // context: attempt id
        AUTO_RETRY_DELAY_EXPIRED,
        CHANNEL_CLOSED_BY_OWN_REQUEST, // context: message channel
        CHANNEL_BROKEN, // context: message channel
        CHANNEL_CLOSED_BY_REMOTE // context: message channel
    }

    /**
     * The event objects posted to the connection setup's state machine, encapsulating an event type and an optional channel id this event
     * relates to.
     * 
     * The reason why this is necessary is that otherwise, delayed asynchronous events could be misinterpreted, for example a delayed
     * "connection error" event that was caused by a channel different from the one that is currently active. Without the channel id, this
     * could lead to the current channel being wrongly closed; with the id, this can be prevented.
     * 
     * @author Robert Mischke
     */
    private static final class StateMachineEvent {

        private final StateMachineEventType type;

        private final MessageChannel relatedChannel;

        private final long taskId;

        StateMachineEvent(StateMachineEventType type) {
            this(type, null, 0);
        }

        StateMachineEvent(StateMachineEventType type, MessageChannel relatedChannel) {
            this(type, relatedChannel, 0);
        }

        StateMachineEvent(StateMachineEventType type, MessageChannel relatedChannel, long taskId) {
            if (taskId < 0) {
                throw new IllegalArgumentException();
            }
            this.type = type;
            this.relatedChannel = relatedChannel;
            this.taskId = taskId;
        }

        public StateMachineEventType getType() {
            return type;
        }

        public MessageChannel getRelatedChannel() {
            return relatedChannel;
        }

        public long getTaskId() {
            return taskId;
        }

        @Override
        public String toString() {
            return StringUtils.format("%s (#%d, %s)", type.name(), taskId, relatedChannel);
        }

    }

    /**
     * Internal state machine for {@link ConnectionSetupImpl} instances.
     * 
     * @author Robert Mischke
     */
    private final class StateMachine extends AbstractFixedTransitionsStateMachine<ConnectionSetupState, StateMachineEvent> {

        /**
         * A {@link Runnable} that performs a connect attempt. Posts either a {@link StateMachineEventType#CONNECT_ATTEMPT_SUCCESSFUL} or a
         * {@link StateMachineEventType#CONNECT_ATTEMPT_FAILED} event on completion.
         * 
         * @author Robert Mischke
         */
        private final class AsyncConnectTask implements Runnable {

            private final long taskId;

            private final boolean isAutoRetry;

            private volatile Future<MessageChannel> future;

            AsyncConnectTask(long taskId, boolean isAutoRetry) {
                this.taskId = taskId;
                this.isAutoRetry = isAutoRetry;
            }

            @Override
            @TaskDescription("Communication Layer: ConnectionSetup connecting")
            public void run() {
                try {
                    future = channelService.connect(ncp, true);
                    try {
                        MessageChannel newMessageChannel = future.get();
                        log.debug("Message channel " + newMessageChannel.getChannelId() + " established for connection setup " + id);
                        channelService.registerNewOutgoingChannel(newMessageChannel);
                        postEvent(new StateMachineEvent(CONNECT_ATTEMPT_SUCCESSFUL, newMessageChannel, taskId));
                    } catch (InterruptedException e) {
                        throw new CommunicationException("The connection attempt was interrupted");
                    } catch (ExecutionException e) {
                        // unwrap exception as far as possible for shorter stacktrace
                        throw unwrapFailedToConnectException(e);
                    }
                } catch (CommunicationException e) {
                    // TODO reduce number of stacktrace layers by unwrapping or changing source behaviour - misc_ro
                    final String exceptionString;
                    if (e.getCause() == null) {
                        // typical case: only use message
                        exceptionString = e.getMessage();
                    } else {
                        // rare/unexpected case: add whole cause chain
                        exceptionString = e.toString();
                    }
                    if (isAutoRetry) {
                        log.info(StringUtils.format("Failed to auto-reconnect to \"%s\": %s (Connection details: %s)",
                            displayName, exceptionString, getNetworkContactPointString()));
                    } else {
                        log.warn(StringUtils.format("Failed to connect to \"%s\": %s (Connection details: %s)",
                            displayName, exceptionString, getNetworkContactPointString()));
                    }
                    postEvent(new StateMachineEvent(CONNECT_ATTEMPT_FAILED, null, taskId));
                } catch (CancellationException e) {
                    log.info(StringUtils.format("The connect attempt to \"%s\" was cancelled", displayName));
                }
            }

            public void attemptToCancel() {
                final Future<MessageChannel> copyOfFuture = future;
                if (copyOfFuture != null) {
                    copyOfFuture.cancel(true);
                }
            }

            private CommunicationException unwrapFailedToConnectException(ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof CommunicationException) {
                    return (CommunicationException) cause;
                } else if (cause != null) {
                    return new CommunicationException(cause);
                } else {
                    return new CommunicationException(e);
                }
            }
        }

        /**
         * A {@link Runnable} that closes the given {@link MessageChannel}. No {@link StateMachineEvent} is posted on completion; instead,
         * the closing channel will be detected by an external call to the {@link ConnectionSetupImpl#onMessageChannelClosed()} method.
         * 
         * @author Robert Mischke
         */
        private final class AsyncDisconnectTask implements Runnable {

            private final MessageChannel channel;

            private AsyncDisconnectTask(MessageChannel channel) {
                this.channel = channel;
            }

            @Override
            @TaskDescription("Communication Layer: ConnectionSetup disconnecting")
            public void run() {
                channelService.closeOutgoingChannel(channel);
            }
        }

        /**
         * A simple callback {@link Runnable} to signal when the auto-retry wait time has expired.
         * 
         * @author Robert Mischke
         */
        private final class AsyncAutoRetryTrigger implements Runnable {

            private final long taskId;

            private AsyncAutoRetryTrigger(long taskId) {
                this.taskId = taskId;
            }

            @Override
            @TaskDescription("Communication Layer: ConnectionSetup auto-reconnect timer")
            public void run() {
                postEvent(new StateMachineEvent(AUTO_RETRY_DELAY_EXPIRED, null, taskId));
            }
        }

        private boolean isConnectionIntended;

        // cleared on disconnect
        private MessageChannel connectedMessageChannel;

        // not cleared on disconnect (for fetching information after a disconnect)
        private MessageChannel lastConnectedMessageChannel;

        private DisconnectReason lastDisconnectReason;

        private int consecutiveConnectionFailures;

        private boolean currentConnectAttemptIsAutoRetry = false; // set on connection failure, if applicable

        private AsyncConnectTask currentConnectTask = null;

        private long currentConnectTaskId;

        private long currentAutoRetryWaitExpiredTaskId;

        StateMachine() {
            super(ConnectionSetupState.DISCONNECTED, VALID_STATE_TRANSITIONS);
        }

        public synchronized MessageChannel getConnectedMessageChannel() {
            return connectedMessageChannel;
        }

        public synchronized MessageChannel getLastConnectedMessageChannel() {
            return lastConnectedMessageChannel;
        }

        public synchronized DisconnectReason getLastDisconnectReason() {
            return lastDisconnectReason;
        }

        @Override
        protected ConnectionSetupState processEvent(ConnectionSetupState currentState, StateMachineEvent event)
            throws StateChangeException {
            log.debug(StringUtils.format("Processing event %s while in state %s", event, currentState));
            switch (event.getType()) {
            case START_REQUESTED:
                isConnectionIntended = true;
                switch (currentState) {
                case DISCONNECTED:
                    // connect
                    connectAsync(false); // false = no auto-retry
                    return CONNECTING;
                case WAITING_TO_RECONNECT:
                    connectAsync(false); // false = no auto-retry (reset failure count etc.)
                    return CONNECTING;
                default:
                    // TODO add reconnect wait time shortening
                    log.debug("Ignoring connection START request while in state " + currentState);
                    return null;
                }
            case STOP_REQUESTED:
                isConnectionIntended = false;
                switch (currentState) {
                case CONNECTED:
                    // this state is only used on active shutdown; external events switch to DISCONNECTED immediately
                    lastDisconnectReason = ACTIVE_SHUTDOWN;
                    if (connectedMessageChannel != null) {
                        disconnectAsync(connectedMessageChannel);
                        return DISCONNECTING;
                    } else {
                        log.warn("Undefined active channel when processing event " + event);
                        return null;
                    }
                case CONNECTING:
                    // invalidate the current connect attempt
                    currentConnectTaskId++;
                    if (currentConnectTask != null) {
                        log.debug("Cancelling connect attempt");
                        currentConnectTask.attemptToCancel();
                    } else {
                        log.warn("Unexpected state: Connection is " + CONNECTING + ", but there is no associated task");
                    }
                    return DISCONNECTED;
                case WAITING_TO_RECONNECT:
                    lastDisconnectReason = ACTIVE_SHUTDOWN;
                    return DISCONNECTED;
                default:
                    // FIXME implement: connect cancelling, ...
                    log.warn("Ignoring STOP request as state " + currentState + " is not supported yet");
                    return null;
                }
            case CONNECT_ATTEMPT_SUCCESSFUL:
                MessageChannel newChannel = event.getRelatedChannel();
                if (!checkForCurrentAttemptId(event, currentConnectTaskId)) {
                    log.warn("Connection established, but it belongs to an outdated connect request; triggering disconnect of channel "
                        + newChannel);
                    disconnectAsync(newChannel);
                    return null;
                }
                currentConnectTask = null;
                if (!isConnectionIntended) {
                    log.warn("Connection established, but no connection is intended anymore; triggering disconnect of channel "
                        + newChannel);
                    disconnectAsync(newChannel);
                    return null;
                }
                if (currentState != CONNECTING) {
                    log.debug("Discarding event " + event + " as the current state is not " + CONNECTING);
                    return null;
                }
                connectedMessageChannel = newChannel;
                lastConnectedMessageChannel = newChannel;
                if (consecutiveConnectionFailures == 0) {
                    log.info(StringUtils.format("Network connection established: \"%s\"", displayName));
                } else {
                    // TODO text is not quite correct if a connection broke down and is reestablished on the first attempt - misc_ro
                    log.info(StringUtils.format("Network connection \"%s\" was successfully established after %d failed attempts",
                        displayName, consecutiveConnectionFailures));
                }
                return CONNECTED;
            case CONNECT_ATTEMPT_FAILED:
                if (!checkForCurrentAttemptId(event, currentConnectTaskId)) {
                    return null;
                }
                currentConnectTask = null;
                consecutiveConnectionFailures++;
                final boolean wasAutoRetryAttempt = currentConnectAttemptIsAutoRetry;
                final boolean triggerAutoRetry;
                if (wasAutoRetryAttempt) {
                    lastDisconnectReason = FAILED_TO_AUTO_RECONNECT;
                    triggerAutoRetry = true;
                } else {
                    lastDisconnectReason = FAILED_TO_CONNECT;
                    triggerAutoRetry = isConnectionIntended && autoRetryEnabled;
                }
                listener.onConnectionAttemptFailed(ConnectionSetupImpl.this, consecutiveConnectionFailures == 1, triggerAutoRetry);
                if (triggerAutoRetry) {
                    return WAITING_TO_RECONNECT;
                } else {
                    return DISCONNECTED;
                }
            case CHANNEL_CLOSED_BY_OWN_REQUEST:
            case CHANNEL_BROKEN:
            case CHANNEL_CLOSED_BY_REMOTE:
                return handleDisconnectEvent(event);
            case AUTO_RETRY_DELAY_EXPIRED:
                // if auto-retry mode was cancelled in the meantime, ignore this timer call
                if (currentState != WAITING_TO_RECONNECT) {
                    return null;
                }
                if (currentAutoRetryWaitExpiredTaskId != event.getTaskId()) {
                    log.debug("Ignoring an outdated auto-retry timer callback");
                    return null;
                }
                log.debug("Reconnect delay expired, auto-retrying connection " + displayName);
                connectAsync(true); // true = auto-retry
                return CONNECTING;
            default:
                log.warn("Unprocessed event: " + event);
                break;
            }
            return null; // do not change state
        }

        private boolean checkForCurrentAttemptId(StateMachineEvent event, long currentAttemptId) {
            // sanity check
            if (currentAttemptId <= 0) {
                throw new IllegalStateException();
            }
            if (currentAttemptId == event.getTaskId()) {
                return true;
            } else {
                log.debug(StringUtils.format("Ignoring event of type %s as it refers to attempt #%d while the current attempt is #%d",
                    event.getType(), event.getTaskId(), currentAttemptId));
                return false;
            }
        }

        private ConnectionSetupState handleDisconnectEvent(StateMachineEvent event) {
            if (event.getRelatedChannel() != connectedMessageChannel) {
                log.debug("Ignoring " + CHANNEL_BROKEN + " event as it refers to message channel " + event.getRelatedChannel()
                    + ", while the current channel is " + connectedMessageChannel);
                return null;
            }
            boolean triggerAutoRetry = false;
            switch (event.getType()) {
            case CHANNEL_CLOSED_BY_OWN_REQUEST:
                lastDisconnectReason = ACTIVE_SHUTDOWN;
                // never auto-retry on active disconnect, regardless of setting
                break;
            case CHANNEL_BROKEN:
                triggerAutoRetry = autoRetryEnabled;
                lastDisconnectReason = ERROR;
                break;
            case CHANNEL_CLOSED_BY_REMOTE:
                triggerAutoRetry = autoRetryEnabled;
                lastDisconnectReason = REMOTE_SHUTDOWN;
                break;
            default:
                throw new RuntimeException("Should not be reached with event type " + event.getType());
            }
            listener.onConnectionClosed(ConnectionSetupImpl.this, lastDisconnectReason, triggerAutoRetry);
            log.info(StringUtils.format("Network connection closed (%s): \"%s\"", lastDisconnectReason.getDisplayText(), displayName));
            if (triggerAutoRetry) {
                // count the initial connection breakdown towards the number of connection failures; this prevents redundant user feedback
                // on the first attempt to reconnect
                // TODO move to state change code? would require additional "enter reconnect" state, though
                consecutiveConnectionFailures = 1;
                return WAITING_TO_RECONNECT;
            } else {
                return DISCONNECTED;
            }
        }

        private void connectAsync(boolean isAutoRetry) {
            currentConnectAttemptIsAutoRetry = isAutoRetry;
            if (!isAutoRetry) {
                consecutiveConnectionFailures = 0;
            }
            currentConnectTaskId++;
            currentConnectTask = new AsyncConnectTask(currentConnectTaskId, isAutoRetry);
            threadPool.execute(currentConnectTask);
        }

        private void disconnectAsync(final MessageChannel channel) {
            threadPool.execute(new AsyncDisconnectTask(channel));
        }

        @Override
        protected void onStateChanged(ConnectionSetupState oldState, ConnectionSetupState newState) {
            log.debug("Connection setup " + displayName + " changed state from " + oldState + " to " + newState);
            switch (newState) {
            case CONNECTING:
                if (connectedMessageChannel != null) {
                    log.error("Internal error: Current message channel was not 'null' when transitioning from " + oldState + " to "
                        + newState);
                    connectedMessageChannel = null;
                }
                lastDisconnectReason = null;
                break;
            case DISCONNECTING:
                break;
            case DISCONNECTED:
                connectedMessageChannel = null;
                break;
            case WAITING_TO_RECONNECT:
                connectedMessageChannel = null;
                long targetDelay = calculateNextAutoRetryDelay();
                log.debug(StringUtils.format("Scheduling auto-retry of connection %s in %d msec "
                    + "(failure count: %d, delay multiplier: %s, maximum: %d)", displayName,
                    targetDelay, consecutiveConnectionFailures, autoRetryDelayMultiplier, autoRetryMaximumDelayMsec));

                final long taskId = ++currentAutoRetryWaitExpiredTaskId;
                threadPool.scheduleAfterDelay(new AsyncAutoRetryTrigger(taskId), targetDelay);
                break;
            default:
                break;
            }

            listener.onStateChanged(ConnectionSetupImpl.this, oldState, newState);
        }

        @Override
        protected void onStateChangeException(StateMachineEvent event, StateChangeException e) {
            log.error("Invalid state change attempt, cause by event " + event, e);

        }

        private long calculateNextAutoRetryDelay() {
            long targetDelay =
                Math.round(autoRetryInitialDelayMsec * Math.pow(autoRetryDelayMultiplier, consecutiveConnectionFailures - 1));
            if (autoRetryMaximumDelayMsec != NO_MAXIMUM_AUTO_RETRY_DELAY) {
                // apply upper limit, if set
                targetDelay = Math.min(targetDelay, autoRetryMaximumDelayMsec);
            }
            return targetDelay;
        }

    }

    public ConnectionSetupImpl(NetworkContactPoint ncp, String displayName, long id, boolean connnectOnStartup,
        MessageChannelService channelService, ConnectionSetupListener listener) {
        this.ncp = ncp;
        this.displayName = displayName;
        this.id = id;
        this.connnectOnStartup = connnectOnStartup;
        this.channelService = channelService;
        this.listener = listener;
        Map<String, String> attributes = ncp.getAttributes();
        parseAutoRetryConfiguration(attributes);
    }

    @Override
    public void connectSync() throws CommunicationException {
        // FIXME implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void signalStartIntent() {
        stateMachine.postEvent(new StateMachineEvent(START_REQUESTED));
    }

    @Override
    public void signalStopIntent() {
        stateMachine.postEvent(new StateMachineEvent(STOP_REQUESTED));
    }

    @Override
    public void awaitState(ConnectionSetupState targetState, int timeoutMsec) throws TimeoutException, InterruptedException {
        if (stateMachine.getState() == targetState) {
            return;
        }
        int timeRemaining = timeoutMsec;
        while (timeRemaining > 0) {
            int waitTime = Math.min(STATE_WAITING_POLLING_INTERVAL, timeRemaining);
            Thread.sleep(waitTime);
            if (stateMachine.getState() == targetState) {
                return;
            }
            timeRemaining -= STATE_WAITING_POLLING_INTERVAL;
        }
        throw new TimeoutException();
    }

    @Override
    public ConnectionSetupState getState() {
        return stateMachine.getState();
    }

    @Override
    public DisconnectReason getDisconnectReason() {
        return stateMachine.getLastDisconnectReason();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean getConnnectOnStartup() {
        return connnectOnStartup;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getNetworkContactPointString() {
        return NetworkContactPointUtils.toDefinitionString(ncp);
    }

    /**
     * Callback to notify this setup that its associated {@link MessageChannel} was closed.
     * 
     * @param messageChannel the associated {@link MessageChannel}
     */
    public void onMessageChannelClosed(MessageChannel messageChannel) {
        log.debug("Message channel closed, adapting state of connection setup " + id + "; channel state: " + messageChannel.getState());
        if (messageChannel.getState() == MARKED_AS_BROKEN) {
            stateMachine.postEvent(new StateMachineEvent(CHANNEL_BROKEN, messageChannel));
        } else if (messageChannel.isClosedBecauseMirrorChannelClosed()) {
            // this assumes that remote channels only close on an orderly remote shutdown for now
            stateMachine.postEvent(new StateMachineEvent(CHANNEL_CLOSED_BY_REMOTE, messageChannel));
        } else {
            stateMachine.postEvent(new StateMachineEvent(CHANNEL_CLOSED_BY_OWN_REQUEST, messageChannel));
        }
    }

    @Override
    public MessageChannel getCurrentChannel() {
        return stateMachine.getConnectedMessageChannel();
    }

    @Override
    public String getCurrentChannelId() {
        MessageChannel channel = stateMachine.getConnectedMessageChannel();
        if (channel != null) {
            return channel.getChannelId();
        } else {
            return null;
        }
    }

    @Override
    public String getLastChannelId() {
        MessageChannel channel = stateMachine.getLastConnectedMessageChannel();
        if (channel != null) {
            return channel.getChannelId();
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != getClass()) {
            return false;
        }
        return ((ConnectionSetupImpl) obj).id == id;
    }

    @Override
    public int hashCode() {
        return ConnectionSetupImpl.class.hashCode() ^ (int) id;
    }

    private void parseAutoRetryConfiguration(Map<String, String> attributes) {
        // set defaults
        this.autoRetryEnabled = false;
        this.autoRetryDelayMultiplier = 1.0f;
        this.autoRetryMaximumDelayMsec = NO_MAXIMUM_AUTO_RETRY_DELAY;
        // parse
        String attrInitialDelay = attributes.get("autoRetryInitialDelay");
        if (attrInitialDelay != null) {
            try {
                this.autoRetryInitialDelayMsec = SEC_TO_MSEC_FACTOR * Integer.parseInt(attrInitialDelay);
                if (autoRetryInitialDelayMsec < MINIMUM_INITIAL_DELAY_MSEC) {
                    log.warn("Initial auto-retry delay cannot be less than " + MINIMUM_INITIAL_DELAY_MSEC + "; disabling for connection "
                        + getDisplayName());
                    return; // disable auto-retry
                }
                String attrMultiplier = attributes.get("autoRetryDelayMultiplier");
                if (attrMultiplier == null || attrMultiplier.isEmpty()) {
                    autoRetryDelayMultiplier = 1.0f;
                } else {
                    // Note: always expects dot-separated float (as intended), regardless of locale
                    autoRetryDelayMultiplier = Float.parseFloat(attrMultiplier);
                }
                if (autoRetryDelayMultiplier < 1.0f) {
                    log.warn("Auto-retry backoff multiplier cannot be less than 1; setting to 1");
                    autoRetryDelayMultiplier = 1.0f;
                }
                String attrMaxDelay = attributes.get("autoRetryMaximumDelay");
                if (attrMaxDelay != null) {
                    this.autoRetryMaximumDelayMsec = SEC_TO_MSEC_FACTOR * Integer.parseInt(attrMaxDelay);
                    if (autoRetryMaximumDelayMsec < autoRetryInitialDelayMsec) {
                        log.warn("Maximum auto-retry delay cannot be less than initial delay; disabling maximum delay for connection "
                            + getDisplayName());
                        autoRetryMaximumDelayMsec = NO_MAXIMUM_AUTO_RETRY_DELAY;
                    }
                }
                // no parse exceptions -> enable
                this.autoRetryEnabled = true;
                log.debug(StringUtils.format(
                    "Parsed auto-retry settings for connection \"%s\": Initial delay=%d msec, maximum=%d msec, multiplier=%s",
                    getDisplayName(), autoRetryInitialDelayMsec, autoRetryMaximumDelayMsec, autoRetryDelayMultiplier));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse auto-retry settings for connection setup " + getNetworkContactPointString());
            }
        }

    }

}
