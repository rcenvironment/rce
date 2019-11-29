/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.instancemanagement.internal;

/**
 * Container for uplink connection parameters.
 *
 * @author Brigitte Boden
 */
public class ConfigurationUplinkConnection {
    
    private String id;
    private String host;
    private int port;
    private String user;
    private String displayName;
    private String clientID;
    private String keyFileLocation;
    private boolean connectOnStartup;
    private boolean autoRetry;
    private boolean isGateway;
    private String password;
    
    public ConfigurationUplinkConnection(String id, String host, int port, String user, String displayName, String clientID,
        String keyFileLocation, boolean connectOnStartup, boolean autoRetry, boolean isGateway, String password) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.user = user;
        this.displayName = displayName;
        this.clientID = clientID;
        this.keyFileLocation = keyFileLocation;
        this.connectOnStartup = connectOnStartup;
        this.autoRetry = autoRetry;
        this.isGateway = isGateway;
        this.password = password;
    }

    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getUser() {
        return user;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getId() {
        return id;
    }

    public String getKeyFileLocation() {
        return keyFileLocation;
    }

    public boolean getConnectOnStartup() {
        return connectOnStartup;
    }

    public boolean getAutoRetry() {
        return autoRetry;
    }

    public String getClientID() {
        return clientID;
    }

    public boolean isGateway() {
        return isGateway;
    }
    
    public String getPassword() {
        return password;
    }
}
