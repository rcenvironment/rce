/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.instancemanagement.internal;


/**
 * 
 * Container for ssh connection.
 *
 * @author David Scholz
 */
public class ConfigurationSshConnection {

    private final String name;
    
    private final String displayName;
    
    private final String host;
    
    private final int port;
    
    private final String loginName;
    
    public ConfigurationSshConnection(String name, String displayName, String host, int port, String loginName) {
        this.name = name;
        this.displayName = displayName;
        this.host = host;
        this.port = port;
        this.loginName = loginName;
    }
       
    public String getName() {
        return name;
    }

    
    public String getDisplayName() {
        return displayName;
    }

    
    public String getHost() {
        return host;
    }

    
    public int getPort() {
        return port;
    }

    
    public String getLoginName() {
        return loginName;
    }
}
