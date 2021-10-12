/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.relay.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.common.internal.UplinkProtocolMessageConverter;
import de.rcenvironment.core.communication.uplink.entities.ChannelCreationRequest;
import de.rcenvironment.core.communication.uplink.entities.ChannelCreationResponse;
import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.network.channel.internal.AbstractChannelEndpoint;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkEndpointService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSession;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * Default {@link ServerSideUplinkEndpointService} implementation.
 *
 * @author Robert Mischke
 */
@Component
public class ServerSideUplinkEndpointServiceImpl implements ServerSideUplinkEndpointService {

    private static final String FROM_SESSION = " from session "; // quick Checkstyle fix

    private final UplinkProtocolMessageConverter messageConverter = new UplinkProtocolMessageConverter("server endpoint");

    private final AtomicInteger sessionCounter = new AtomicInteger(0);

    private final Set<ServerSideUplinkSession> activeSessions = new HashSet<>();

    private final Map<String, MessageBlock> cachedToolDescriptorUpdatesByDestinationId = new HashMap<>();

    private final Map<ServerSideUplinkSession, ServerSideSessionHandler> sessionHandlers = new HashMap<>();

    // injected
    private ConcurrencyUtilsFactory concurrencyUtilsFactory;

    /**
     * Synchronization guard for {@link #activeSessions}, {@link #cachedToolDescriptorUpdatesByDestinationId}.
     */
    private final Object crossSessionStateLock = new Object();

    private final Log log = LogFactory.getLog(getClass());

    private final AtomicLong channelIdCounter = new AtomicLong(0);

    private final Map<Long, ChannelData> channelDataMap = new HashMap<>();

    private final Map<String, ServerSideUplinkSession> namespacesToSessionsMap = new HashMap<>();

    /**
     * A class for holding all endpoint-related session state and performing message dispatch. As it has a one-to-one relation with
     * {@link ServerSideUplinkSession}, this could have theoretically be covered by {@link ServerSideUplinkSession} itself, too. This
     * functionality was split off in the hope of better separation of concerns, at the price of a little more mapping overhead.
     *
     * @author Robert Mischke
     */
    private final class ServerSideSessionHandler {

        private final ServerSideUplinkSession session;

        private final DefaultChannelRelayEndpoint defaultChannelEndpoint;

        private ServerSideSessionHandler(ServerSideUplinkSession session) {
            this.session = session;
            this.defaultChannelEndpoint = new DefaultChannelRelayEndpoint(session);
        }

        public void processMessageblock(long channelId, MessageBlock messageBlock) {
            try {
                if (channelId == UplinkProtocolConstants.DEFAULT_CHANNEL_ID) {
                    defaultChannelEndpoint.processMessage(messageBlock);
                } else {
                    forwardChannelMessage(session, channelId, messageBlock);
                }
            } catch (IOException e) {
                // TODO improve log output; actually close the session/channel
                log.error("Error while processing a received message", e);
            }
        }
    }

    /**
     * Encapsulates all server-side data about a message channel, most importantly which client sessions are the initiator and destination
     * of this channel. These two clients are also the only ones that are allowed to send messages to the channel.
     *
     * @author Robert Mischke
     */
    private final class ChannelData {

        private ServerSideUplinkSession initiatorSession;

        private ServerSideUplinkSession destinationSession;

        private Long channelId;

        private ChannelData(ServerSideUplinkSession initiatorSession, ServerSideUplinkSession destinationSession, Long sourceChannelId) {
            this.initiatorSession = initiatorSession;
            this.destinationSession = destinationSession;
            this.channelId = sourceChannelId;
        }

        public ServerSideUplinkSession getInitiatorSession() {
            return initiatorSession;
        }

        public ServerSideUplinkSession getDestinationSession() {
            return destinationSession;
        }

        public Long getChannelId() {
            return channelId;
        }

    }

    /**
     * The relay-side end of a default channel. Default channels are used for general communication, for example the publication of tool
     * descriptors and the management of non-default channels.
     * <p>
     * This implementation is realized as a nested class as this simplifies access to state and methods that are by their nature shared
     * across sessions, most notably cached tool descriptor updates.
     *
     * @author Robert Mischke
     */
    private final class DefaultChannelRelayEndpoint extends AbstractChannelEndpoint {

        // the number of times to attempt finding an active session for an incoming request's destination;
        // mostly to prevent (semantic) race conditions in automated setups
        private static final int DEFAULT_CHANNEL_MATCH_ATTEMPTS = 5;

