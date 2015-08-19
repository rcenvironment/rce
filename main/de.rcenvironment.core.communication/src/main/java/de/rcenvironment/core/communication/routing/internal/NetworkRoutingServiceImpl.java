/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
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
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.core.utils.incubator.ListenerDeclaration;
import de.rcenvironment.core.utils.incubator.ListenerProvider;

/**
 * A implementation of the {@link NetworkRoutingService} interface.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public class NetworkRoutingServiceImpl implements NetworkRoutingService, MessageRoutingService, ListenerProvider {

    /**
     * Keeps track of of distributed link state changes to adapt the local graph knowledge.
     * 
     * @author Robert Mischke
     */
    private final class LinkStateKnowledgeChangeTracker implements LinkStateKnowledgeChangeListener {

        // private LinkState localLinkState = new LinkState(new ArrayList<Link>());

        @Override
        public void onLinkStateKnowledgeChanged(Map<NodeIdentifier, LinkState> knowledge) {
            if (verboseLogging) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(StringUtils.format("New link state knowledge of %s (%d entries):", localNodeId, knowledge.size()));
                for (Entry<NodeIdentifier, LinkState> entry : knowledge.entrySet()) {
                    buffer.append(StringUtils.format("\n  Link state for %s: %s", entry.getKey(), entry.getValue()));
                }
                log.debug(buffer.toString());
            }

            if (knowledge.size() == 0) {
                // before any link state is known, remain at the initial placeholder model
                return;
            }
            // consistency check
            if (localNodeId == null) {
                throw new IllegalStateException();
            }

            NetworkGraphImpl rawGraph = new NetworkGraphImpl(localNodeId);

            Set<NodeIdentifier> nodeIdsWithLinkState = knowledge.keySet();
            for (NodeIdentifier nodeId : nodeIdsWithLinkState) {
                if (nodeId == null) {
                    throw new IllegalArgumentException("Map contained 'null' node id");
                }
                addNode(rawGraph, nodeId);
            }
            // consistency check
            int expectedGraphSize = knowledge.size();
            if (!knowledge.containsKey(localNodeId)) {
                expectedGraphSize++;
            }
            if (rawGraph.getNodeCount() != expectedGraphSize) {
                throw new IllegalStateException(StringUtils.format("Graph with %d nodes constructed, but expectes size was %d",
                    rawGraph.getNodeCount(), localNodeId));
            }

            int totalLinks = 0;
            for (Map.Entry<NodeIdentifier, LinkState> entry : knowledge.entrySet()) {
                NodeIdentifier sourceNodeId = entry.getKey();
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

        private void addNode(NetworkGraphImpl rawGraph, NodeIdentifier nodeId) {
            rawGraph.addNode(nodeId);
        }

        private void addLinks(NetworkGraphImpl rawGraph, NodeIdentifier sourceNodeId, List<Link> links) {
            for (Link link : links) {
                NodeIdentifier targetNodeId = NodeIdentifierFactory.fromNodeId(link.getNodeIdString());
                rawGraph.addLink(new NetworkGraphLinkImpl(link.getLinkId(), sourceNodeId, targetNodeId));
            }
        }

        @Override
        public void onLinkStatesUpdated(Map<NodeIdentifier, LinkState> delta) {
            if (verboseLogging) {
                log.debug("Updated link states for " + delta.size() + " nodes: " + delta.keySet());
            }
        }

        @Override
        public void onLocalLinkStateUpdated(LinkState linkState) {
            if (verboseLogging) {
                log.debug("Local link state updated (for " + localNodeId + "): " + linkState);
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
                    + " now contains %d node(s) and %d connection(s)", localNodeId,
                    topologyMap.getNodeCount(), topologyMap.getLinkCount()));

                rawNetworkGraph = (NetworkGraphImpl) topologyMap.toRawNetworkGraph();
            }

            // forward to outer class
            // updateFromRawNetworkGraph(rawNetworkGraph);
        }
    }

    private InitialNodeInformation ownNodeInformation;

    private MessageChannelService messageChannelService;

    private NodeConfigurationService configurationService;

    private LinkStateRoutingProtocolManager protocolManager;

    private volatile NetworkGraphImpl cachedRawNetworkGraph;

    private volatile NetworkGraphImpl cachedReachableNetworkGraph;

    private NodeIdentifier localNodeId;

    private TopologyMap topologyMap;

    private final ThreadPool threadPool = SharedThreadPool.getInstance();

    private final NetworkTopologyChangeTracker topologyChangeTracker = new NetworkTopologyChangeTracker();

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    // NOTE: used in several locations
    private final boolean forceLocalRPCSerialization = System
        .getProperty(NodeConfigurationService.SYSTEM_PROPERTY_FORCE_LOCAL_RPC_SERIALIZATION) != null;

    private final Log log = LogFactory.getLog(getClass());

    private long forwardingTimeoutMsec;

    private String localNodeIdString;

    /**
     * OSGi activate method.
     */
    public void activate() {
        ownNodeInformation = configurationService.getInitialNodeInformation();
        forwardingTimeoutMsec = configurationService.getForwardingTimeoutMsec();
        localNodeId = ownNodeInformation.getNodeId();
        localNodeIdString = localNodeId.getIdString();

        // create initial placeholders
        NetworkGraphImpl initialRawNetworkGraph = new NetworkGraphImpl(localNodeId);
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
    public Collection<ListenerDeclaration> defineListeners() {
        List<ListenerDeclaration> result = new ArrayList<ListenerDeclaration>();
        result.add(new ListenerDeclaration(LinkStateKnowledgeChangeListener.class, new LinkStateKnowledgeChangeTracker()));
        return result;
    }

    @Override
    public Future<NetworkResponse> performRoutedRequest(byte[] payload, String messageType, NodeIdentifier receiver) {
        final NetworkRequest request = NetworkRequestFactory.createNetworkRequest(payload, messageType, localNodeId, receiver);
        if (forceLocalRPCSerialization && receiver.equals(localNodeId)) {
            return threadPool.submit(new Callable<NetworkResponse>() {

                @Override
                @TaskDescription("Simulate local RPC (forced serialization)")
                public NetworkResponse call() throws Exception {
                    return messageChannelService.handleLocalForcedSerializationRPC(request, localNodeId);
                }
            });
        }
        return sendToNextHop(request);
    }

    @Override
    public NetworkResponse forwardAndAwait(NetworkRequest forwardingRequest) {
        // TODO refactor/improve?
        return forwardToNextHop(forwardingRequest);
    }

    @Override
    public List<? extends NetworkGraphLink> getRouteTo(NodeIdentifier destination) {
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
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param service The configuration service.
     */
    public void bindNodeConfigurationService(NodeConfigurationService service) {
        // do not allow rebinding for now
        if (this.configurationService != null) {
            throw new IllegalStateException();
        }
        this.configurationService = service;
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
                localNodeId,
                rawNetworkGraph.getNodeCount(), rawNetworkGraph.getLinkCount(),
                cachedReachableNetworkGraph.getNodeCount(), cachedReachableNetworkGraph.getLinkCount()));
        }

        // FIXME debug output; remove when done
        // log.debug("Raw network graph update:\n" + NetworkFormatter.networkGraphToGraphviz(cachedRawNetworkGraph, true));
        // log.debug("Reachable network graph update:\n" + NetworkFormatter.networkGraphToGraphviz(cachedReachableNetworkGraph, true));

        StatsCounter.count("Network topology changes", "network graph changed");
        if (topologyChangeTracker.updateReachableNetwork(cachedReachableNetworkGraph)) {
            StatsCounter.count("Network topology updates", "set of reachable nodes changed");
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
        String ownNodeIdString = localNodeId.getIdString();
        String sender = metadata.getSender().getIdString();
        String receiver = metadata.getFinalRecipient().getIdString();

        // TODO this blocks a thread for each forwarded request; improve in future version
        Future<NetworkResponse> responseFuture = sendToNextHop(forwardingRequest);
        NetworkResponse response = null;
        try {
            response = responseFuture.get(configurationService.getForwardingTimeoutMsec(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn(StringUtils.format("Timeout while forwarding message from %s to %s at %s (ReqId=%s)", sender, receiver,
                ownNodeIdString, requestId));
            response = NetworkResponseFactory.generateResponseForExceptionWhileRouting(forwardingRequest, ownNodeIdString, e);
        } catch (InterruptedException e) {
            log.warn(StringUtils.format("Interrupted while forwarding message from %s to %s at %s (ReqId=%s)", sender, receiver,
                ownNodeIdString, requestId), e);
            response = NetworkResponseFactory.generateResponseForExceptionWhileRouting(forwardingRequest, ownNodeIdString, e);
        } catch (ExecutionException e) {
            log.warn(
                StringUtils.format("Error while forwarding message from %s to %s at %s (ReqId=%s)", sender,
                    receiver, ownNodeIdString, requestId), e);
            response = NetworkResponseFactory.generateResponseForExceptionWhileRouting(forwardingRequest, ownNodeIdString, e);
        }
        if (response == null) {
            throw new IllegalStateException(
                StringUtils.format("NULL response after forwarding message from %s to %s at %s (ReqId=%s)", sender,
                    receiver, ownNodeIdString, requestId));
        }
        return response;
    }

    private Future<NetworkResponse> sendToNextHop(final NetworkRequest request) {
        NodeIdentifier receiver = request.accessMetaData().getFinalRecipient();
        // TODO move routing into Callable for faster return of caller thread? (still relevant @4.0?) - misc_ro
        NetworkGraphLink nextLink;
        try {
            nextLink = cachedReachableNetworkGraph.getRoutingInformation().getNextLinkTowards(receiver);
        } catch (NoRouteToNodeException e) {
            final NodeIdentifier sender = request.accessMetaData().getSender();
            log.warn(StringUtils.format("Found no route for a request from %s to %s (occurred on %s, type=%s, trace=%s)",
                sender, receiver, localNodeId, request.getMessageType(), request.accessMetaData().getTrace()));
            // convert to Future containing failure response
            return threadPool.submit(new Callable<NetworkResponse>() {

                @Override
                @TaskDescription("Create response for routing failure")
                public NetworkResponse call() throws Exception {
                    if (localNodeId.equals(sender)) {
                        return NetworkResponseFactory.generateResponseForNoRouteAtSender(request, localNodeId);
                    } else {
                        return NetworkResponseFactory.generateResponseForNoRouteWhileForwarding(request, localNodeId);
                    }
                };
            });
        }

        // if (verboseLogging) {
        // log.debug(StringUtils.format("Sending routed message for %s towards %s via link %s",
        // receiver, nextLink.getTargetNodeId(), nextLink.getLinkId()));
        // }

        WaitForResponseCallable responseCallable =
            new WaitForResponseCallable(request, forwardingTimeoutMsec, localNodeIdString);
        sendIntoLink(request, nextLink, responseCallable);
        return threadPool.submit(responseCallable);

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
    private void sendIntoLink(NetworkRequest request, final NetworkGraphLink link,
        final NetworkResponseHandler outerResponseHander) {

        NetworkResponseHandler responseHandler = new NetworkResponseHandler() {

            @Override
            public void onResponseAvailable(NetworkResponse response) {
                if (!response.isSuccess()) {
                    Serializable loggableContent;
                    try {
                        loggableContent = response.getDeserializedContent();
                    } catch (SerializationException e) {
                        // used for logging only
                        loggableContent = "Failed to deserialize content: " + e;
                    }
                    log.warn(StringUtils.format("Received non-success response for request id '%s' at '%s': result code: %s, body: '%s'",
                        response.getRequestId(), localNodeId, response.getResultCode(), loggableContent));
                }
                if (outerResponseHander != null) {
                    outerResponseHander.onResponseAvailable(response);
                } else {
                    log.warn("No outer response handler");
                }
            }

        };

        messageChannelService.sendRequest(request, link.getLinkId(), responseHandler);
    }
}
