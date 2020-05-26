/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import de.rcenvironment.core.communication.protocol.ProtocolConstants.ResultCode;


/**
 * A connection-level response to a {@link NetworkRequest}.
 * 
 * @author Robert Mischke
 */
public interface NetworkResponse extends NetworkMessage {

    /**
     * @return the internal id associated with the original request; can be used to correlate
     *         responses to original requests
     */
    String getRequestId();

    /**
     * @return true if the request was successfully processed
     */
    boolean isSuccess();

    /**
     * @return the enum-wrapped result code; see enum for possible values
     */
    ResultCode getResultCode();

}
