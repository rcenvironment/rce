/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.common.impl.NodeIdentifierServiceImpl;
import de.rcenvironment.core.communication.configuration.CommunicationConfiguration;
import de.rcenvironment.core.communication.configuration.CommunicationIPFilterConfiguration;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.impl.InitialNodeInformationImpl;
import de.rcenvironment.core.communication.sshconnection.InitialSshConnectionConfig;
import de.rcenvironment.toolkit.utils.common.IdGeneratorType;

/**
 * Replacement {@link NodeConfigurationService} for {@link VirtualInstance} integrations tests. Defines the configuration of
 * {@link VirtualInstance}s.
 * 
 * @author Robert Mischke
 */
public class NodeConfigurationServiceTestStub implements NodeConfigurationService {

    private static final long TEST_INSTANCES_INITIAL_CONNECT_DELAY_MSEC = 300;

    private final InstanceNodeSessionId localInstanceSessionId;

    private final InitialNodeInformationImpl localNodeInformation;

    private final List<NetworkContactPoint> serverContactPoints;

    private final List<NetworkContactPoint> initialNetworkPeers;

    private final boolean isRelay;

    private NodeIdentifierServiceImpl nodeIdentifierService;

    public NodeConfigurationServiceTestStub(String predefinedInstanceId, String displayName, boolean isRelay) {
        nodeIdentifierService = new NodeIdentifierServiceImpl(IdGeneratorType.FAST);

        final InstanceNodeId localInstanceId;
        if (predefinedInstanceId != null) {
            try {
                localInstanceId = nodeIdentifierService.parseInstanceNodeIdString(predefinedInstanceId);
            } catch (IdentifierException e) {
                throw NodeIdentifierUtils.wrapIdentifierException(e); // only used in testing, so use the simple wrapper
            }
        } else {
            localInstanceId = nodeIdentifierService.generateInstanceNodeId();
        }
        localInstanceSessionId = nodeIdentifierService.generateInstanceNodeSessionId(localInstanceId);
        LogFactory.getLog(getClass()).debug("Created instance session id " + localInstanceSessionId + " for provided instance id");

        localNodeInformation = new InitialNodeInformationImpl(localInstanceSessionId);
        localNodeInformation.setDisplayName(displayName);
        serverContactPoints = new ArrayList<NetworkContactPoint>();
        initialNetworkPeers = new ArrayList<NetworkContactPoint>();
        this.isRelay = isRelay;
    }

    @Override
    public NodeIdentifierServiceImpl getNodeIdentifierService() {
        return nodeIdentifierService;
    }

    @Override
    public InstanceNodeSessionId getInstanceNodeSessionId() {
        return localInstanceSessionId;
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
    public int getForwardingTimeoutMsec() {
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

    @Override
    public List<InitialSshConnectionConfig> getInitialSSHConnectionConfigs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double[] getLocationCoordinates() {
        return new double[] { 0, 0 };
    }

    @Override
    public String getLocationName() {
        return "";
    }

    @Override
    public String getInstanceContact() {
        return "";
    }

    @Override
    public String getInstanceAdditionalInformation() {
        return "";
    }

}
