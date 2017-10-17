/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.instancemanagement.internal;

/**
 * 
 * Encapsules a connection in the configuration.
 *
 * @author David Scholz
 */
public class ConfigurationConnection {
    
    private final String connectionName;

    private final String host;

    private final int port;

    private final boolean connectOnStartup;

    private final long autoRetryInitialDelay;

    private final long autoRetryMaximumDelay;

    private final int autoRetryDelayMultiplier;

    public ConfigurationConnection(String connectionName, String host, int port, boolean connectOnStartup, long autoRetryInitialDelay,
        long autoRetryMaximumDelay, int autoRetryDelayMultiplier) {
        this.connectionName = connectionName;
        this.host = host;
        this.port = port;
        this.connectOnStartup = connectOnStartup;
        this.autoRetryInitialDelay = autoRetryInitialDelay;
        this.autoRetryMaximumDelay = autoRetryMaximumDelay;
        this.autoRetryDelayMultiplier = autoRetryDelayMultiplier;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean getConnectOnStartup() {
        return connectOnStartup;
    }

    public long getAutoRetryInitialDelay() {
        return autoRetryInitialDelay;
    }

    public long getAutoRetryMaximumDelay() {
        return autoRetryMaximumDelay;
    }

    public int getAutoRetryDelayMultiplier() {
        return autoRetryDelayMultiplier;
    }

}
