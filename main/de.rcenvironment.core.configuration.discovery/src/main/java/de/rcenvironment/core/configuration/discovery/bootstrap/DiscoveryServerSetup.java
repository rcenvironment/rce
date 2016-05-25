/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.bootstrap;

import java.io.Serializable;

/**
 * Defines if and how a discovery server should should run; usually used as part of a
 * {@link DiscoveryConfiguration}.
 * 
 * @author Robert Mischke
 */
public class DiscoveryServerSetup implements Serializable {

    private static final long serialVersionUID = 14975985569812996L;

    private static final String DEFAULT_ADDRESS = "0.0.0.0";

    private static final int DEFAULT_PORT = 8081;

    private String bindAddress = DEFAULT_ADDRESS;

    private int port = DEFAULT_PORT;

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String address) {
        this.bindAddress = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