        // the time to wait between attempts; this is mostly relevant for automated/scripted setups, so retry fairly quickly
        private static final int DEFAULT_CHANNEL_MATCH_RETRY_INTERVAL = 500;

        private final ServerSideUplinkSession initiatingSession;

        private DefaultChannelRelayEndpoint(ServerSideUplinkSession session) {
            super(session, session.getLocalSessionId(), UplinkProtocolConstants.DEFAULT_CHANNEL_ID);
            this.initiatingSession = session;
        }

        @Override
        protected boolean processMessageInternal(MessageBlock messageBlock) throws IOException {

            final MessageType messageType = messageBlock.getType();
            switch (messageType) {
            case TOOL_DESCRIPTOR_LIST_UPDATE:
                return handleToolDescriptorListUpdate(messageBlock);
            case CHANNEL_INIT:
                return handleChannelInit(messageBlock);
            case CHANNEL_INIT_RESPONSE:
                return handleChannelInitResponse(messageBlock);
            default:
                // development behavior for all remaining messages: echo them back to the client
                log.debug(
                    "Received other message from client, mirroring back (DEVELOPMENT ONLY): "
                        + new String(messageBlock.getData(), Charsets.UTF_8));
                enqueueMessageBlockForSending(messageBlock, MessageBlockPriority.DEFAULT, true);
                return true;
            }
        }

        @Override
        public void dispose() {}

        private boolean handleChannelInit(MessageBlock messageBlock) throws ProtocolException, IOException {
            // decode the request
            ChannelCreationRequest request = messageConverter.decodeChannelCreationRequest(messageBlock);
            final String channelType = request.getType();

            // determine the session responsible for the given destination id, with a brief time window for retrying
            Optional<ServerSideUplinkSession> optionalDestinationSession = findSessionForDestinationIdWithRetry(request.getDestinationId(),
                DEFAULT_CHANNEL_MATCH_ATTEMPTS, DEFAULT_CHANNEL_MATCH_RETRY_INTERVAL);

            if (!optionalDestinationSession.isPresent()) {
                log.warn("Received a channel creation request for destination id '" + request.getDestinationId()
                    + "', but there was no client session matching its destination prefix");
                ChannelCreationResponse failureResponse =
                    new ChannelCreationResponse(UplinkProtocolConstants.UNDEFINED_CHANNEL_ID, request.getRequestId(), false);
                enqueueMessageBlockForSending(messageConverter.encodeChannelCreationResponse(failureResponse),
                    MessageBlockPriority.CHANNEL_INITIATION, false);
                return true;
            }
            final ServerSideUplinkSession destinationSession = optionalDestinationSession.get();
            if (destinationSession == initiatingSession) {
                log.warn("Received a request to create a channel where both endpoints are session " + initiatingSession
                    + "; this is not allowed");
                ChannelCreationResponse failureResponse =
                    new ChannelCreationResponse(UplinkProtocolConstants.UNDEFINED_CHANNEL_ID, request.getRequestId(), false);
                enqueueMessageBlockForSending(messageConverter.encodeChannelCreationResponse(failureResponse),
                    MessageBlockPriority.CHANNEL_INITIATION, false);
                return true;
            }
            // optimistically register the channel; if the destination session's client refuses, it will be discarded immediately
            Long channelId = createAndRegisterChannel(initiatingSession, destinationSession, channelType);
            // compose the channel offer to the destination client; note that the client's "request id" must be forwarded
            ChannelCreationRequest channelOffer =
                new ChannelCreationRequest(request.getType(), request.getDestinationId(), channelId, request.getRequestId());
            log.debug("Forwarding a channel request from session " + initiatingSession + " to session "
                + destinationSession + "; assigned channel id " + channelId);
            // then forward this message to the destination client
            // parameter "false": if the receiver does not process requests fast enough, terminate its session
            // note that this allows session DoS, as other clients trigger these requests, but the source is easily identified;
            // if this approach triggers session terminations by accident, switching to an event-sourcing-based approach (instead of active
            // message pushing) should handle this more gracefully, but this should not be necessary for now -- misc_ro
            destinationSession.enqueueMessageBlockForSending(UplinkProtocolConstants.DEFAULT_CHANNEL_ID,
                messageConverter.encodeChannelCreationRequest(channelOffer), MessageBlockPriority.CHANNEL_INITIATION, false);
            return true;
        }

