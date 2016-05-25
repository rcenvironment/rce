/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.communication.sshconnection;

/**
 * Internal representation of configuration for one SSH connection.
 *
 * @author Brigitte Boden
 */
public class InitialSshConnectionConfig {
    
    private String id;
    private String host;
    private int port;
    private String user;
    private String displayName;
    private String keyFileLocation;
    private boolean usePassphrase;
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    
    public String getId() {
        return id;
    }

    
    public void setId(String id) {
        this.id = id;
    }

    public String getKeyFileLocation() {
        return keyFileLocation;
    }

    public void setKeyFileLocation(String keyFileLocation) {
        this.keyFileLocation = keyFileLocation;
    }

    public boolean getUsePassphrase() {
        return usePassphrase;
    }

    public void setUsePassphrase(boolean usePassphrase) {
        this.usePassphrase = usePassphrase;
    }
    
}
