/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link ConnectionFilter} that filters against a provided list of whitelisted IPs.
 * 
 * @author Robert Mischke
 */
public class IPWhitelistConnectionFilter implements ConnectionFilter {

    private Set<String> acceptedIps = new HashSet<String>();

    @Override
    public synchronized boolean isIpAllowedToConnect(String ip) {
        return (acceptedIps == null) || acceptedIps.contains(ip);
    }

    /**
     * Changes the configuration of this filter. This method is synchronized against concurrent queries. Using null as a parameter sets the
     * filter into "allow all" mode.
     * 
     * @param newAcceptedIps a collection of IPs to whitelist/accept/allow (the collection will be copied, not used); pass null to
     *        deactivate filtering (allow all)
     */
    public synchronized void configure(Collection<String> newAcceptedIps) {
        if (newAcceptedIps != null) {
            this.acceptedIps = new HashSet<String>(newAcceptedIps);
        } else {
            this.acceptedIps = null;
        }
    }

    /**
     * @return a detached copy of the current list of whitelisted IPs, or "null" if the filter is in "allow all" mode
     */
    public synchronized Set<String> getAcceptedIps() {
        if (acceptedIps == null) {
            return null;
        }
        return new HashSet<String>(acceptedIps);
    }

}
