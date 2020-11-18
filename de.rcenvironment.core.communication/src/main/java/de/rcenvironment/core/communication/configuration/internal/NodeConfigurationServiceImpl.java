/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.configuration.CommunicationConfiguration;
import de.rcenvironment.core.communication.configuration.CommunicationIPFilterConfiguration;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.configuration.SshConnectionsConfiguration;
import de.rcenvironment.core.communication.configuration.UplinkConnectionsConfiguration;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.impl.InitialNodeInformationImpl;
import de.rcenvironment.core.communication.sshconnection.InitialSshConnectionConfig;
import de.rcenvironment.core.communication.sshconnection.InitialUplinkConnectionConfig;
import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default {@link NodeConfigurationService} implementation.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Brigitte Boden
 */
public class NodeConfigurationServiceImpl implements NodeConfigurationService {

    // TODO temporary hardcoded default for actual RCE instances; see Mantis #8074
    private static final int STARTUP_INITIAL_CONNECT_DELAY_MSEC = 2500;

    private final List<NetworkContactPoint> serverContactPoints;

    private final List<NetworkContactPoint> initialNetworkPeers;

    private InitialNodeInformationImpl localNodeInformation;

    private CommunicationConfiguration configuration;

    private SshConnectionsConfiguration sshConfiguration;

    private UplinkConnectionsConfiguration uplinkConfiguration;

    private ConfigurationService configurationService;

    private PersistentSettingsService persistentSettingsService;

    private CommunicationIPFilterConfiguration ipFilterConfiguration;

    private boolean initialIPFilterConfigLoaded = false;

    private final Log log = LogFactory.getLog(getClass());

    private boolean localNodeIsRelay;

    private InstanceNodeId localInstanceId;

    private NodeIdentifierService nodeIdentifierService;

    private InstanceNodeSessionId localInstanceSessionId;

    public NodeConfigurationServiceImpl() {
        serverContactPoints = new ArrayList<NetworkContactPoint>();
        initialNetworkPeers = new ArrayList<NetworkContactPoint>();
    }

    @Override
    public NodeIdentifierService getNodeIdentifierService() {
        return nodeIdentifierService;
    }

    @Override
    public InstanceNodeSessionId getInstanceNodeSessionId() {
        return localNodeInformation.getInstanceNodeSessionId();
    }

    @Override
    @Deprecated
    public boolean isWorkflowHost() {
        return configurationService.getIsWorkflowHost();
    }

    @Override
    public InitialNodeInformation getInitialNodeInformation() {
        return localNodeInformation;
    }

    @Override
    public List<NetworkContactPoint> getServerContactPoints() {
        return Collections.unmodifiableList(serverContactPoints);
    }

    @Override
    public List<NetworkContactPoint> getInitialNetworkContactPoints() {
        return Collections.unmodifiableList(initialNetworkPeers);
    }

    @Override
    public boolean isRelay() {
        return localNodeIsRelay;
    }

    @Override
    public long getDelayBeforeStartupConnectAttempts() {
        // TODO temporary hardcoded default for actual RCE instances; see Mantis #8074
        return STARTUP_INITIAL_CONNECT_DELAY_MSEC;
    }

    @Override
    public int getRequestTimeoutMsec() {
        return configuration.getRequestTimeoutMsec();
    }

    @Override
    public int getForwardingTimeoutMsec() {
        return configuration.getForwardingTimeoutMsec();
    }

    @Override
    public CommunicationIPFilterConfiguration getIPFilterConfiguration() {
        // not handled in activate() to allow reloading
        if (initialIPFilterConfigLoaded) {
            configurationService.reloadConfiguration();
        } else {
            initialIPFilterConfigLoaded = true;
        }
        ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("network/ipFilter");
        try {
            ipFilterConfiguration = configurationSegment.mapToObject(CommunicationIPFilterConfiguration.class);
        } catch (IOException e) {
            log.error("Error parsing IP filter configuration; falling back to blocking all IPs!", e);
            ipFilterConfiguration = new CommunicationIPFilterConfiguration();
            ipFilterConfiguration.setEnabled(true); // enable, but do not add IPs -> block all
        }
        log.info(StringUtils.format("IP filter enabled: %s, configured number of IPs: %d",
            ipFilterConfiguration.getEnabled(), ipFilterConfiguration.getAllowedIPs().size()));
        return ipFilterConfiguration;
    }

    @Override
    public File getStandardImportDirectory(String subdir) {
        return configurationService.getStandardImportDirectory(subdir);
    }

    /**
     * OSGi-DS lifecycle method; made public for unit testing.
     * 
     * @param context OSGi {@link BundleContext}
     */
    public void activate(BundleContext context) {
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // do not activate this service if is was spawned as part of a default test environment
            return;
        }
        
        ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("network");
        configuration = new CommunicationConfiguration(configurationSegment);
        createLocalNodeInformation();
        parseNetworkConfiguration();
        localNodeIsRelay = configurationService.getIsRelay();
        log.info("Local 'isRelay' setting: " + isRelay());

