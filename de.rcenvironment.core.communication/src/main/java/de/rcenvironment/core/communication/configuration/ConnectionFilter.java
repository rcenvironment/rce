/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

/**
 * A generic interface for filtering inbound P2P connections. The current implementation filters by the remote peer's IP address, but could
 * be extended to other attributes.
 * 
 * @author Robert Mischke
 */
public interface ConnectionFilter {

    /**
     * @param ip the remote peer's IP address; currently, only numeric IPv4 addresses are supported
     * @return true if this IP address should be allowed to connect to local contact points
     */
    boolean isIpAllowedToConnect(String ip);
}
