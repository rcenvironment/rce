/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

/**
 * A connection-level request.
 * 
 * @author Robert Mischke
 */
public interface NetworkRequest extends NetworkMessage {

    /**
     * @return the internal id associated with this request
     */
    String getRequestId();

}
