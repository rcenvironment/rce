/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

/**
 * A connection-level request.
 * 
 * @author Robert Mischke
 */
public interface NetworkRequest extends NetworkMessage {

    /**
     * The length of request id strings as returned by {@link #getRequestId()}.
     */
    int REQUEST_ID_LENGTH = 32;

    /**
     * @return the internal id associated with this request
     */
    String getRequestId();

}
