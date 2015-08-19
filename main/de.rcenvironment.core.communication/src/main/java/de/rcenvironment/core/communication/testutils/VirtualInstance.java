/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.messaging.RawMessageChannelTrafficListener;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodePropertyConstants;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.routing.internal.NetworkFormatter;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Provides a simulated/"virtual" node instance. Intended for use in integration testing; a major use case is setting up networks of virtual
 * nodes to test network dynamics and routing behavior.
 * 
 * @author Robert Mischke
 */
public class VirtualInstance extends VirtualInstanceSkeleton implements CommonVirtualInstanceControl {

    private static volatile boolean rememberRuntimePeersAfterRestart;

    private CommunicationManagementService managementService;

    private NetworkRoutingService networkRoutingService;

    private MessageRoutingService messageRoutingService;

    private MessageChannelService messageChannelService;

    private ConnectionSetupService connectionSetupService;

    private NodeConfigurationService nodeConfigurationService;

    private VirtualCommunicationBundle virtualCommunicationBundle;

    /**
     * Creates a virtual instance with the same string as its id and log/display name, and its "relay" flag set to "true".
     * 
     * Note that enabling "is relay" by default is done for backward compatibility of integration tests; the actual default configuration
     * setting is "false".
     * 
     * @param nodeId the string to use as node id and display/log name
     */
    public VirtualInstance(String nodeId) {
        this(nodeId, nodeId, true);
    }

    /**
     * Creates a virtual instance with the given log/display name, and its "relay" flag set to "true".
     * 
     * Note that enabling "is relay" by default is done for backward compatibility of integration tests; the actual default configuration
     * setting is "false".
     * 
     * @param the node id string to use
     * @param logName the log/display name to use
     */
    public VirtualInstance(String nodeId, String logName) {
        this(nodeId, logName, true);
    }

    /**
     * Creates a virtual instance with the given log/display name.
     * 
     * @param the node id string to use
     * @param logName the log/display name to use
     * @param isRelay whether the "is relay" flag of this node should be set
     */
    public VirtualInstance(String nodeId, String logName, boolean isRelay) {
        super(nodeId, logName, isRelay);

        virtualCommunicationBundle = VirtualCommunicationBundleFactory.createFromNodeConfigurationService(getNodeConfigurationService());
        virtualCommunicationBundle.setAutoStartNetworkOnActivation(false);
        virtualCommunicationBundle.activate();

        nodeConfigurationService = virtualCommunicationBundle.getService(NodeConfigurationService.class);
        messageChannelService = virtualCommunicationBundle.getService(MessageChannelService.class);
        connectionSetupService = virtualCommunicationBundle.getService(ConnectionSetupService.class);
        networkRoutingService = virtualCommunicationBundle.getService(NetworkRoutingService.class);
        messageRoutingService = virtualCommunicationBundle.getService(MessageRoutingService.class);
        managementService = virtualCommunicationBundle.getService(CommunicationManagementService.class);

        // register custom test message type
        messageChannelService.registerRequestHandler(ProtocolConstants.VALUE_MESSAGE_TYPE_TEST,
            new TestNetworkRequestHandler(nodeConfigurationService.getLocalNodeId()));
    }

    public static void setRememberRuntimePeersAfterRestarts(boolean rememberRuntimePeers) {
        VirtualInstance.rememberRuntimePeersAfterRestart = rememberRuntimePeers;
    }

    /**
     * Convenience method to send a payload to another node.
     * 
     * @param messageContent the request payload
     * @param messageType the message type to send; see {@link ProtocolConstants}
     * @param targetNodeId the id of the destination node
     * @return a {@link Future} providing the response
     * @throws CommunicationException on messaging errors
     * @throws InterruptedException on interruption
     * @throws ExecutionException on internal errors
     * @throws SerializationException on serialization failure
     */
    public Future<NetworkResponse> performRoutedRequest(Serializable messageContent, String messageType, NodeIdentifier targetNodeId)
        throws CommunicationException, InterruptedException, ExecutionException, SerializationException {
        byte[] serializedBody = MessageUtils.serializeObject(messageContent);
        return messageRoutingService.performRoutedRequest(serializedBody, messageType, targetNodeId);
    }

