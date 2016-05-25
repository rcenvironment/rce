/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import java.util.Map;

/**
 * Representation of a network "contact point", which consists of a host string, a port number, and
 * the id of the network transport to use.
 * 
 * @author Robert Mischke
 */
public interface NetworkContactPoint {

    /**
     * @return the host string
     */
    String getHost();

    /**
     * @return the port number
     */
    int getPort();

    /**
     * @return the opaque id of the transport to use for the connection
     */
    String getTransportId();
    
    /**
     * @return the map of additional attributes
     */
    Map<String, String> getAttributes();

}
