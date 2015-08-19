/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

/**
 * Callback interface to return {@link NetworkResponse}s received in return for
 * {@link NetworkRequest}s.
 * 
 * @author Robert Mischke
 */
public interface NetworkResponseHandler {

    /**
     * Signals a received {@link NetworkResponse}.
     * 
     * @param response the received response
     */
    void onResponseAvailable(NetworkResponse response);
}
