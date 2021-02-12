/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import java.io.File;
import java.util.List;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.sshconnection.InitialSshConnectionConfig;
import de.rcenvironment.core.communication.sshconnection.InitialUplinkConnectionConfig;

/**
 * Configuration management service for the local node. It serves to decouple the communication classes from the low-level
 * {@link CommunicationConfiguration} class, which simplifies the configuration of integration tests.
 * 
 * @author Robert Mischke
 */
public interface NodeConfigurationService {

    /**
     * A system property to specify a certain node id for testing. Example usage in command line:
     * "-Drce.network.overrideNodeId=12312312312312312312312312312312"
     */
    String SYSTEM_PROPERTY_OVERRIDE_NODE_ID = "rce.network.overrideNodeId";

    /**
     * Regular expression for the node id override value.
     */
    String NODE_ID_OVERRIDE_PATTERN = "[0-9a-f]{32}";

    /**
     * A system property that forces local RPCs to be sent through message serialization (to catch serialization issues in local testing).
     */
    String SYSTEM_PROPERTY_FORCE_LOCAL_RPC_SERIALIZATION = "rce.internal.forceLocalRPCSerialization";

    /**
     * Storage property key for the auto-generated local node id.
     */
    String PERSISTENT_SETTINGS_KEY_PLATFORM_ID = "rce.network.nodeId";

    /**
     * @return the identifier of the local node
     */
    InstanceNodeSessionId getInstanceNodeSessionId();

    /**
     * @return true if this node is a "workflow host"; temporary pass-through method
     */
    @Deprecated
    boolean isWorkflowHost();

    /**
     * @return the {@link NodeIdentifierService} implementation for this instance
     */
    NodeIdentifierService getNodeIdentifierService();

    /**
     * @return an {@link InitialNodeInformation} object for the local node
     */
    InitialNodeInformation getInitialNodeInformation();

    /**
     * @return the list of "provided" {@link NetworkContactPoint}s for the local node; these are the {@link NetworkContactPoint}s that the
     *         local node listens on as a "server"
     */
    List<NetworkContactPoint> getServerContactPoints();

    /**
     * @return the list of "remote" {@link NetworkContactPoint}s for the local node; these are the {@link NetworkContactPoint}s that the
     *         local node connects to as a "client"
     */
    List<NetworkContactPoint> getInitialNetworkContactPoints();

    /**
     * @return true if this node reports its outgoing message channels to other nodes, and accepts message forwarding requests from other
     *         nodes; in effect, setting this to "true" makes this node merge all networks it is connected to into one
     */
    boolean isRelay();

    /**
     * @return the delay (in milliseconds) before connections to the configured "remote contact points" are attempted
     */
    long getDelayBeforeStartupConnectAttempts();

    /**
     * @return the timeout for a request/response cycle on the initiating node
     */
    int getRequestTimeoutMsec();

    /**
     * @return the timeout for a request/response cycle on a forwarding/routing node
     */
    int getForwardingTimeoutMsec();

    /**
     * @return the current IP filter configuration; the implementing service is responsible for returning the most up-to-date configuration,
     *         for example by reloading a configuration file
     */
    CommunicationIPFilterConfiguration getIPFilterConfiguration();

    /**
     * @return The list of configured ssh connections.
     */
    List<InitialSshConnectionConfig> getInitialSSHConnectionConfigs();

    /**
     * @return The list of configured ssh connections.
     */
    List<InitialUplinkConnectionConfig> getInitialUplinkConnectionConfigs();

    /**
     * Longitude and latitude values of the instance.
     * 
     * @return non null 2-dim array
     */
    double[] getLocationCoordinates();

    /**
     * Name of the location.
     * 
     * @return name
     */
    String getLocationName();

    /**
     * Contact information for the instance.
     * 
     * @return contact string
     */
    String getInstanceContact();

    /**
     * Some information about the instance.
     * 
     * @return information string
     */
    String getInstanceAdditionalInformation();

    /**
     * @param subdir the requested subdirectory within the profile's import directory
     * @return the path of the profile's standard import directory for the given subdir part (currently, this is &lt;profile>/import/xy for
     *         subdir value "xy"); no check is made whether this directory actually exists
     */
    File getStandardImportDirectory(String subdir);

}