    /**
     * Convenience method to send a payload to another node. This method blocks until it has received a response, or the timeout was
     * exceeded.
     * 
     * @param messageContent the request payload
     * @param targetNodeId the id of the destination node
     * @param timeoutMsec the maximum time to wait for the response
     * @return the response
     * @throws CommunicationException on messaging errors
     * @throws InterruptedException on interruption
     * @throws TimeoutException on timeout
     * @throws ExecutionException on internal errors
     * @throws SerializationException on serialization failure
     */
    public NetworkResponse performRoutedRequest(Serializable messageContent, NodeIdentifier targetNodeId, int timeoutMsec)
        throws CommunicationException, InterruptedException, ExecutionException, TimeoutException, SerializationException {
        return performRoutedRequest(messageContent, ProtocolConstants.VALUE_MESSAGE_TYPE_TEST, targetNodeId).get(timeoutMsec,
            TimeUnit.MILLISECONDS);
    }

    @Override
    public void addNetworkConnectionListener(MessageChannelLifecycleListener listener) {
        getMessageChannelService().addChannelLifecycleListener(listener);
    }

    @Override
    public void addNetworkTrafficListener(RawMessageChannelTrafficListener listener) {
        getMessageChannelService().addTrafficListener(listener);
    }

    @Override
    public synchronized void addInitialNetworkPeer(NetworkContactPoint contactPoint) {
        VirtualInstanceState currentState = getCurrentState();
        if (currentState == VirtualInstanceState.STARTED) {
            // FIXME transitional code; rewrite calls
            log.warn("addInitialNetworkPeer() called for an instance in the STARTED state; change to addRuntimeNetworkPeer()");
            connectAsync(contactPoint);
            return;
        }
        if (currentState != VirtualInstanceState.INITIAL) {
            throw new IllegalStateException("Initial peers can only be added in the INITIAL state");
        }
        super.addInitialNetworkPeer(contactPoint);
    }

    /**
     * Adds and connects to a {@link NetworkContactPoint}.
     * 
     * @param contactPoint the {@link NetworkContactPoint} to add and connect to
     */
    public synchronized void connectAsync(NetworkContactPoint contactPoint) {
        if (getCurrentState() != VirtualInstanceState.STARTED) {
            throw new IllegalStateException("Runtime peers can only be added in the STARTED state (is " + getCurrentState() + ")");
        }
        if (rememberRuntimePeersAfterRestart) {
            // add this as an initial peer for next network startup
            super.addInitialNetworkPeer(contactPoint);
        }
        ConnectionSetup connectionSetup = connectionSetupService.createConnectionSetup(contactPoint, null, true);
        connectionSetup.signalStartIntent();
    }

    @Deprecated
    public String getFormattedLegacyNetworkGraph() {
        // TODO examine callers and check if they need to be adapted
        return getFormattedRawNetworkGraph();
        // return NetworkFormatter.formatTopologyMap(getRoutingService().getProtocolManager().getTopologyMap(), true);
    }

    public String getFormattedRawNetworkGraph() {
        return NetworkFormatter.networkGraphToGraphviz(getRoutingService().getRawNetworkGraph(), true);
    }

    public String getFormattedReachableNetworkGraph() {
        return NetworkFormatter.networkGraphToGraphviz(getRoutingService().getReachableNetworkGraph(), true);
    }

    /**
     * @return a formatted summary of this instance's LSA (routing) knowledge
     */
    public String getFormattedLSAKnowledge() {
        StringBuilder buffer = new StringBuilder();
        Map<NodeIdentifier, Map<String, String>> properties = getService(NodePropertiesService.class).getAllNodeProperties();
        buffer.append(StringUtils.format("LSA properties as seen by %s (%d entries):", getNodeId(), properties.size()));
        for (Entry<NodeIdentifier, Map<String, String>> entry : properties.entrySet()) {
            buffer.append(StringUtils.format("\n  %s: %s", entry.getKey(), entry.getValue().get(NodePropertyConstants.KEY_LSA)));
        }
        return buffer.toString();
    }

    public String getFormattedNetworkStats() {
        return NetworkFormatter.networkStats(getRoutingService().getProtocolManager().getNetworkStats());
    }

    /**
     * Test method: verifies same reporded topology hashes for all known nodes.
     * 
     * TODO verify description
     * 
     * @return true if all hashes are consistent
     */
    // TODO only called by obsolete test case in RoutingProtocolTest
    @Deprecated
    public boolean hasSameTopologyHashesForAllNodes() {
        throw new UnsupportedOperationException("Obsolete method");
        // return getRoutingService().getProtocolManager().getTopologyMap().hasSameTopologyHashesForAllNodes();
    }