        private boolean handleChannelInitResponse(MessageBlock messageBlock) throws IOException {
            // decode the request
            ChannelCreationResponse receivedResponse = messageConverter.decodeChannelCreationResponse(messageBlock);
            final long channelId = receivedResponse.getChannelId();
            ChannelData channelData = channelDataMap.get(channelId);
            // security check: is the channel id consistent?
            if (channelData.getDestinationSession() != initiatingSession) {
                log.error("Received a channel creation response from a different session (" + initiatingSession
                    + ") than the expected one (" + channelData.getDestinationSession()
                    + "); ignoring this message");
                // TODO close that session?
                return true;
            }
            log.debug("Forwarding a channel acceptance message for channel " + channelId + FROM_SESSION
                + initiatingSession + " to the initiating session "
                + channelData.getInitiatorSession());
            // generate the forwarded response
            ChannelCreationResponse forwardedResponse = new ChannelCreationResponse(channelId, receivedResponse.getRequestId(), true);
            // send it back to the channel's initiator
            // parameter "false": if the receiver does not process responses fast enough, terminate its session
            channelData.getInitiatorSession()
                .enqueueMessageBlockForSending(UplinkProtocolConstants.DEFAULT_CHANNEL_ID,
                    messageConverter.encodeChannelCreationResponse(forwardedResponse), MessageBlockPriority.CHANNEL_INITIATION, false);
            return true;
        }

        private boolean handleToolDescriptorListUpdate(MessageBlock messageBlock) throws IOException {
            final ToolDescriptorListUpdate update = messageConverter.decodeToolDescriptorListUpdate(messageBlock);
            // TODO security: validate whether this session is authorized to send an update for the contained destination id
            processIncomingToolDescriptorUpdate(update, initiatingSession, messageBlock);
            return true;
        }

    }

    @Override
    public String assignSessionId(ServerSideUplinkSession session) {
        return "s" + sessionCounter.incrementAndGet();
    }

    @Override
    public void setSessionActiveState(ServerSideUplinkSession session, boolean active) {
        log.debug(StringUtils.format("[%s] Setting active state of session to %s", session.getLogDescriptor(), active));
        synchronized (crossSessionStateLock) {
            if (active) {
                activeSessions.add(session);
                sessionHandlers.put(session, new ServerSideSessionHandler(session));
                try {
                    provideCachedToolDescriptorsToNewSession(session);
                } catch (IOException e) {
                    log.warn("Error while sending cached tool descriptors to new session " + session);
                }
            } else {
                // Get the namespace id assigned to that session, if there is one. Note that such namespace assignments are kept in that
                // object forever, i.e. they are not reset when the namespace is "released" by calling #releaseNamespaceId(). -- misc_ro
                final Optional<String> assignedNamespaceId = session.getAssignedNamespaceIdIfAvailable();
                // if the session was assigned a namespace, perform a consistency check: the namespace id should have already been released
                if (assignedNamespaceId.isPresent()) {
                    final ServerSideUplinkSession mappedSessionForNamespaceId = namespacesToSessionsMap.get(assignedNamespaceId.get());
                    if (mappedSessionForNamespaceId == session) {
                        // abnormal case; should not happen
                        log.warn("[" + session + "] Session still had a namespace assigned when resetting its 'active' state");
                        // best-effort removal, as we are in an inconsistent state already
                        releaseNamespaceId(assignedNamespaceId.get(), session);
                    } else if (mappedSessionForNamespaceId != null) {
                        // not strictly an error, but highly unusual, so log a warning
                        log.warn("Unusual state: Namespace id " + assignedNamespaceId.get() + " has been assigned to another session ("
                            + mappedSessionForNamespaceId + ") before the previous one (" + session + ") has been deactivated");
                    }
                    // if none of these clauses matched, the map had no entry (null result) - this is the expected typical case
                }

                activeSessions.remove(session);
                sessionHandlers.remove(session); // note: this requires active disposal of delayed messages coming in from that session
                deregisterCachedToolDescriptorsOfClosedSession(session); // note: should be below removing the session from the active set
            }
        }
    }

    @Override
    public void onMessageBlock(ServerSideUplinkSession receivingSession, long channelId, MessageBlock messageBlock)
        throws ProtocolException {
        final ServerSideSessionHandler sessionHandler;
        synchronized (crossSessionStateLock) {
            sessionHandler = sessionHandlers.get(receivingSession);
        }
        if (sessionHandler == null) {
            log.debug("Discarding a message of type " + messageBlock.getType() + " that arrived after session "
                + receivingSession + " was deactivated");
            return;
        }
        sessionHandler.processMessageblock(channelId, messageBlock);
    }

