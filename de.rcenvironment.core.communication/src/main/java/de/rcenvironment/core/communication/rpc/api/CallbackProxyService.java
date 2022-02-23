/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.api;

import de.rcenvironment.core.communication.rpc.internal.CallbackProxy;

/**
 * Service handling callbacks from one platform to another.
 * 
 * @author Doreen Seider
 */
public interface CallbackProxyService {

    /**
     * Adds a proxy so that callbacks on this object can be invoked.
     * 
     * @param callBackProxy {@link Object} to invoke callbacks on.
     */
    void addCallbackProxy(CallbackProxy callBackProxy);

    /**
     * Gets an already added proxy.
     * 
     * @param objectIdentifier The identifier of the proxied object.
     * @return the proxy or <code>null</code>, if there is none.
     */
    Object getCallbackProxy(String objectIdentifier);

    /**
     * Sets the time to live for a bunch of objects represented by its identifier.
     * 
     * @param objectIdentifier The object's identifier to set the TTL for.
     * @param ttl The TTL to set.
     */
    void setTTL(String objectIdentifier, Long ttl);

}