    /**
     * Test method: determine the messaging route to the given node.
     * 
     * @param destination the destination node
     * @return the calculated sequence of {@link NetworkGraphLink}s
     */
    public List<? extends NetworkGraphLink> getRouteTo(VirtualInstance destination) {
        return messageRoutingService.getRouteTo(destination.getConfigurationService().getLocalNodeId());
    }

    public NetworkGraph getRawNetworkGraph() {
        return networkRoutingService.getRawNetworkGraph();
    }

    public NetworkGraph getReachableNetworkGraph() {
        return networkRoutingService.getReachableNetworkGraph();
    }

    public int getKnownNodeCount() {
        // note: before migration to the new network code, it was kind of undefined whether this method should refer
        // to the *raw* or *reachable* graph; now it does the former - misc_ro
        return getReachableNetworkGraph().getNodeCount();
    }

    /**
     * @param targetInstance the other instance to
     * @return if there is a direct link/channel to the other instance in this instance's known topology
     */
    public boolean knownTopologyContainsLinkTo(VirtualInstance targetInstance) {
        // note: before migration to the new network code, it was kind of undefined whether this method should refer
        // to the *raw* or *reachable* graph; now it does the former - misc_ro
        return getReachableNetworkGraph().containsLinkBetween(this.getNodeId(), targetInstance.getNodeId());
    }

    /**
     * Test method; probably obsolete.
     * 
     * @param messageId a message id
     * @return whether this message was received (?)
     */
    @Deprecated
    public boolean checkMessageReceivedById(String messageId) {
        return networkRoutingService.getProtocolManager().messageReivedById(messageId);
    }

    /**
     * Test method; probably obsolete.
     * 
     * @param messageContent a message content
     * @return whether this content was received (?)
     */
    @Deprecated
    // check the response received by the initiating node instead
    public boolean checkMessageReceivedByContent(Serializable messageContent) {
        return networkRoutingService.getProtocolManager().messageReivedByContent(messageContent);
    }

    @Override
    public void registerNetworkTransportProvider(NetworkTransportProvider provider) {
        messageChannelService.addNetworkTransportProvider(provider);
    }

    /**
     * @param <T> the service class to fetch
     * @param clazz the service class to fetch
     * @return the registered service of the virtual communication bundle
     */
    public <T> T getService(Class<T> clazz) {
        return virtualCommunicationBundle.getService(clazz);
    }

    public VirtualCommunicationBundle getVirtualCommunicationBundle() {
        return virtualCommunicationBundle;
    }

    /**
     * Provide unit/integration test access to the management service.
     * 
     * @return The management service.
     */
    public CommunicationManagementService getManagementService() {
        return managementService;
    }

    /**
     * Provide unit/integration test access to the routing service.
     * 
     * @return The routing service.
     */
    public NetworkRoutingService getRoutingService() {
        return networkRoutingService;
    }

    /**
     * Provide unit/integration test access to the connection service.
     * 
     * @return The connection service.
     */
    public MessageChannelService getMessageChannelService() {
        return messageChannelService;
    }

    /**
     * @return The configuration service.
     */
    public NodeConfigurationService getConfigurationService() {
        return super.getNodeConfigurationService();
    }

    public ConnectionSetupService getConnectionSetupService() {
        return virtualCommunicationBundle.getService(ConnectionSetupService.class);
    }

    public NodePropertiesService getNodePropertiesService() {
        return virtualCommunicationBundle.getService(NodePropertiesService.class);
    }

    @Override
    protected void performStartup() throws InterruptedException, CommunicationException {
        managementService.startUpNetwork();
    }

    @Override
    protected void performShutdown() throws InterruptedException {
        managementService.shutDownNetwork();
    }

    @Override
    protected void performSimulatedCrash() throws InterruptedException {
        // simply shut down the network; this should not send any "goodbye" messages etc.
        managementService.simulateUncleanShutdown();
    }

    /**
     * @return the node identifier of this virtual instance
     */
    public NodeIdentifier getNodeId() {
        return nodeInformation.getNodeId();
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return getNodeId().toString();
    }

    /**
     * @return the first server contact point; if none exists, an exception is thrown
     */
    public NetworkContactPoint getDefaultContactPoint() {
        List<NetworkContactPoint> serverContactPoints = getConfigurationService().getServerContactPoints();
        if (serverContactPoints.isEmpty()) {
            throw new IllegalStateException("No server contact points configured");
        }
        return serverContactPoints.get(0);
    }
}