    @Override
    public boolean attemptToAssignNamespaceId(String namespaceId, ServerSideUplinkSession newSession) {
        synchronized (namespacesToSessionsMap) {
            final ServerSideUplinkSession existingSession = namespacesToSessionsMap.get(namespaceId);
            if (existingSession != null) {
                log.warn("Refusing session " + newSession + " from using namespace " + namespaceId + " as it is already in use by session "
                    + existingSession);
                return false;
            }

            namespacesToSessionsMap.put(namespaceId, newSession);
            log.debug(StringUtils.format("[%s] Assigning namespace '%s'", newSession.getLogDescriptor(), namespaceId));
            return true;
        }
    }

    @Override
    public void releaseNamespaceId(String namespaceId, ServerSideUplinkSession session) {
        synchronized (namespacesToSessionsMap) {
            final ServerSideUplinkSession existingSession = namespacesToSessionsMap.get(namespaceId);
            if (existingSession == null) {
                // this can be regularly happen in cleanup/safeguard code; eliminate this message if it is logged too often
                log.debug("Ignoring request to release namespace " + namespaceId + FROM_SESSION + session
                    + " as it is not registered for any session");
                return;
            }
            if (existingSession != session) {
                log.warn("Ignoring request to release namespace " + namespaceId + FROM_SESSION + session + " as it is bound to session "
                    + existingSession + " instead");
                return;
            }
            namespacesToSessionsMap.remove(namespaceId);
            log.debug(StringUtils.format("[%s] Releasing namespace '%s'", session.getLogDescriptor(), namespaceId));
        }
    }

    /**
     * OSGi-DS injection method.
     * 
     * @param newInstance the service instance to inject
     */
    @Reference
    public void bindConcurrencyUtilsFactory(ConcurrencyUtilsFactory newInstance) {
        this.concurrencyUtilsFactory = newInstance;
    }

    private void processIncomingToolDescriptorUpdate(ToolDescriptorListUpdate update, ServerSideUplinkSession sourceSession,
        MessageBlock messageBlock) throws IOException {
        synchronized (crossSessionStateLock) {
            log.debug(
                "Forwarding a tool descriptor update from session " + sourceSession + " to " + activeSessions.size()
                    + " session(s)");
            if (!update.getToolDescriptors().isEmpty()) {
                // non-empty update -> cache it
                cachedToolDescriptorUpdatesByDestinationId.put(update.getDestinationId(), messageBlock);
            } else {
                // empty update -> remove cache entry
                final MessageBlock previousEntry = cachedToolDescriptorUpdatesByDestinationId.remove(update.getDestinationId());
                // consistency check: every empty update should have been preceded by a non-empty update
                if (previousEntry != null) {
                    log.debug("Removed cached tool descriptor list for destination id '" + update.getDestinationId()
                        + "' as its session sent an empty list update");
                } else {
                    // currently, this should not happen; there may be reasons to accept this in the future, though
                    log.warn("Received an empty tool descriptor list update for destination id '" + update.getDestinationId()
                        + "', but there was no previous cached entry to remove?");
                }
            }
            // iterate over a *copy* of the current list of active sessions, as send operations may fail and cause sessions to be removed
            for (ServerSideUplinkSession session : getIterationSafeListOfActiveSessions()) {
                if (session == sourceSession) {
                    continue; // skip the session that published this update
                }
                session.enqueueMessageBlockForSending(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, messageBlock,
                    MessageBlockPriority.TOOL_DESCRIPTOR_UPDATES, false);
            }
        }
    }

    private void provideCachedToolDescriptorsToNewSession(ServerSideUplinkSession session) throws IOException {
        synchronized (crossSessionStateLock) {
            for (Entry<String, MessageBlock> entry : cachedToolDescriptorUpdatesByDestinationId.entrySet()) {
                final String destinationId = entry.getKey();
                final MessageBlock cachedMessageBlock = entry.getValue();
                log.debug("Forwarding a cached tool descriptor list update for destination " + destinationId + " to new session "
                    + session);
                // false = terminate on queue congestion instead of blocking
                session.enqueueMessageBlockForSending(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, cachedMessageBlock,
                    MessageBlockPriority.TOOL_DESCRIPTOR_UPDATES, false);
            }
        }
    }

