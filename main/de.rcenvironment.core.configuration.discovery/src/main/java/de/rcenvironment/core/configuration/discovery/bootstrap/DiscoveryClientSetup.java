/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines if and how discovery services should be used; usually used as part of a
 * {@link DiscoveryConfiguration}.
 * 
 * @author Robert Mischke
 */
public class DiscoveryClientSetup implements Serializable {

    /**
     * Represents a single remote discovery server.
     * 
     * @author Robert Mischke
     * 
     */
    public static class ServerEntry {

        private static final String DEFAULT_ADDRESS = "0.0.0.0";

        private static final int DEFAULT_PORT = 8889; // slightly uncommon value for visibility

        private String address = DEFAULT_ADDRESS;

        private int port = DEFAULT_PORT;

        public ServerEntry() {}

        public ServerEntry(String address, int port) {
            this.address = address;
            this.port = port;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    private static final long serialVersionUID = 2632682027397966277L;

    private List<ServerEntry> servers = new ArrayList<ServerEntry>();

    private Map<String, String> fallbackProperties = new HashMap<String, String>();

    public List<ServerEntry> getServers() {
        return Collections.unmodifiableList(servers);
    }

    public void setServers(List<ServerEntry> servers) {
        this.servers = servers;
    }

    public Map<String, String> getFallbackProperties() {
        return Collections.unmodifiableMap(fallbackProperties);
    }

    public void setFallbackProperties(Map<String, String> fallbackProperties) {
        this.fallbackProperties = fallbackProperties;
    }

}
