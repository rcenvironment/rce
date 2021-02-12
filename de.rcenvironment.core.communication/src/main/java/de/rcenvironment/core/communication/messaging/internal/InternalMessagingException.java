/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import de.rcenvironment.core.communication.rpc.ServiceCallRequest;

/**
 * An exception to represent failures while handling {@link ServiceCallRequest}s on the receiving instance. These exceptions, their message
 * texts, and their embedded "cause" {@link Exception}s, are <b>never</b> intended to be sent back to the caller; if this is observed, it
 * should be considered an error.
 * 
 * @author Robert Mischke
 */
public class InternalMessagingException extends Exception {

    private static final long serialVersionUID = -916803304996017300L;

    public InternalMessagingException(String message, Exception e) {
        super(message, e);
    }

}