    private void deregisterCachedToolDescriptorsOfClosedSession(ServerSideUplinkSession closedSession) {
        final String destinationIdPrefix = closedSession.getDestinationIdPrefix();
        List<String> destinationIdsToRemove = new ArrayList<>();
        synchronized (crossSessionStateLock) {
            // identify destination ids belonging to the closed session
            for (Entry<String, MessageBlock> entry : cachedToolDescriptorUpdatesByDestinationId.entrySet()) {
                final String destinationId = entry.getKey();
                if (destinationId.startsWith(destinationIdPrefix)) {
                    destinationIdsToRemove.add(destinationId);
                }
            }
            for (String destinationId : destinationIdsToRemove) {
                log.debug("Removing a cached tool descriptor list update for destination " + destinationId + " as its session "
                    + closedSession + " is closing");

                // remove this entry from the list of cached updates
                cachedToolDescriptorUpdatesByDestinationId.remove(destinationId);

                // create a MessageBlock with an empty tool list for this destination id
                MessageBlock emptyToolDescriptorListUpdateMessageBlock;
                try {
                    emptyToolDescriptorListUpdateMessageBlock =
                        messageConverter.encodeToolDescriptorListUpdate(new ToolDescriptorListUpdate(destinationId, "", new ArrayList<>()));
                } catch (ProtocolException e1) {
                    throw new IllegalStateException(); // should never happen
                }

                // iterate over all other sessions and send the message block to each
                for (ServerSideUplinkSession sessionToInform : getIterationSafeListOfActiveSessions()) {
                    if (sessionToInform == closedSession) {
                        throw new IllegalStateException("The closing session should have been unregisterd at this point");
                    }
                    try {
                        // parameter "false": if the receiver does not process requests fast enough, terminate its session
                        // note that this allows session DoS, as other clients trigger these requests, but the source is easily identified
                        sessionToInform.enqueueMessageBlockForSending(UplinkProtocolConstants.DEFAULT_CHANNEL_ID,
                            emptyToolDescriptorListUpdateMessageBlock, MessageBlockPriority.TOOL_DESCRIPTOR_UPDATES, false);
                    } catch (IOException e) {
                        log.warn("Error while sending a tool removal update to session " + sessionToInform);
                    }
                }
            }
        }
    }

    private Long createAndRegisterChannel(final ServerSideUplinkSession initiatingSession,
        ServerSideUplinkSession destinationSession, final String channelType) {
        Long channelId = generateChannelId(channelType);
        if (initiatingSession == destinationSession) {
            throw new IllegalArgumentException(); // sanity check; this shoudl have been caught before
        }
        ChannelData channelData = new ChannelData(initiatingSession, destinationSession, channelId);
        synchronized (channelDataMap) {
            channelDataMap.put(channelId, channelData);
        }
        return channelId;
    }

    private Optional<ServerSideUplinkSession> findSessionForDestinationIdWithRetry(String destinationId, int numAttempts,
        int intervalMsec) {
        Optional<ServerSideUplinkSession> result;
        if (numAttempts < 1) {
            throw new IllegalArgumentException();
        }
        int attempt = 0;
        do {
            attempt++;
            if (attempt > 1) {
                log.debug(StringUtils.format("Waiting %d msec until starting attempt %d at resolving destination id '%s'",
                    intervalMsec, attempt, destinationId));
                try {
                    Thread.sleep(intervalMsec);
                } catch (InterruptedException e) {
                    log.debug("Interrupted while waiting for retry; most likely, the application is shutting down");
                    return Optional.empty();
                }
            }
            result = findSessionForDestinationId(destinationId);

        } while (!result.isPresent() && attempt < numAttempts);
        return result;
    }

    /**
     * Returns a copy of the list of active sessions to allow safe iteration over this list if operations within the loop may modify the
     * list of active sessions. Most notably, this includes calls to enqueueMessageBlockForSending() in non-blocking mode, as sending may
     * fail on a full queue, and cause the related session to be terminated.
     * <p>
     * Technically, the returned list does not need to be processed with the {@link #crossSessionStateLock} held, although in most cases
     * this is probably still a good idea.
     * 
     * @return a detached copy of the {@link #activeSessions} list
     */
    private List<ServerSideUplinkSession> getIterationSafeListOfActiveSessions() {
        synchronized (crossSessionStateLock) {
            return new ArrayList<>(activeSessions);
        }
    }

