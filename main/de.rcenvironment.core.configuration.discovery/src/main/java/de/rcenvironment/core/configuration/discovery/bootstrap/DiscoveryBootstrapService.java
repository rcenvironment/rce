/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.bootstrap;

import java.util.Map;

/**
 * A service that allows external bundles to initialize the discovery setup process. This is
 * necessary to avoid cyclic coupling with the RCE Configuration Service component.
 * 
 * @author Robert Mischke
 * 
 */
public interface DiscoveryBootstrapService {

    /**
     * The property key for the external address of the local host, as determined by a remote
     * discovery server. This property is surrounded by double quotes so it can be easily embedded
     * in JSON configuration files. If necessary, a separate property could provide the "raw" value.
     */
    String QUOTED_REFLECTED_CLIENT_ADDRESS_PROPERTY = "reflectedClientAddress";

    /**
     * Initializes the discovery system. This may include starting a local discovery server and/or
     * query existing discovery servers.
     * 
     * The return value is a map of the property values learned from remote discovery servers. If a
     * remote server was contacted and it was able to report the external address of the local
     * client, this address (with double quotes) is available under the
     * QUOTED_REFLECTED_CLIENT_ADDRESS_PROPERTY key.
     * 
     * @param configuration the discovery configuration to use
     * @return the map of discovery values learned from remote discovery servers
     */
    Map<String, String> initializeDiscovery(DiscoveryConfiguration configuration);

    /**
     * @return the symbolic name of the containing discovery bundle
     */
    String getSymbolicBundleName();

}
