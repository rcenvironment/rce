/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.jetty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.mortbay.jetty.Request;

/**
 * Utility methods related to {@link JettyService} implementations. Currently, its single purpose is
 * to determine the IP addresses of SOAP clients.
 * 
 * @author Robert Mischke
 */
public abstract class JettyServiceUtils {

    private static final Log LOGGER = LogFactory.getLog(JettyServiceUtils.class);
    
    /**
     * Private constructor to satisfy Checkstyle.
     */
    private JettyServiceUtils() {
    }

    /**
     * Retrieves the client IP for a SOAP request that is currently being handled by the calling
     * thread.
     * 
     * @return the client IP for a current SOAP request being handled, or null if an error occured
     */
    public static String getClientIPForCurrentContext() {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message == null) {
            LOGGER.error("No SOAP message found in context while trying to determine client IP");
            return null;
        }
        Request object = (Request) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (object == null) {
            LOGGER.error("No HTTP request attached to SOAP message while trying to determine client IP");
            return null;
        }
        return object.getRemoteAddr();
    }

}
