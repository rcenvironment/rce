/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.configuration.internal;

import java.util.Map;

import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;


/**
 * Holds information about a cluster connection configuration.
 *
 * @author Doreen Seider
 */
public class ClusterConnectionConfiguration implements PlainClusterConnectionConfiguration, SensitiveClusterConnectionConfiguration {

    private PlainClusterConnectionConfiguration plainConfiguration;
    
    private SensitiveClusterConnectionConfiguration sensitiveConfiguration;

    public ClusterConnectionConfiguration(ClusterQueuingSystem queueingSystem, Map<String, String> pathToClusterQueuingSystemCommands,
        String host, int port, String username, String configurationName, String password) {
        this.plainConfiguration = new PlainClusterConnectionConfigurationImpl(queueingSystem, host, port, username, configurationName);
        ((PlainClusterConnectionConfigurationImpl) this.plainConfiguration)
            .setPathToClusterQueuingSystemCommands(pathToClusterQueuingSystemCommands);
        this.sensitiveConfiguration = new SensitiveClusterConnectionConfigurationImpl(password);
        ((SensitiveClusterConnectionConfigurationImpl) sensitiveConfiguration).setKey(plainConfiguration.getUsername()
            +  "@" + plainConfiguration.getHost() + ":" + plainConfiguration.getPort());
    }
    
    public ClusterConnectionConfiguration(ClusterQueuingSystem queueingSystem, Map<String, String> pathToClusterQueuingSystemCommands,
        String host, int port, String username, String configurationName) {
        this(queueingSystem, pathToClusterQueuingSystemCommands, host, port, username, configurationName, null);
    }
    
    @Override
    public String toString() {
        if (plainConfiguration.getConfigurationName() != null && !plainConfiguration.getConfigurationName().isEmpty()) {
            return plainConfiguration.getConfigurationName();            
        } else {
            return plainConfiguration.getUsername() +  "@" + plainConfiguration.getHost();
        }
    }

    @Override
    public String getKey() {
        return sensitiveConfiguration.getKey();
    }

    @Override
    public String getPassword() {
        return sensitiveConfiguration.getPassword();
    }

    @Override
    public ClusterQueuingSystem getClusterQueuingSystem() {
        return plainConfiguration.getClusterQueuingSystem();
    }
    
    @Override
    public Map<String, String> getPathToClusterQueuingSystemCommands() {
        return plainConfiguration.getPathToClusterQueuingSystemCommands();
    }
    
    @Override
    public String getHost() {
        return plainConfiguration.getHost();
    }

    @Override
    public int getPort() {
        return plainConfiguration.getPort();
    }

    @Override
    public String getUsername() {
        return plainConfiguration.getUsername();
    }

    @Override
    public String getConfigurationName() {
        return plainConfiguration.getConfigurationName();
    }

    @Override
    public void setPassword(String password) {
        sensitiveConfiguration.setPassword(password);
    }
    
    protected PlainClusterConnectionConfiguration getPlainClusterConnectionConfiguration() {
        return plainConfiguration;
    }
    
    protected SensitiveClusterConnectionConfiguration getSensitiveClusterConnectionConfiguration() {
        return sensitiveConfiguration;
    }

}
