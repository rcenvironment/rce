/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.session.internal.BoundedMessageBlockPrioritizer;

/**
 * Provides Uplink configuration parameters that should be adjustable, especially for unit/integration tests, and potentially be runtime
 * configuration. Immutable values should go into {@link UplinkProtocolConstants} instead.
 *
 * @author Robert Mischke
 */
public final class UplinkProtocolConfiguration {

    // set the default configuration to an instance as configured by the builder with no additional calls
    private static final UplinkProtocolConfiguration DEFAULT_CONFIGURATION = UplinkProtocolConfiguration.newBuilder().build();

    private static volatile UplinkProtocolConfiguration sharedCurrentConfiguration = DEFAULT_CONFIGURATION;

    // NOTE: the constants below are not meant to be used directly; these are set as default values in the map, where they can be overridden

    /**
     * This should be plenty, but as these are critical message, better leave a bit of head space to prevent surprises.
     */
    private static final int DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_HIGH = 10;

    /**
     * Intentionally set quite high to allow for some backpressure, but fail hard once it is exhausted to prevent complete server lockups.
     */
    private static final int DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_DEFAULT = 100;

    /**
     * Should be fairly high, as the server/relay message forwarding is a natural choke point, so some buffering is to be expected. However,
     * as backpressure forwarding is implemented for incoming messages as well, excessive buffering would be counter-productive as well.
     * <p>
     * A consequence of this is that when many clients swamp the server with traffic, this may result in fairly high (but known) memory
     * usage: approximately "maximum message block size * this message limit * number of sending clients". If a "fail on full queue" policy
     * was selected on submitting messages to this queue (which is not the case as of 10.2.4), this would cause intentional session aborts
     * on reaching this buffer size to protect the server, but at the cost of disconnecting data-receiving clients.
     * 
     * For file transfers, the total buffered data volume is (this value * number of file-transferring sessions * 32kb), the latter being
     * the maximum file transfer block size.
     */
    private static final int DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_LOW_BLACK_BOX_FORWARDING = 20; // adjust as necessary

    /**
     * These messages should not include bulk transfers, so this can be set fairly high.
     */
    private static final int DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_LOW_NON_BLOCKABLE = 50;

    /**
     * Client-side messages including bulk transfers, so this should have a fairly low limit.
     */
    private static final int DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_LOW_BLOCKABLE = 10;

    // individual parameters' default values below; these are applied to the fields on initialization

    /**
     * See {@link #getMaxBufferedIncomingMessagesPerSession()} JavaDoc.
     */
    private static final int DEFAULT_MAX_BUFFERED_INCOMING_MESSAGES_PER_SESSION = 3; // arbitrary; candidate for benchmarked optimization

    // see field getter JavaDoc for description
    private static final int DEFAULT_HANDSHAKE_RESPONSE_TIMEOUT_MSEC = 3000; // NOTE: raised in 10.2.4 2000 -> 3000

    // see field getter JavaDoc for description
    private static final int DEFAULT_HEARTBEAT_SERVER_TO_CLIENT_SEND_INTERVAL_AVERAGE_MSEC = 30 * 1000; // NOTE: raised in 10.2.3 20->30

    // see field getter JavaDoc for description
    private static final int DEFAULT_HEARTBEAT_SERVER_TO_CLIENT_SEND_INTERVAL_SPREAD_MSEC = 2 * 1000;

    // see field getter JavaDoc for description
    private static final int DEFAULT_HEARTBEAT_RESPONSE_TIME_WARNING_THRESHOLD_MSEC = 5000;

    // see field getter JavaDoc for description
    private static final int DEFAULT_CHANNEL_REQUEST_ROUNDTRIP_TIMEOUT = 30 * 1000; // raised in 10.2.3 10->20, 10.2.4 ->30

    // see field getter JavaDoc for description
    private static final int DEFAULT_DOCUMENTATION_REQUEST_ROUNDTRIP_TIMEOUT = 30 * 1000; // raised in 10.2.3 10->20, 10.2.4 ->30

    // start of the fields holding the "live" values

    private int maxBufferedIncomingMessagesPerSession = DEFAULT_MAX_BUFFERED_INCOMING_MESSAGES_PER_SESSION;

    private int handshakeResponseTimeout = DEFAULT_HANDSHAKE_RESPONSE_TIMEOUT_MSEC;

    private int heartbeatServerToClientSendIntervalAverage = DEFAULT_HEARTBEAT_SERVER_TO_CLIENT_SEND_INTERVAL_AVERAGE_MSEC;

    private int heartbeatServerToClientSendIntervalSpread = DEFAULT_HEARTBEAT_SERVER_TO_CLIENT_SEND_INTERVAL_SPREAD_MSEC;

    private int heartbeatResponseTimeWarningThreshold = DEFAULT_HEARTBEAT_RESPONSE_TIME_WARNING_THRESHOLD_MSEC;

    // unlike single parameters, the default values for this map are applied in the Builder's constructor.
    private Map<MessageBlockPriority, Integer> maxBufferedMessagesPerPriority = new HashMap<>();

    /**
     * A simple builder providing setter access for configuration values. Currently only intended for unit/integration tests.
     *
     * @author Robert Mischke
     */
    public static final class Builder {

        private UplinkProtocolConfiguration instance = new UplinkProtocolConfiguration();

