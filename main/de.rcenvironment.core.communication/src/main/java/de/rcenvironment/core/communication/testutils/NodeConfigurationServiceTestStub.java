/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.configuration.CommunicationConfiguration;
import de.rcenvironment.core.communication.configuration.CommunicationIPFilterConfiguration;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.impl.InitialNodeInformationImpl;

/**
 * Replacement {@link NodeConfigurationService} for {@link VirtualInstance} integrations tests. Defines the configuration of
 * {@link VirtualInstance}s.
 * 
 * @author Robert Mischke
 */
public class NodeConfigurationServiceTestStub implements NodeConfigurationService {

    private static final long TEST_INSTANCES_INITIAL_CONNECT_DELAY_MSEC = 300;

    private final NodeIdentifier localNodeId;

    private final InitialNodeInformationImpl localNodeInformation;

    private final List<NetworkContactPoint> serverContactPoints;

    private final List<NetworkContactPoint> initialNetworkPeers;

    private final boolean isRelay;

    public NodeConfigurationServiceTestStub(String nodeId, String displayName, boolean isRelay) {
        localNodeId = NodeIdentifierFactory.fromNodeId(nodeId);
        localNodeInformation = new InitialNodeInformationImpl(localNodeId);
        localNodeInformation.setDisplayName(displayName);
        serverContactPoints = new ArrayList<NetworkContactPoint>();
        initialNetworkPeers = new ArrayList<NetworkContactPoint>();
        this.isRelay = isRelay;
    }

    @Override
    public NodeIdentifier getLocalNodeId() {
        return localNodeId;
    }

    @Override
    @Deprecated
    public boolean isWorkflowHost() {
        return false;
    }

    @Override
    public InitialNodeInformation getInitialNodeInformation() {
        return localNodeInformation;
    }

    @Override
    public List<NetworkContactPoint> getServerContactPoints() {
        return serverContactPoints;
    }

    @Override
    public List<NetworkContactPoint> getInitialNetworkContactPoints() {
        return initialNetworkPeers;
    }

    @Override
    public boolean isRelay() {
        return isRelay;
    }

    @Override
    public long getDelayBeforeStartupConnectAttempts() {
        return TEST_INSTANCES_INITIAL_CONNECT_DELAY_MSEC;
    }

    @Override
    public int getRequestTimeoutMsec() {
        // use the default value for tests, too; can be changed if useful
        return CommunicationConfiguration.DEFAULT_REQUEST_TIMEOUT_MSEC;
    }

    @Override
    public long getForwardingTimeoutMsec() {
        // use the default value for tests, too; can be changed if useful
        return CommunicationConfiguration.DEFAULT_FORWARDING_TIMEOUT_MSEC;
    }

    /**
     * Adds a server ("provided") {@link NetworkContactPoint}.
     * 
     * @param contactPoint the server {@link NetworkContactPoint} to add
     */
    public void addServerConfigurationEntry(NetworkContactPoint contactPoint) {
        serverContactPoints.add(contactPoint);
    }

    /**
     * Adds an initial neighbor ("remote") {@link NetworkContactPoint}.
     * 
     * @param contactPoint the server {@link NetworkContactPoint} to add
     */
    public void addInitialNetworkPeer(NetworkContactPoint contactPoint) {
        initialNetworkPeers.add(contactPoint);
    }

    @Override
    public CommunicationIPFilterConfiguration getIPFilterConfiguration() {
        return new CommunicationIPFilterConfiguration(); // return default settings (no filtering)
    }

}
