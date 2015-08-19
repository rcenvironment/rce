/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.jetty.internal;

import javax.jws.WebService;

/**
 * Implemenation of {@link WebCall}.
 * 
 * @author Tobias Menden
 */
@WebService(endpointInterface = "de.rcenvironment.core.jetty.internal.WebCall", serviceName = "WebCallTest")
public class WebCallImpl implements WebCall {
    
    private static final int ADDITION = 666;
    
    @Override
    public int call(int request) {
        return request + ADDITION;
    }
    
}
