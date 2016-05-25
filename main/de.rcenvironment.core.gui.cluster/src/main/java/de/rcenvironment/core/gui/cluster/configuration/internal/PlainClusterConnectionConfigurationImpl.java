/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.configuration.internal;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;


/**
 * Implementation of {@link PlainClusterConnectionConfiguration}.
 *
 * @author Doreen Seider
 */
public class PlainClusterConnectionConfigurationImpl implements PlainClusterConnectionConfiguration {

    private ClusterQueuingSystem clusterQueuingSystem;
    
    private Map<String, String> pathToClusterQueuingSystemCommands = new HashMap<>();

    private String host;

    private int port;

    private String username;

    private String configurationName;
    
    /**
     * Should only be used by JSON object mapper.
     */
    @Deprecated
    public PlainClusterConnectionConfigurationImpl() {}
    
    public PlainClusterConnectionConfigurationImpl(ClusterQueuingSystem queueingSystem, String host, int port,
        String username, String configurationName) {
        this.clusterQueuingSystem = queueingSystem;
        this.host = host;
        this.port = port;
        this.username = username;
        this.configurationName = configurationName;
    }
    
    @Override
    public ClusterQueuingSystem getClusterQueuingSystem() {
        return clusterQueuingSystem;
    }
    
    @Override
    public Map<String, String> getPathToClusterQueuingSystemCommands() {
        return pathToClusterQueuingSystemCommands;
    }
    
    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getConfigurationName() {
        return configurationName;
    }
    
    public void setClusterQueuingSystem(ClusterQueuingSystem clusterQueuingSystem) {
        this.clusterQueuingSystem = clusterQueuingSystem;
    }
    
    public void setPathToClusterQueuingSystemCommands(Map<String, String> pathToClusterQueuingSystemCommands) {
        this.pathToClusterQueuingSystemCommands = pathToClusterQueuingSystemCommands;
    }
    
    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }
    
    @Override
    public String toString() {
        if (configurationName != null && !configurationName.isEmpty()) {
            return configurationName;            
        } else {
            return username +  "@" + host;
        }
    }

}
