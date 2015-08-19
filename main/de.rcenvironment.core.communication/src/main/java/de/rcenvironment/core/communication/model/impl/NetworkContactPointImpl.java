/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.model.NetworkContactPoint;

/**
 * Default {@link NetworkContactPoint} implementation.
 * 
 * @author Robert Mischke
 */
public class NetworkContactPointImpl implements NetworkContactPoint, Serializable {

    private static final long serialVersionUID = 2534982536387750871L;

    private String host;

    private int port;

    private String transportId;

    private Map<String, String> attributes = Collections.unmodifiableMap(new HashMap<String, String>());

    /**
     * Default constructor for bean-style initialization.
     */
    protected NetworkContactPointImpl() {}

    /**
     * Standard constructor.
     * 
     * @param host the host string
     * @param port the port number
     * @param transportId the transport id
     */
    public NetworkContactPointImpl(String host, int port, String transportId) {
        this.host = host;
        this.port = port;
        this.transportId = transportId;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getTransportId() {
        return transportId;
    }

    public void setTransportId(String transportId) {
        this.transportId = transportId;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes; // read-only map
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    @Override
    public int hashCode() {
        return host.hashCode() ^ port ^ transportId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // FIXME make safer (compare class type and fields); improve performance
        return this.toString().equals(obj.toString());
    }

    @Override
    public String toString() {
        return String.format("%s:%d (%s)", host, port, transportId);
    }

}
