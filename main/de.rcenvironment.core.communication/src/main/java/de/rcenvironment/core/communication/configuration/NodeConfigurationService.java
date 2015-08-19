/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import java.util.List;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;

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
    NodeIdentifier getLocalNodeId();

    /**
     * @return true if this node is a "workflow host"; temporary pass-through method
     */
    @Deprecated
    boolean isWorkflowHost();

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
    long getForwardingTimeoutMsec();

    /**
     * @return the current IP filter configuration; the implementing service is responsible for returning the most up-to-date configuration,
     *         for example by reloading a configuration file
     */
    CommunicationIPFilterConfiguration getIPFilterConfiguration();
}
