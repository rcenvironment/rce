/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.jetty.internal;

import javax.jws.WebService;

/**
 * Dummy Web service interface for tests.
 * 
 * @author Tobias Menden
 */
@WebService
public interface WebCall {

    /**
     * Method of the Web service.
     * 
     * @param request The request parameter.
     * @return response The response parameter.
     */
    int call(int request);
}
