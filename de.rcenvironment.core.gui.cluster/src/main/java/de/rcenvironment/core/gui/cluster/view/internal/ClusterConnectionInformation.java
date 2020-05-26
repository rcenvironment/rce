/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.view.internal;

import java.util.Date;

import de.rcenvironment.core.gui.cluster.configuration.internal.ClusterConnectionConfiguration;



/**
 * Holds information about an established cluster connection.
 *
 * @author Doreen Seider
 */
public class ClusterConnectionInformation {

    private ClusterConnectionConfiguration configuration;
    
    private Date connected;
    
    private Date lastUpdate;
    
    private int updateInterval;

    public ClusterConnectionInformation(ClusterConnectionConfiguration configuration, Date connectedDate) {
        super();
        this.configuration = configuration;
        this.connected = connectedDate;
    }

    public String getHost() {
        return configuration.getHost();
    }
    
    public String getUsername() {
        return configuration.getUsername();
    }
    
    public Date getConnectedDate() {
        return connected;
    }

    public Date getLastUpdateDate() {
        return lastUpdate;
    }
    
    protected void setLastUpdate(Date lastUpdateDate) {
        this.lastUpdate = lastUpdateDate;
    }
    
    public int getUpdateInterval() {
        return updateInterval;
    }
    
    protected void setUpdateInterval(int updateIntervalInSec) {
        this.updateInterval = updateIntervalInSec ;
    }
    
}
