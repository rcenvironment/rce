/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.server;

/**
 * Local service for running a discovery server.
 * 
 * @author Robert Mischke
 */
public interface DiscoveryServerManagementService {

    /**
     * Starts a discovery service at the given address and port. The address must be a valid to bind
     * a local server socket to.
     * 
     * @param address the IP address to use for the new server
     * @param port the port to use for the new server
     */
    void startServer(String address, int port);
}
