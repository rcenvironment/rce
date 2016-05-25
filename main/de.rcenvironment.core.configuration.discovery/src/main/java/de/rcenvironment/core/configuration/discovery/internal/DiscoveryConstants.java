/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.internal;

/**
 * Discovery-related constants.
 * 
 * @author Robert Mischke
 */
public abstract class DiscoveryConstants {

    /**
     * The format string for the SOAP discovery service URL. Takes two paramters: the server
     * address, and the server port.
     */
    public static final String SOAP_SERVICE_URL_PATTERN = "http://%s:%d/Discovery";

    /**
     * Private constructor to satisfy Checkstyle.
     */
    private DiscoveryConstants() {}

}
