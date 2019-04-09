/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

/**
 * A default {@link ConnectionFilter} that allows all inbound connections.
 * 
 * @author Robert Mischke
 */
public class InactiveConnectionFilter implements ConnectionFilter {

    @Override
    public boolean isIpAllowedToConnect(String ip) {
        return true;
    }

}
