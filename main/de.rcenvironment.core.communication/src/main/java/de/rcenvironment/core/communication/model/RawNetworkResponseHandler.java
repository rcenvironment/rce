/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

/**
 * Internal callback interface for the communication bundle and transport implementations. It is
 * similar to the {@link NetworkResponseHandler} interface, but also provides a callback for
 * low-level connection breakdowns. These are handled internally by the communication layer, and are
 * not meant to propagate to the original request senders.
 * 
 * For these senders, connection breakdowns are converted into {@link NetworkResponse}s and returned
 * via the {@link NetworkResponseHandler#onResponseAvailable(NetworkResponse)} callback. If the
 * breakdown occurred on an outgoing connection, this will usually be followed by a
 * {@link MessageChannelLifecycleListener#onOutgoingChannelTerminated(MessageChannel)} callback.
 * 
 * @author Robert Mischke
 */
public interface RawNetworkResponseHandler {

    /**
     * Signals an available {@link NetworkResponse}.
     * 
     * @param response the received response
     */
    void onResponseAvailable(NetworkResponse response);

    /**
     * Signals a broken {@link MessageChannel}.
     * 
     * @param request the request associated with the detected breakdown, if available
     * @param connection the affected connection
     */
    void onChannelBroken(NetworkRequest request, MessageChannel connection);
}
