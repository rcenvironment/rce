/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.internal;

import javax.jws.WebService;

/**
 * Contract interface of the remote discovery call. Currently provided as a SOAP service.
 * 
 * @author Robert Mischke
 */
@WebService
public interface RemoteDiscoveryService {

    /**
     * Makes a best-effort attempt to determine the IP address of the remote caller. The format of
     * the returned address is the same as returned by javax.servlet.ServletRequest#getRemoteAddr();
     * usually, this is the numeric form of the IP address.
     * 
     * May return null if the address could not be determined.
     * 
     * @return the caller IP address, or null if it could not be determined
     */
    String getReflectedCallerAddress();
}
