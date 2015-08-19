/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of the communication bundle IP filter.
 * 
 * @author Robert Mischke
 */
public class CommunicationIPFilterConfiguration {

    private boolean enabled = false; // default: no filtering

    private List<String> allowedIPs = new ArrayList<String>();

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enableIPFiltering) {
        this.enabled = enableIPFiltering;
    }

    public List<String> getAllowedIPs() {
        return allowedIPs;
    }

    public void setAllowedIPs(List<String> allowedIPs) {
        this.allowedIPs = allowedIPs;
    }

}
