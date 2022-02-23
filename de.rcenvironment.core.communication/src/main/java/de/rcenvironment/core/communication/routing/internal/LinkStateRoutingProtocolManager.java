/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListenerAdapter;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandlerMap;
import de.rcenvironment.core.communication.messaging.direct.api.DirectMessagingSender;
import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.messaging.internal.NetworkRequestUtils;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkResponseHandler;
import de.rcenvironment.core.communication.protocol.MessageMetaData;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of a link state based routing table.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public class LinkStateRoutingProtocolManager {

    private static final int DEFAULT_TIME_TO_LIVE = 200;

    /**
     * Delegating adapter for {@link MessageChannelLifecycleListener} events.
     * 
     * @author Robert Mischke
     */
    private class MessageChannelLifecycleHandler extends MessageChannelLifecycleListenerAdapter {

        @Override
        public void onOutgoingChannelEstablished(final MessageChannel connection) {
            handleOutgoingChannelEstablished(connection);
        }

        @Override
        public void onOutgoingChannelTerminated(MessageChannel connection) {
            handleOutgoingChannelTerminated(connection);
        }
    }

    /**
     * Handler for incoming LSA (link state advertisement) requests.
     * 
     * @author Robert Mischke
     */
    private static class LSANetworkRequestHandler implements NetworkRequestHandler {

        private LinkStateRoutingProtocolManager protocolManager;

        LSANetworkRequestHandler(LinkStateRoutingProtocolManager protocolManager) {
            this.protocolManager = protocolManager;
        }

        @Override
        public NetworkResponse handleRequest(NetworkRequest request, InstanceNodeSessionId sourceId) throws InternalMessagingException {
            Serializable messageContent = NetworkRequestUtils.deserializeWithExceptionHandling(request);
            Serializable responseBody;
            if (messageContent instanceof LinkStateAdvertisementBatch) {
                responseBody = protocolManager.handleReceivedInitialLSABatch(messageContent);
            } else {
                // TODO @7.0 should not be in use anymore; remove
                responseBody = protocolManager.handleSingleLinkStateAdvertisement(messageContent, request.accessRawMetaData());
            }
            byte[] responseBodyBytes = MessageUtils.serializeSafeObject(responseBody);
            return NetworkResponseFactory.generateSuccessResponse(request, responseBodyBytes);
        }
    }

    private static int timeToLive = DEFAULT_TIME_TO_LIVE;

    private static final int MESSAGE_BUFFER_SIZE = 50;

    private static final boolean DEBUG_DUMP_INITIAL_LSA_BATCHES = false;

    /**
     * TODO Enter comment.
     */
    public volatile boolean sendCompactLsaLists = false;

    private final Log log = LogFactory.getLog(getClass());

    private final TopologyMap topologyMap;

    private final NetworkRequestHandler networkRequestHandler;

    private final InitialNodeInformation ownNodeInformation;

    private final InstanceNodeSessionId ownNodeId;

    private final DirectMessagingSender directMessagingSender;

    private NetworkTopologyChangeListener topologyChangeListener;

    private final Map<String, Serializable> messageBuffer = new LinkedHashMap<String, Serializable>(MESSAGE_BUFFER_SIZE);

    private final NetworkStats networkStats;

    private final Map<String, MessageChannel> connectionsById = new HashMap<String, MessageChannel>();

    // private final LinkStateAdvertisementBatch lsaCache = new LinkStateAdvertisementBatch();

    // @Deprecated

    /**
     * Constructor needs to get services injected.
     * 
     * @param communicationService
     * @param platformService
     * @param changeListener listener for topology change events
     */
    public LinkStateRoutingProtocolManager(TopologyMap topologyMap, MessageChannelService connectionService,
        NetworkTopologyChangeListener changeListener) {
        this.topologyMap = topologyMap;
        this.networkRequestHandler = new LSANetworkRequestHandler(this);
        this.ownNodeInformation = topologyMap.getLocalNodeInformation();
        this.ownNodeId = ownNodeInformation.getInstanceNodeSessionId();
        this.directMessagingSender = connectionService;
        this.networkStats = new NetworkStats();
        connectionService.addChannelLifecycleListener(new MessageChannelLifecycleHandler());
        // initialize topology with self
        TopologyNode ownNode = topologyMap.addNode(ownNodeId);
        ownNode.setDisplayName(ownNodeInformation.getDisplayName());
        ownNode.setIsWorkflowHost(false); // not used anymore
        // initialize own sequence number
        ownNode.invalidateSequenceNumber();
        this.topologyChangeListener = changeListener;
        fireTopologyChangedListener();
    }

    public NetworkRequestHandlerMap getNetworkRequestHandlers() {
        return new NetworkRequestHandlerMap(ProtocolConstants.VALUE_MESSAGE_TYPE_LSA, networkRequestHandler);
    }

    /**
     * Broadcast the information that this node is shutting down, and will disappear from the network.
     * 
     * @throws CommunicationException The communication exception.
     */
    public void announceShutdown() throws CommunicationException {
        broadcastLsa(topologyMap.generateShutdownLSA());
    }

    /**
     * Test if a message with a given message id has been received recently.
     * 
     * @param messageId The id of the message.
     * @return A boolean.
     */
    public boolean messageReivedById(String messageId) {
        return messageBuffer.containsKey(messageId);
    }

    /**
     * Test if a message has recently been received that had a given content.
     * 
     * @param messageContent The content.
     * @return A boolean.
     */
    public boolean messageReivedByContent(Serializable messageContent) {
        return messageBuffer.containsValue(messageContent);
    }

    /**
     * This method is called, when the routing service receives an {@link LinkStateAdvertisement} from a remote instance.
     * 
     * @param messageContent The message content.
     * @param metaData The meta data.
     * @return an optional {@link Serializable} response or null
     */
    private Serializable handleSingleLinkStateAdvertisement(Serializable messageContent, Map<String, String> metaData) {

        boolean topologyChanged = false;

        synchronized (topologyMap) {

            if (!(messageContent instanceof LinkStateAdvertisement)) {
                throw new IllegalStateException("Received a non-LSA in handleLinkStateAdvertisement()");
            }

            LinkStateAdvertisement lsa = (LinkStateAdvertisement) messageContent;

            networkStats.incReceivedLSAs();
            // networkStats.incHopCountOfReceivedLSAs(MessageMetaData.wrap(metaData).getHopCount());

            // TODO review: currently not sent; see LSA batch handling above
            // if (LinkStateAdvertisement.REASON_STARTUP.equals(lsa.getReason())) {
            // }

            // if the received LSA was accepted
            if (topologyMap.update(lsa)) {

                topologyChanged = true;

                // TODO Dynamically adjust maximum time to live for LSAs
                networkStats.setMaxTimeToLive(getTimeToLive());

                // // fill cache
                // synchronized (lsaCache) {
                // lsaCache.put(lsa.getOwner(), lsa);
                // }

                broadcastLsa(lsa);

                // NetworkFormatter.nodeList(topologyMap));

                // synchronized (lsaCache) {
                // LinkStateAdvertisementCache clonedCache;
                // clonedCache = new LinkStateAdvertisementCache(lsaCache);
                // return clonedCache;
                // }
            } else {
                networkStats.incRejectedLSAs();
                networkStats.incHopCountOfRejectedLSAs(MessageMetaData.wrap(metaData).getHopCount());
                // send null response
            }
        }
        if (topologyChanged) {
            fireTopologyChangedListener();
        }
        return null;
    }

    /**
     * TODO Robert Mischke: Enter comment!
     * 
     * @param messageContent The message content.
     * @return The link state advertisement batch.
     */
    private Serializable handleReceivedInitialLSABatch(Serializable messageContent) {

        boolean topologyChanged = false;
        LinkStateAdvertisementBatch response;

        synchronized (topologyMap) {
            // sanity check
            if (!(messageContent instanceof LinkStateAdvertisementBatch)) {
                throw new IllegalStateException("Message content of wrong type.");
            }

            LinkStateAdvertisementBatch lsaCache = (LinkStateAdvertisementBatch) messageContent;

            if (DEBUG_DUMP_INITIAL_LSA_BATCHES) {
                // TODO add origin/sender information
                String dump = StringUtils.format("Processing LSA cache at %s (as incoming request):", ownNodeId);
                for (InstanceNodeSessionId id : lsaCache.keySet()) {
                    dump += "\n" + id + " -> " + lsaCache.get(id);
                }
                log.debug(dump);
            }

            LinkStateAdvertisementBatch lsaCacheNew = new LinkStateAdvertisementBatch();

            // TODO increment stats
            for (LinkStateAdvertisement lsa : lsaCache.values()) {
                if (topologyMap.update(lsa)) {
                    topologyChanged = true;
                    // update main cache
                    // lsaCache.put(lsa.getOwner(), lsa);
                    // lsaCacheNew.put(lsa.getOwner(), lsa);
                    broadcastLsa(lsa);
                }

            }

            // return lsaCache;
            response = topologyMap.generateLsaBatchOfAllNodes();
        }
        if (topologyChanged) {
            fireTopologyChangedListener();
        }
        return response;
    }

    /**
     * Handles the response received from a remote node in response to the inital {@link LinkStateAdvertisementBatch}.
     * 
     * @param messageContent The message content.
     * @return whether the received response caused a topology change
     */
    private boolean handleInitialLSABatchResponse(Serializable messageContent) {
        boolean topologyChanged = false;
        synchronized (topologyMap) {
            // sanity check
            if (!(messageContent instanceof LinkStateAdvertisementBatch)) {
                log.warn("Message content was of wrong type.");
                return false;
            }

            LinkStateAdvertisementBatch lsaCache = (LinkStateAdvertisementBatch) messageContent;

            if (DEBUG_DUMP_INITIAL_LSA_BATCHES) {
                // TODO add origin/sender information
                String dump = StringUtils.format("Processing LSA cache at %s (as incoming response):", ownNodeId);
                for (InstanceNodeSessionId id : lsaCache.keySet()) {
                    dump += "\n" + id + " -> " + lsaCache.get(id);
                }
                log.debug(dump);
            }

            // LinkStateAdvertisementCache lsaCacheNew = new LinkStateAdvertisementCache();

            for (LinkStateAdvertisement lsa : lsaCache.values()) {
                if (topologyMap.update(lsa)) {
                    topologyChanged = true;
                    // lsaCacheNew.put(lsa.getOwner(), lsa);
                    broadcastLsa(lsa);
                }

            }
        }
        return topologyChanged;
    }

    private void onOutgoingChannelHandshakeCompleted(MessageChannel connection, boolean topologyChanged) {
        broadcastNewLocalLSA();
        if (topologyChanged) {
            fireTopologyChangedListener();
        }
    }

    /**
     * Send link state advertisement of the own node.
     * 
     * @return The message id.
     */
    public String broadcastNewLocalLSA() {
        // extract fresh LSA from topology map
        // synchronized (lsaCache) {
        LinkStateAdvertisement ownLsa = topologyMap.generateNewLocalLSA();
        // lsaCache.put(ownNodeId, ownLsa);
        return broadcastLsa(ownLsa);
        // }
    }

    /**
     * Send a given LSA to all neighbors.
     * 
     * @param lsa
     * @throws CommunicationException
     * @return The message id.
     */
    private String broadcastLsa(LinkStateAdvertisement lsa) {

        byte[] lsaBytes = MessageUtils.serializeSafeObject(lsa);
        String messageId = "";

        // update metadata
        List<TopologyNode> neighbors = new ArrayList<TopologyNode>(topologyMap.getSuccessors());
        // Use a randomized list
        Collections.shuffle(neighbors);

        /*
         * Changed in 3.0.0: LSAs are now broadcast into all outgoing channels, instead of sending a routed message to each neighbor. The
         * rationale is that if there are parallel message channels, they exist either for intentional redundancy, or one or more of them
         * are stale/broken. In both cases, it is appropriate to send to all channels. - misc_ro, July 2013
         */
        Collection<TopologyLink> links = topologyMap.getAllOutgoingLinks(ownNodeId);

        // iterate over all neighbor nodes of the current node
        // NOTE: "links" is not threadsafe (can cause a ConcurrentModificationException); not fixing as this code is deprecated
        for (TopologyLink link : links) {

            networkStats.incSentLSAs();
            // networkStats.incHopCountOfSentLSAs(MessageMetaData.wrap(metaData).getHopCount());
            // non-routed broadcast -> no recipient
            NetworkRequest request =
                NetworkRequestFactory.createNetworkRequest(lsaBytes, ProtocolConstants.VALUE_MESSAGE_TYPE_LSA, ownNodeId, null);
            final String channelId = link.getConnectionId();
            directMessagingSender.sendDirectMessageAsync(request, connectionsById.get(channelId), new NetworkResponseHandler() {

                @Override
                public void onResponseAvailable(NetworkResponse response) {
                    if (!response.isSuccess()) {
                        // TODO add cause to log entry
                        log.warn("Failed to send LSA via channel " + channelId);
                    }
                }
            });
            // sendToNeighbor(request, neighbor.getNodeIdentifier());
        }

        return messageId;
    }

    /**
     * @param id a {@link MessageChannel}'s id
     * @return the associated {@link MessageChannel}
     */
    // FIXME move/replace
    public MessageChannel getMessageChannelById(String id) {
        MessageChannel connection = null;
        synchronized (connectionsById) {
            connection = connectionsById.get(id);
        }
        if (connection == null) {
            throw new IllegalStateException("No registered connection for connection id " + id);
        }
        return connection;
    }

    private TopologyLink registerNewConnection(MessageChannel connection) {
        String connectionId = connection.getChannelId();
        synchronized (connectionsById) {
            // consistency check: there should be no connection with the same id already
            if (connectionsById.get(connectionId) != null) {
                // consistency error
                throw new IllegalStateException("Existing connection found for connection id " + connectionId);
            }
            connectionsById.put(connectionId, connection);
            // LOGGER.debug(StringUtils.format("Registered new connection %s in node %s",
            // connection.toString(),
            // ownNodeInformation.getLogName()));

            // NOTE: this replaces the obsolete "pingNetworkContactPoint" method -- misc_ro
            InstanceNodeSessionId remoteNodeId = connection.getRemoteNodeInformation().getInstanceNodeSessionId();

            // TODO restore onCommunicationSuccess callback (via traffic listener?)
            // onCommunicationSuccess("", MetaDataWrapper.createEmpty().getInnerMap(), connection,
            // remoteNodeId);

            // update graph model
            topologyMap.addNode(remoteNodeId);

            if (topologyMap.hasLinkForConnection(connection.getChannelId())) {
                // unexpected state / consistency error
                throw new IllegalStateException("Found existing link for new connection " + connectionId);
            }

            // add newly discovered link to network model
            TopologyLink newLink = topologyMap.addLink(getOwner(), remoteNodeId, connection.getChannelId());

            return newLink;
        }
    }

    private void handleOutgoingChannelEstablished(final MessageChannel connection) {
        synchronized (topologyMap) {

            log.debug("Registering connection " + connection.getChannelId() + " at node " + ownNodeId);

            registerNewConnection(connection);

            if (!connection.getInitiatedByRemote()) {
                // only reply with an LSA batch if the connection was self-initiated
                LinkStateAdvertisementBatch payloadLsaCache = topologyMap.generateLsaBatchOfAllNodes();

                log.debug("Sending initial LSA batch into connection " + connection.getChannelId());

                byte[] lsaBytes = MessageUtils.serializeSafeObject(payloadLsaCache);
                NetworkRequest lsaRequest =
                    NetworkRequestFactory.createNetworkRequest(lsaBytes, ProtocolConstants.VALUE_MESSAGE_TYPE_LSA, ownNodeId,
                        connection.getRemoteNodeInformation().getInstanceNodeSessionId());
                directMessagingSender.sendDirectMessageAsync(lsaRequest, connection, new NetworkResponseHandler() {

                    @Override
                    public void onResponseAvailable(NetworkResponse response) {
                        if (!response.isSuccess()) {
                            log.warn("Failed to send initial LSA batch via connection " + connection.getChannelId() + ": Code "
                                + response.getResultCode());
                            return;
                        }
                        Serializable deserializedContent;
                        try {
                            deserializedContent = response.getDeserializedContent();
                            if (deserializedContent instanceof LinkStateAdvertisementBatch) {
                                boolean topologyChanged = handleInitialLSABatchResponse(deserializedContent);
                                onOutgoingChannelHandshakeCompleted(connection, topologyChanged);
                            } else {
                                log.error("Unexpected response to initial LSA batch: " + deserializedContent);
                            }
                        } catch (SerializationException e) {
                            log.error("Failed to deserialize response to initial LSA batch", e);
                        }
                    }

                });
            } else {
                // for a remote-initiated connection, an update LSA is sufficient
                broadcastNewLocalLSA();
            }
        }
        fireTopologyChangedListener();
    }

    private void handleOutgoingChannelTerminated(MessageChannel connection) {
        synchronized (connectionsById) {
            String channelId = connection.getChannelId();

            // remove link from topology
            TopologyLink link = topologyMap.getLinkForConnection(channelId);
            if (link == null) {
                log.debug("Channel " + channelId + " to unregister does not exist in the topology; "
                    + "the usual cause is that the remote node " + connection.getRemoteNodeInformation().getInstanceNodeSessionId()
                    + " was removed after a shutdown notice");
            } else {
                if (!topologyMap.removeLink(link)) {
                    log.warn("Unexpected state: Channel was found in topology, but could not be removed; id=" + channelId);
                }
            }

            // is there already a connection to this NCP?
            MessageChannel registeredConnection = connectionsById.get(channelId);
            if (registeredConnection == null) {
                log.warn("No registered connection for id " + channelId);
                return;
            }
            if (registeredConnection != connection) {
                log.warn("Another connection is registered under id " + channelId + "; ignoring unregistration");
                return;
            }
            connectionsById.remove(channelId);
            log.debug(StringUtils.format("Unregistered connection %s from %s", connection.toString(),
                ownNodeInformation.getLogDescription()));
        }
        broadcastNewLocalLSA();
        fireTopologyChangedListener();
    }

    /**
     * Event.
     * 
     * @param messageContent
     * @param metaData
     * @param ncp
     */
    private void onMaxTimeToLiveReached(Serializable messageContent, Map<String, String> metaData,
        NetworkContactPoint ncp) {
        networkStats.incFailedCommunications();

        log.debug(StringUtils.format(
            "'%s' reports that a message that was issued by '%s' exeeded the maximum time to live (%s).",
            ownNodeId, MessageMetaData.wrap(metaData).getSender(), timeToLive));
    }

    /**
     * 
     * TODO krol_ph: Enter comment!
     * 
     * @param messageId
     * @param messageContent
     */
    private void onMessageReceived(String messageId, Serializable messageContent) {

    }

    /**
     * @return Returns the owner.
     */
    public InstanceNodeSessionId getOwner() {
        return ownNodeId;
    }

    /**
     * Add received messages to a buffer so that they can be accessed later on.
     * 
     * @param messageContent The message content.
     */
    protected void addToMessageBuffer(String messageId, Serializable messageContent) {
        messageBuffer.put(messageId, messageContent);
        onMessageReceived(messageId, messageContent);
    }

    public TopologyMap getTopologyMap() {
        return topologyMap;
    }

    /**
     * @return Returns the networkStats.
     */
    public NetworkStats getNetworkStats() {
        return networkStats;
    }

    /**
     * @return Returns the timeToLive.
     */
    public int getTimeToLive() {
        return timeToLive;
    }

    public Map<String, Serializable> getMessageBuffer() {
        return messageBuffer;
    }

    private void fireTopologyChangedListener() {
        if (topologyChangeListener != null) {
            topologyChangeListener.onNetworkTopologyChanged();
        }
    }
}
