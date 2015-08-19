/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.configuration.CommunicationConfiguration;
import de.rcenvironment.core.communication.configuration.CommunicationIPFilterConfiguration;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.impl.InitialNodeInformationImpl;
import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.utils.incubator.IdGenerator;

/**
 * Default {@link NodeConfigurationService} implementation.
 * 
 * @author Robert Mischke
 */
public class NodeConfigurationServiceImpl implements NodeConfigurationService {

    /**
     * Constant for the name of the configuration file.
     */
    public static final String IP_WHILTELIST_CONFIGURATION_ID = "de.rcenvironment.core.communication.ipfilter";

    // TODO temporary hardcoded default for actual RCE instances; see Mantis #8074
    private static final int STARTUP_INITIAL_CONNECT_DELAY_MSEC = 2500;

    /**
     * A system property to specify a certain node id for testing. Example usage in command line:
     * "-Drce.network.overrideNodeId=12312312312312312312312312312312"
     */
    private static final String SYSTEM_PROPERTY_OVERRIDE_NODE_ID = "rce.network.overrideNodeId";

    /**
     * Regular expression for the node id override value.
     */
    private static final String NODE_ID_OVERRIDE_PATTERN = "[0-9a-f]{32}";

    private static final String PERSISTENT_SETTINGS_KEY_PLATFORM_ID = "rce.network.nodeId";

    private final List<NetworkContactPoint> serverContactPoints;

    private final List<NetworkContactPoint> initialNetworkPeers;

    private InitialNodeInformationImpl localNodeInformation;

    private CommunicationConfiguration configuration;

    private ConfigurationService configurationService;

    private PersistentSettingsService persistentSettingsService;

    private CommunicationIPFilterConfiguration ipFilterConfiguration;

    private boolean initialIPFilterConfigLoaded = false;

    private final Log log = LogFactory.getLog(getClass());

    private boolean localNodeIsRelay;

    public NodeConfigurationServiceImpl() {
        serverContactPoints = new ArrayList<NetworkContactPoint>();
        initialNetworkPeers = new ArrayList<NetworkContactPoint>();
    }

    @Override
    public NodeIdentifier getLocalNodeId() {
        return localNodeInformation.getNodeId();
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
    public long getForwardingTimeoutMsec() {
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
        log.info(String.format("IP filter enabled: %s, configured number of IPs: %d",
            ipFilterConfiguration.getEnabled(), ipFilterConfiguration.getAllowedIPs().size()));
        return ipFilterConfiguration;
    }

    /**
     * OSGi-DS lifecycle method; made public for unit testing.
     * 
     * @param context OSGi {@link BundleContext}
     */
    public void activate(BundleContext context) {
        ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("network");
        configuration = new CommunicationConfiguration(configurationSegment);
        createLocalNodeInformation();
        parseNetworkConfiguration();
        localNodeIsRelay = configurationService.getIsRelay();
        log.info("Local 'isRelay' setting: " + isRelay());
    }

    /**
     * OSGi-DS bind method; made public for unit testing.
     * 
     * @param newConfigurationService the new {@link ConfigurationService} to set
     */
    public void bindConfigurationService(ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }

    /**
     * OSGi-DS bind method; made public for unit testing.
     * 
     * @param newPersistentSettingsService the new {@link PersistentSettingsService} to set
     */
    public void bindPersistentSettingsService(PersistentSettingsService newPersistentSettingsService) {
        persistentSettingsService = newPersistentSettingsService;
    }

    private void createLocalNodeInformation() {
        // check if a node id override is defined
        String nodeId = System.getProperty(SYSTEM_PROPERTY_OVERRIDE_NODE_ID);
        if (nodeId != null) {
            // validate id form
            if (nodeId.matches(NODE_ID_OVERRIDE_PATTERN)) {
                log.info("Overriding node id: " + nodeId);
            } else {
                log.warn("Ignoring node id override (property '" + SYSTEM_PROPERTY_OVERRIDE_NODE_ID
                    + "') as it does not match the pattern '" + NODE_ID_OVERRIDE_PATTERN + "': " + nodeId);
                // reset to null; this causes fallback to the normal startup behavior
                nodeId = null;
            }
        }
        // standard procedure
        if (nodeId == null) {
            // check for existing persistent node id
            nodeId = persistentSettingsService.readStringValue(PERSISTENT_SETTINGS_KEY_PLATFORM_ID);
            if (nodeId == null) {
                // not found -> generate and save
                nodeId = IdGenerator.randomUUIDWithoutDashes();
                persistentSettingsService.saveStringValue(PERSISTENT_SETTINGS_KEY_PLATFORM_ID, nodeId);
                log.info("Generated and stored id " + nodeId + " for the local node");
            } else {
                log.info("Reusing the previously stored id " + nodeId + " for the local node");
            }
        }

        String instanceName = configurationService.getInstanceName();
        boolean isWorkflowHost = configurationService.getIsWorkflowHost();

        localNodeInformation = new InitialNodeInformationImpl(nodeId);
        localNodeInformation.setDisplayName(instanceName);
        localNodeInformation.setIsWorkflowHost(isWorkflowHost);
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

}