    private Optional<ServerSideUplinkSession> findSessionForDestinationId(String destinationId) {
        // TODO could be optimized by extracting the session id substring and map lookup
        synchronized (crossSessionStateLock) {
            // not using getIterationSafeListOfActiveSessions() as the list is not being modified
            for (ServerSideUplinkSession session : activeSessions) {
                // note: for this to work and be secure, it is important that the assigned prefixes can never be a prefix of each other,
                // for example by making them end with a reserved character (currently "/") that can never occur in the prefix id itself
                if (destinationId.startsWith(session.getDestinationIdPrefix())) {
                    return Optional.of(session);
                }
            }
            log.debug("Found no match for destination id '" + destinationId + "' among active sessions "
                + Arrays.toString(activeSessions.toArray()));
        }
        return Optional.empty();
    }

    private Long generateChannelId(String type) {
        final Long id = Long.valueOf(channelIdCounter.incrementAndGet());
        log.debug(StringUtils.format("Creating channel %s (type '%s')", id, type));
        return id;
    }

    private void forwardChannelMessage(ServerSideUplinkSession sourceSession, long channelId, MessageBlock messageBlock)
        throws IOException {
        final ChannelData channelData;
        synchronized (channelDataMap) {
            channelData = channelDataMap.get(channelId);
        }
        if (channelData == null) {
            log.warn(
                "Received a message for non-existing channel " + channelId + "; it may have been closed in the meantime");
            // TODO close this channel
            return;
        }
        if (sourceSession == channelData.getInitiatorSession()) {
            /*
             * Note: Both options for the 4th parameter (terminating the receiver's session vs. blocking) are currently unsatisfactory here.
             * Blocking does not propagate backpressure to the sender, so it can cause the message receive queue to grow without limitation;
             * but terminating the receiver's session can result in a fast data source sending more data than the receiver can handle, and
             * terminate its session. Blocking on the caller side as it is, however, can block the sender's heartbeat messages from getting
             * processed, and may terminate this side's session.
             * 
             * Ideally, there would be an active flow control mechanism to block the sender to a rate matching the receiver's capacity
             * instead. This is mitigated for now by the fact that the relay's network capacity will often serve as a common limit on
             * transfer speed, and that data transfers are always requested or agreed on by both sides. The latter causes sender and
             * receiver to "synchronize" at the end of each transfer. For large files, however, it is still possible for a fast sender to
             * exhaust the receiver's available bandwidth, and potentially terminate its session this way. To mitigate this, the number of
             * queued incoming message was also limited in 10.2.4 to allow backpressure to reach the sender's TCP connection. (See
             * UplinkProtocolConfiguration#getMaxBufferedIncomingMessagesPerSession()).
             * 
             * As of 10.2.4, this approach still an improvement over the previous situation, as it protects the relay's availability at the
             * cost of terminating individual sessions. If this causes problems in normal real-life operation, the fairly arbitrary queue
             * limits should be tweaked first. Another approach might be per-channel termination (instead of terminating the whole session),
             * but this is not supported in the current protocol yet either.
             * 
             * Regardless of the chosen approach, some sort of protocol-level solution (e.g. active flow control, channel termination)
             * should be implemented in 11.0.0. -- R. Mischke, Jul 2021
             */

            // parameter "true": for now, block if the forwarding queue is full
            channelData.getDestinationSession().enqueueMessageBlockForSending(channelId, messageBlock,
                MessageBlockPriority.FORWARDING, true);
        } else if (sourceSession == channelData.getDestinationSession()) {
            // see comments above
            channelData.getInitiatorSession().enqueueMessageBlockForSending(channelId, messageBlock,
                MessageBlockPriority.FORWARDING, true);
        } else {
            log.error(
                "Detected an attempt to send a message block to an unauthorized channel: source session "
                    + sourceSession
                    + ", message type " + messageBlock.getType() + ",  channel id " + channelId);
            // TODO also close that session?
            return;
        }
    }

    /**
     * Introspection method for unit/integration tests.
     * 
     * @param namespace the namespace to check
     * @return true if the namespace is assigned to a session, and therefore blocked for concurrent usage
     */
    public boolean isNamespaceAssigned(String namespace) {
        synchronized (namespacesToSessionsMap) {
            return namespacesToSessionsMap.containsKey(namespace);
        }
    }

}
