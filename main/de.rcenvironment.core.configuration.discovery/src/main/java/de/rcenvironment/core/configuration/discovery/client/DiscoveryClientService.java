/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.client;

/**
 * The local service to query for discovery information.
 * 
 * @author Robert Mischke
 */
public interface DiscoveryClientService {

    /**
     * Tries to determine the external IP address of the local host from the given discovery
     * service. On failure, this method returns null.
     * 
     * @param address the host address of the discovery service
     * @param port the port of the discovery service
     * @return the determined IP, or null on failure
     */
    String getReflectedIpFromDiscoveryServer(String address, int port);

}
