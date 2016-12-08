/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierContextHolder;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.messaging.direct.api.DirectMessagingSender;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkResponseHandler;
import de.rcenvironment.core.communication.model.internal.NetworkGraphImpl;
import de.rcenvironment.core.communication.model.internal.NetworkGraphLinkImpl;
import de.rcenvironment.core.communication.protocol.MessageMetaData;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.routing.internal.v2.Link;
import de.rcenvironment.core.communication.routing.internal.v2.LinkState;
import de.rcenvironment.core.communication.routing.internal.v2.LinkStateKnowledgeChangeListener;
import de.rcenvironment.core.communication.routing.internal.v2.NoRouteToNodeException;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListenerAdapter;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * A implementation of the {@link NetworkRoutingService} interface.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public class NetworkRoutingServiceImpl implements NetworkRoutingService, MessageRoutingService, AdditionalServicesProvider {

    /**
     * Keeps track of of distributed link state changes to adapt the local graph knowledge.
     * 
     * @author Robert Mischke
     */
    private final class LinkStateKnowledgeChangeTracker implements LinkStateKnowledgeChangeListener {

        // private LinkState localLinkState = new LinkState(new ArrayList<Link>());

        @Override
        public void onLinkStateKnowledgeChanged(Map<InstanceNodeSessionId, LinkState> knowledge) {
            if (verboseLogging) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(StringUtils.format("New link state knowledge of %s (%d entries):", localInstanceSessionId, knowledge.size()));
                for (Entry<InstanceNodeSessionId, LinkState> entry : knowledge.entrySet()) {
                    buffer.append(StringUtils.format("\n  Link state for %s: %s", entry.getKey(), entry.getValue()));
                }
                log.debug(buffer.toString());
            }

            if (knowledge.size() == 0) {
                // before any link state is known, remain at the initial placeholder model
                return;
            }
            // consistency check
            if (localInstanceSessionId == null) {
                throw new IllegalStateException();
            }

            NetworkGraphImpl rawGraph = new NetworkGraphImpl(localInstanceSessionId);

            Set<InstanceNodeSessionId> nodeIdsWithLinkState = knowledge.keySet();
            for (InstanceNodeSessionId nodeId : nodeIdsWithLinkState) {
                if (nodeId == null) {
                    throw new IllegalArgumentException("Map contained 'null' node id");
                }
                addNode(rawGraph, nodeId);
            }
            // consistency check
            int expectedGraphSize = knowledge.size();
            if (!knowledge.containsKey(localInstanceSessionId)) {
                expectedGraphSize++;
            }
            if (rawGraph.getNodeCount() != expectedGraphSize) {
                throw new IllegalStateException(StringUtils.format("Graph with %d nodes constructed, but expectes size was %d",
                    rawGraph.getNodeCount(), localInstanceSessionId));
            }

            int totalLinks = 0;
            for (Map.Entry<InstanceNodeSessionId, LinkState> entry : knowledge.entrySet()) {
                InstanceNodeSessionId sourceNodeId = entry.getKey();
                LinkState linkState = entry.getValue();
                List<Link> links = linkState.getLinks();
                addLinks(rawGraph, sourceNodeId, links);
                totalLinks += linkState.getLinks().size();
            }
            // consistency check
            if (rawGraph.getLinkCount() != totalLinks) {
                throw new IllegalStateException();
            }

            updateFromRawNetworkGraph(rawGraph);
        }

        private void addNode(NetworkGraphImpl rawGraph, InstanceNodeSessionId nodeId) {
            rawGraph.addNode(nodeId);
        }

        private void addLinks(NetworkGraphImpl rawGraph, InstanceNodeSessionId sourceNodeId, List<Link> links) {
            for (Link link : links) {
                InstanceNodeSessionId targetNodeId;
                try {
                    targetNodeId = nodeIdentifierService.parseInstanceNodeSessionIdString(link.getNodeIdString());
                } catch (IdentifierException e) {
                    // note: currently not handling malformed ids here; will throw RTE on failure
                    throw NodeIdentifierUtils.wrapIdentifierException(e);
                }
                rawGraph.addLink(new NetworkGraphLinkImpl(link.getLinkId(), sourceNodeId, targetNodeId));
            }
        }

        @Override
        public void onLinkStatesUpdated(Map<InstanceNodeSessionId, LinkState> delta) {
            if (verboseLogging) {
                log.debug("Updated link states for " + delta.size() + " nodes: " + delta.keySet());
            }
        }

        @Override
        public void onLocalLinkStateUpdated(LinkState linkState) {
            if (verboseLogging) {
                log.debug("Local link state updated (for " + localInstanceSessionId + "): " + linkState);
            }
            // localLinkState = linkState;
        }
    }

    /**
     * Initial listener for low-level topology changes. This is the only listener that receives change events from the routing layer, and
     * delegates these events to external listeners. Each callback to an external listener is performed in a separate thread to prevent
     * blocking listeners from affecting the calling code.
     * 
     * @author Robert Mischke
     */
    private class LowLevelNetworkTopologyChangeHandler extends NetworkTopologyChangeListenerAdapter {

        @Override
        public void onNetworkTopologyChanged() {
            NetworkGraphImpl rawNetworkGraph;
            synchronized (topologyMap) {
                log.debug(StringUtils.format("Low-level topology change detected; the topology map of %s"
                    + " now contains %d node(s) and %d connection(s)", localInstanceSessionId,
                    topologyMap.getNodeCount(), topologyMap.getLinkCount()));

                rawNetworkGraph = (NetworkGraphImpl) topologyMap.toRawNetworkGraph();
            }

            // forward to outer class
            // updateFromRawNetworkGraph(rawNetworkGraph);
        }
    }

    private InitialNodeInformation ownNodeInformation;

    private MessageChannelService messageChannelService;

    private DirectMessagingSender directMessagingSender;

    private NodeConfigurationService nodeConfigurationService;

    private LinkStateRoutingProtocolManager protocolManager;

    private volatile NetworkGraphImpl cachedRawNetworkGraph;

    private volatile NetworkGraphImpl cachedReachableNetworkGraph;

    private InstanceNodeSessionId localInstanceSessionId;

    private TopologyMap topologyMap;

    private final NetworkTopologyChangeTracker topologyChangeTracker = new NetworkTopologyChangeTracker();

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    // NOTE: used in several locations
    private final boolean forceLocalRPCSerialization = System
        .getProperty(NodeConfigurationService.SYSTEM_PROPERTY_FORCE_LOCAL_RPC_SERIALIZATION) != null;

    private final Log log = LogFactory.getLog(getClass());

    private int routedRequestTimeoutMsec;

    private int forwardingTimeoutMsec;

    private NodeIdentifierService nodeIdentifierService;

    /**
     * OSGi activate method.
     */
    public void activate() {
        ownNodeInformation = nodeConfigurationService.getInitialNodeInformation();
        routedRequestTimeoutMsec = nodeConfigurationService.getRequestTimeoutMsec();
        forwardingTimeoutMsec = nodeConfigurationService.getForwardingTimeoutMsec();
        localInstanceSessionId = ownNodeInformation.getInstanceNodeSessionId();

        // create initial placeholders
        NetworkGraphImpl initialRawNetworkGraph = new NetworkGraphImpl(localInstanceSessionId);
        updateFromRawNetworkGraph(initialRawNetworkGraph);

        // initialize tracker with initial graph
        topologyChangeTracker.updateReachableNetwork(cachedReachableNetworkGraph);

        // topologyMap = new TopologyMap(ownNodeInformation);
        // protocolManager = new LinkStateRoutingProtocolManager(topologyMap, connectionService, new
        // LowLevelNetworkTopologyChangeHandler());

        // TODO set here to break up cyclic dependency; refactor? - misc_ro
        messageChannelService.setForwardingService(this);
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        List<AdditionalServiceDeclaration> result = new ArrayList<AdditionalServiceDeclaration>();
        result.add(new AdditionalServiceDeclaration(LinkStateKnowledgeChangeListener.class, new LinkStateKnowledgeChangeTracker()));
        return result;
    }

    @Override
    public NetworkResponse performRoutedRequest(byte[] payload, String messageType, InstanceNodeSessionId receiver) {
        // TODO find a more generic solution to this
        final NodeIdentifierService previousService =
            NodeIdentifierContextHolder.setDeserializationServiceForCurrentThread(nodeIdentifierService);
        try {
            return performRoutedRequest(payload, messageType, receiver, routedRequestTimeoutMsec);
        } finally {
            NodeIdentifierContextHolder.setDeserializationServiceForCurrentThread(previousService);
        }
    }

    @Override
    public NetworkResponse performRoutedRequest(byte[] payload, String messageType, InstanceNodeSessionId receiver, int timeoutMsec) {
        final NetworkRequest request = NetworkRequestFactory.createNetworkRequest(payload, messageType, localInstanceSessionId, receiver);
        if (forceLocalRPCSerialization && receiver.equals(localInstanceSessionId)) {
            return messageChannelService.handleLocalForcedSerializationRPC(request, localInstanceSessionId);
        }
        return sendToNextHopAndAwaitResponse(request, timeoutMsec);
    }

    @Override
    public NetworkResponse forwardAndAwait(NetworkRequest forwardingRequest) {
        // TODO refactor/improve?
        return forwardToNextHop(forwardingRequest);
    }

    @Override
    public List<? extends NetworkGraphLink> getRouteTo(InstanceNodeSessionId destination) {
        return cachedReachableNetworkGraph.getRoutingInformation().getRouteTo(destination);
    }

    @Override
    public synchronized NetworkGraph getRawNetworkGraph() {
        return cachedRawNetworkGraph;
    }

    @Override
    public synchronized NetworkGraph getReachableNetworkGraph() {
        return cachedReachableNetworkGraph;
    }

    /**
     * TODO Restrict method visibility.
     * 
     * @return Returns the protocol.
     */
    @Override
    public LinkStateRoutingProtocolManager getProtocolManager() {
        return protocolManager;
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param service The network connection service.
     */
    public void bindMessageChannelService(MessageChannelService service) {
        // do not allow rebinding for now
        if (this.messageChannelService != null) {
            throw new IllegalStateException();
        }
        this.messageChannelService = service;
        // note: currently extending each other
        this.directMessagingSender = service;
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param service The configuration service.
     */
    public void bindNodeConfigurationService(NodeConfigurationService service) {
        // do not allow rebinding for now
        if (this.nodeConfigurationService != null) {
            throw new IllegalStateException();
        }
        this.nodeConfigurationService = service;
        this.nodeIdentifierService = nodeConfigurationService.getNodeIdentifierService();
    }

    /**
     * Adds a new {@link NetworkTopologyChangeListener}. This method is not part of the service interface; it is only meant to be used via
     * OSGi-DS (whiteboard pattern) and integration tests.
     * 
     * @param listener the listener
     */
    public void addNetworkTopologyChangeListener(NetworkTopologyChangeListener listener) {
        topologyChangeTracker.addListener(listener);
    }

    /**
     * Removes a {@link NetworkTopologyChangeListener}. This method is not part of the service interface; it is only meant to be used via
     * OSGi-DS (whiteboard pattern) and integration tests.
     * 
     * @param listener the listener
     */
    public void removeNetworkTopologyChangeListener(NetworkTopologyChangeListener listener) {
        topologyChangeTracker.removeListener(listener);
    }

    @Override
    public String getFormattedNetworkInformation(String type) {
        if ("info".equals(type)) {
            return NetworkFormatter.networkGraphToConsoleInfo(cachedReachableNetworkGraph);
        }
        if ("graphviz".equals(type)) {
            return NetworkFormatter.networkGraphToGraphviz(cachedReachableNetworkGraph, true);
        }
        if ("graphviz-all".equals(type)) {
            return NetworkFormatter.networkGraphToGraphviz(cachedRawNetworkGraph, true);
        }
        throw new IllegalArgumentException("Invalid type: " + type);
    }

    protected synchronized void updateFromRawNetworkGraph(NetworkGraphImpl rawNetworkGraph) {

        cachedRawNetworkGraph = rawNetworkGraph;
        cachedReachableNetworkGraph = rawNetworkGraph.reduceToReachableGraph();

        if (verboseLogging) {
            log.debug(StringUtils.format(
                "Updating %s with a raw graph of %d nodes and %d edges resulted in a reachable graph of %d nodes and %d edges",
                localInstanceSessionId,
                rawNetworkGraph.getNodeCount(), rawNetworkGraph.getLinkCount(),
                cachedReachableNetworkGraph.getNodeCount(), cachedReachableNetworkGraph.getLinkCount()));
        }

        // FIXME debug output; remove when done
        // log.debug("Raw network graph update:\n" + NetworkFormatter.networkGraphToGraphviz(cachedRawNetworkGraph, true));
        // log.debug("Reachable network graph update:\n" + NetworkFormatter.networkGraphToGraphviz(cachedReachableNetworkGraph, true));

        StatsCounter.count("Network topology/routing", "Network graph changes");
        if (topologyChangeTracker.updateReachableNetwork(cachedReachableNetworkGraph)) {
            StatsCounter.count("Network topology/routing", "Set of reachable nodes changes");
        } else {
            if (verboseLogging) {
                log.debug("Ignoring low-level topology change event, as it had no effect on the set of reachable nodes");
            }
        }
    }

    private NetworkResponse forwardToNextHop(final NetworkRequest forwardingRequest) {
        // extract common metadata for logging
        MessageMetaData metadata = forwardingRequest.accessMetaData();
        String requestId = forwardingRequest.getRequestId();
        String localNodeIdString = localInstanceSessionId.getInstanceNodeSessionIdString();
        String sender = metadata.getSenderIdString();
        String receiver = metadata.getFinalRecipientIdString();

        // TODO while improved in 7.0, this still blocks one thread for each forwarded request
        final NetworkResponse response = sendToNextHopAndAwaitResponse(forwardingRequest, forwardingTimeoutMsec);
        // should be redundant; can be removed in 8.0.0
        if (response == null) {
            throw new IllegalStateException(
                StringUtils.format("NULL response after forwarding message from %s to %s at %s (ReqId=%s)", sender,
                    receiver, localNodeIdString, requestId));
        }
        return response;
    }

    private NetworkResponse sendToNextHopAndAwaitResponse(final NetworkRequest request, int timeoutMsec) {
        WaitForResponseBlocker responseBlocker = new WaitForResponseBlocker(request, localInstanceSessionId);
        sendToNextHopAsync(request, responseBlocker);
        return responseBlocker.await(timeoutMsec);

        // TODO review: attach error source or trace information here if the response does not represent success

        // try {
        // response = responseFuture.get(configurationService.getForwardingTimeoutMsec(), TimeUnit.MILLISECONDS);
        // } catch (TimeoutException e) {
        // log.warn(StringUtils.format("Timeout while forwarding message from %s to %s at %s (ReqId=%s)", sender, receiver,
        // ownNodeIdString, requestId));
        // response = NetworkResponseFactory.generateResponseForExceptionDuringDelivery(forwardingRequest, ownNodeIdString, e);
        // } catch (InterruptedException e) {
        // log.warn(StringUtils.format("Interrupted while forwarding message from %s to %s at %s (ReqId=%s)", sender, receiver,
        // ownNodeIdString, requestId), e);
        // response = NetworkResponseFactory.generateResponseForExceptionDuringDelivery(forwardingRequest, ownNodeIdString, e);
        // } catch (ExecutionException e) {
        // log.warn(
        // StringUtils.format("Error while forwarding message from %s to %s at %s (ReqId=%s)", sender,
        // receiver, ownNodeIdString, requestId), e);
        // response = NetworkResponseFactory.generateResponseForExceptionDuringDelivery(forwardingRequest, ownNodeIdString, e);
        // }

    }

    private void sendToNextHopAsync(final NetworkRequest request, NetworkResponseHandler responseHandler) {
        InstanceNodeSessionId receiver = request.accessMetaData().getFinalRecipient();
        NetworkGraphLink nextLink;
        try {
            // TODO move routing into Callable for faster return of caller thread? (still relevant @4.0?) - misc_ro
            nextLink = cachedReachableNetworkGraph.getRoutingInformation().getNextLinkTowards(receiver);
        } catch (NoRouteToNodeException e) {
            final InstanceNodeSessionId sender = request.accessMetaData().getSender();
            log.debug(StringUtils.format("Found no route for a request from %s to %s (type=%s, trace=%s)",
                sender, receiver, request.getMessageType(), request.accessMetaData().getTrace()));

            // generate failure response
            final NetworkResponse response;
            if (localInstanceSessionId.equals(sender)) {
                response = NetworkResponseFactory.generateResponseForNoRouteAtSender(request, localInstanceSessionId);
            } else {
                response = NetworkResponseFactory.generateResponseForNoRouteWhileForwarding(request, localInstanceSessionId);
            }

            // send to result handler
            responseHandler.onResponseAvailable(response);
            return;
        }

        // if (verboseLogging) {
        // log.debug(StringUtils.format("Sending routed message for %s towards %s via link %s",
        // receiver, nextLink.getTargetNodeId(), nextLink.getLinkId()));
        // }

        sendDirectMessageAsync(request, nextLink, responseHandler);

        // TODO restore routing retry? (on higher call level?)

        // // if there is a route, use it
        // int routeRetries = 0;
        // while (route.validate()) {
        //
        // // forward message content to next network contact point on the route
        // WaitForResponseCallable responseCallable = new WaitForResponseCallable();
        // if (protocolManager.sendTowardsNeighbor(messageContent, metaData, route.getFirstLink(),
        // responseCallable)) {
        // return executorService.submit(responseCallable);
        // } else {
        // routeRetries++;
        // // TODO make limit a constant
        // if (routeRetries >= 3) {
        // break;
        // }
        // // try to get a new route.
        // // TODO add retry limit? -- misc_ro
        // route = protocolManager.getRouteTo(receiver);
        // }
        // }
        // throw new CommunicationException(StringUtils.format("'%s' could not find a route to '%s'.",
        // ownNodeId, receiver));
    }

    /**
     * Central method to send a {@link NetworkRequest} into a {@link NetworkGraphLink}. No routing is involved here anymore.
     * 
     * @param request the {@link NetworkRequest} to send
     * @param link the {@link NetworkGraphLink} identifying the message channel to use
     * @param outerResponseHander the {@link NetworkResponseHandler} to report the response to
     */
    private void sendDirectMessageAsync(NetworkRequest request, final NetworkGraphLink link,
        final NetworkResponseHandler outerResponseHander) {

        if (outerResponseHander == null) {
            throw new IllegalArgumentException("Outer response handler must not be null");
        }

        NetworkResponseHandler responseHandler = new NetworkResponseHandler() {

            @Override
            public void onResponseAvailable(NetworkResponse response) {
                // TODO add failure backtrace route here?
                // if (!response.isSuccess()) {
                // }
                outerResponseHander.onResponseAvailable(response);
            }

        };

        directMessagingSender.sendDirectMessageAsync(request, link.getLinkId(), responseHandler);
    }
}