        ConfigurationSegment sshConfigurationSegment = configurationService.getConfigurationSegment("sshRemoteAccess");
        ConfigurationSegment uplinkConfigurationSegment = configurationService.getConfigurationSegment("uplink");
        sshConfiguration = new SshConnectionsConfiguration(sshConfigurationSegment);
        uplinkConfiguration = new UplinkConnectionsConfiguration(uplinkConfigurationSegment);
    }

    /**
     * OSGi-DS bind method; made public for unit testing.
     * 
     * @param newService the new {@link ConfigurationService} to set
     */
    public void bindConfigurationService(ConfigurationService newService) {
        configurationService = newService;
    }

    /**
     * OSGi-DS bind method; made public for unit testing.
     * 
     * @param newService the new {@link NodeIdentifierService} to set
     */
    public void bindNodeIdentifierService(NodeIdentifierService newService) {
        this.nodeIdentifierService = newService;
    }

    /**
     * OSGi-DS bind method; made public for unit testing.
     * 
     * @param newService the new {@link PersistentSettingsService} to set
     */
    public void bindPersistentSettingsService(PersistentSettingsService newService) {
        persistentSettingsService = newService;
    }

    private void createLocalNodeInformation() {
        String predefinedInstanceIdString = getStoredOrOverriddenInstanceId();

        if (predefinedInstanceIdString == null) {
            localInstanceId = nodeIdentifierService.generateInstanceNodeId();
            persistentSettingsService.saveStringValue(PERSISTENT_SETTINGS_KEY_PLATFORM_ID, localInstanceId.getInstanceNodeIdString());
            log.info("Generated and stored id " + localInstanceId.getInstanceNodeIdString() + " for the local node");
        } else {
            try {
                localInstanceId = nodeIdentifierService.parseInstanceNodeIdString(predefinedInstanceIdString);
                log.info("Reusing the previously stored id " + predefinedInstanceIdString + " for the local node");
            } catch (IdentifierException e) {
                throw new IllegalStateException("Invalid stored or overridden instance id '" + predefinedInstanceIdString
                    + "'; aborting to avoid running with an inconsistent state");
            }
        }

        localInstanceSessionId = nodeIdentifierService.generateInstanceNodeSessionId(localInstanceId);

        String instanceName = configurationService.getInstanceName();

        localNodeInformation = new InitialNodeInformationImpl(localInstanceSessionId);
        localNodeInformation.setDisplayName(instanceName);

    }

    private String getStoredOrOverriddenInstanceId() {
        // check if a node id override property is defined; this takes precedence over any profile setting
        String nodeId = System.getProperty(SYSTEM_PROPERTY_OVERRIDE_NODE_ID);
        if (nodeId != null) {
            // no need to validate the value; this will be done below anyway
            log.info("Using custom node id defined by system property: " + nodeId);
        }
        // check if a node id override property is defined in the profile
        if (nodeId == null) {
            nodeId = configuration.getNodeIdOverrideValue();
            if (nodeId != null) {
                log.info("Using custom node id defined by profile setting: " + nodeId);
            }
        }
        // otherwise, check for an existing persistent node id
        if (nodeId == null) {
            nodeId = persistentSettingsService.readStringValue(PERSISTENT_SETTINGS_KEY_PLATFORM_ID);
        }
        return nodeId;
    }

    private void parseNetworkConfiguration() {
        // "provided" (server) NCPs
        List<String> serverContactPointDefs = configuration.getProvidedContactPoints();
        log.info("Parsing " + serverContactPointDefs.size() + " server port entries");
        for (String contactPointDef : serverContactPointDefs) {
            NetworkContactPoint ncp;
            try {
                ncp = NetworkContactPointUtils.parseStringRepresentation(contactPointDef);
                log.debug("Adding configured server NCP " + ncp);
                serverContactPoints.add(ncp);
            } catch (IllegalArgumentException e) {
                log.error("Unable to parse contact point definition: " + contactPointDef);
            }
        }
        // "remote" (client) NCPs
        List<String> remoteContactPointDefs = configuration.getRemoteContactPoints();
        log.info("Parsing " + remoteContactPointDefs.size() + " network connection entries");
        for (String contactPointDef : remoteContactPointDefs) {
            NetworkContactPoint ncp;
            try {
                ncp = NetworkContactPointUtils.parseStringRepresentation(contactPointDef);
                log.debug("Adding configured remote NCP " + ncp);
                initialNetworkPeers.add(ncp);
            } catch (IllegalArgumentException e) {
                log.error("Unable to parse contact point definition: " + contactPointDef);
            }
        }
    }

    @Override
    public List<InitialSshConnectionConfig> getInitialSSHConnectionConfigs() {
        return sshConfiguration.getProvidedConnectionConfigs();
    }

    @Override
    public double[] getLocationCoordinates() {
        return configurationService.getLocationCoordinates();
    }

    @Override
    public String getLocationName() {
        return configurationService.getLocationName();
    }

    @Override
    public String getInstanceContact() {
        return configurationService.getInstanceContact();
    }

    @Override
    public String getInstanceAdditionalInformation() {
        return configurationService.getInstanceAdditionalInformation();
    }

    @Override
    public List<InitialUplinkConnectionConfig> getInitialUplinkConnectionConfigs() {
        return uplinkConfiguration.getProvidedConnectionConfigs();
    }

}