        private Builder() {
            // set priority queue limits here as this would be cumbersome in the map field's initialization
            setMaxBufferedMessagesForPriority(MessageBlockPriority.HIGH,
                DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_HIGH);
            setMaxBufferedMessagesForPriority(MessageBlockPriority.DEFAULT,
                DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_DEFAULT);
            setMaxBufferedMessagesForPriority(MessageBlockPriority.FORWARDING,
                DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_LOW_BLACK_BOX_FORWARDING);
            setMaxBufferedMessagesForPriority(MessageBlockPriority.LOW_NON_BLOCKABLE,
                DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_LOW_NON_BLOCKABLE);
            setMaxBufferedMessagesForPriority(MessageBlockPriority.LOW_BLOCKABLE,
                DEFAULT_MAX_BUFFERED_MESSAGES_FOR_PRIORITY_LOW_BLOCKABLE);
        }

        public UplinkProtocolConfiguration build() {

            // convert the internal (mutable) instance into the return value to be set as the new configuration

            // clear the internal field to fail-fast on further modification
            UplinkProtocolConfiguration temp = instance;
            instance = null;

            // make all collections immutable to prevent accidents
            temp.maxBufferedMessagesPerPriority = Collections.unmodifiableMap(temp.maxBufferedMessagesPerPriority);

            return temp;
        }

        public Builder setHandshakeResponseTimeout(int handshakeResponseTimeout) {
            instance.handshakeResponseTimeout = handshakeResponseTimeout;
            return this;
        }

        public Builder setHeartbeatServerToClientSendIntervalAverage(int heartbeatServerToClientSendIntervalAverage) {
            instance.heartbeatServerToClientSendIntervalAverage = heartbeatServerToClientSendIntervalAverage;
            return this;
        }

        public Builder setHeartbeatServerToClientSendIntervalSpread(int heartbeatServerToClientSendIntervalSpread) {
            instance.heartbeatServerToClientSendIntervalSpread = heartbeatServerToClientSendIntervalSpread;
            return this;
        }

        public Builder setHeartbeatResponseTimeWarningThreshold(int heartbeatResponseTimeWarningThreshold) {
            instance.heartbeatResponseTimeWarningThreshold = heartbeatResponseTimeWarningThreshold;
            return this;
        }

        public Builder setMaxBufferedMessagesForPriority(MessageBlockPriority priority, int maxMessages) {
            instance.maxBufferedMessagesPerPriority.put(priority, maxMessages);
            return this;
        }

        public Builder setMaxBufferedIncomingMessagesPerSession(int maxBufferedIncomingMessagesPerSession) {
            instance.maxBufferedIncomingMessagesPerSession = maxBufferedIncomingMessagesPerSession;
            return this;
        }

    }

    public static UplinkProtocolConfiguration getCurrent() {
        return sharedCurrentConfiguration;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static void override(Builder builder) {
        sharedCurrentConfiguration = builder.build();
    }

    public static void resetToDefaults() {
        sharedCurrentConfiguration = DEFAULT_CONFIGURATION;
    }

    /**
     * @return The maximum time that each side of the handshake exchange should wait for the other side's response (in msec).
     */
    public int getHandshakeResponseTimeout() {
        return handshakeResponseTimeout;
    }

    /**
     * @return The average interval between sending heartbeat message from server to client (in msec); currently, this is also the implicit
     *         timeout for the response, as response reception is currently checked before sending the next heartbeat for simplicity.
     * 
     *         TODO split these parameters so they can be adjusted independently?
     */
    public int getHeartbeatServerToClientSendIntervalAverage() {
        return heartbeatServerToClientSendIntervalAverage;
    }

    /**
     * @return The variation width of the heartbeat interval (in msec); added to avoid heartbeat events all arriving at the same time when
     *         started in sync.
     */
    public int getHeartbeatServerToClientSendIntervalSpread() {
        return heartbeatServerToClientSendIntervalSpread;
    }

    /**
     * @return If the time between sending a heartbeat and receiving the related response exceeds this time (in msec), a warning is logged.
     */
    public int getHeartbeatResponseTimeWarningThreshold() {
        return heartbeatResponseTimeWarningThreshold;
    }

    /**
     * @return The maximum time (in msec) to wait for a remote response to a channel initiation request.
     */
    public int getChannelRequestRoundtripTimeout() {
        return DEFAULT_CHANNEL_REQUEST_ROUNDTRIP_TIMEOUT; // allow parameterization later without changing callers
    }

    /**
     * @return The maximum time (in msec) to wait for a remote response to a tool documentation transfer request.
     */
    public int getToolDocumentationRequestRoundtripTimeout() {
        return DEFAULT_DOCUMENTATION_REQUEST_ROUNDTRIP_TIMEOUT;
    }

    /**
     * @return The priority-to-queueSize map for {@link BoundedMessageBlockPrioritizer}.
     */
    public Map<MessageBlockPriority, Integer> getMaxBufferedOutgoingMessagesPerSessionAndPriority() {
        return maxBufferedMessagesPerPriority;
    }

    /**
     * The number of {@link MessageBlock#}s to buffer on the server/relay side between reading from the network input stream (so
     * effectively, from the OS's incoming network buffers) and their processing by the central thread pool. This limiting was introduced in
     * 10.2.4 to cause intentional TCP backpressure to the message sender if message processing is blocked at the relay, typically because
     * of backpressure from the receiving end of a file transfer.
     * 
     * @return the maximum number of {@link MessageBlock}s to queue
     */
    public int getMaxBufferedIncomingMessagesPerSession() {
        return maxBufferedIncomingMessagesPerSession;
    }
}
