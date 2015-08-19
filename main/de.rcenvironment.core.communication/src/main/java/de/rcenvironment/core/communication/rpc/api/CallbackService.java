/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.api;

import java.io.Serializable;
import java.util.List;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.spi.CallbackObject;

/**
 * Service handling call backs from one platform to another.
 * 
 * @author Doreen Seider
 */
public interface CallbackService {

    /**
     * Adds an object so that call backs on this object can be invoked.
     * 
     * @param callBackObject The object to invoke call backs on.
     * @param nodeId The {@link NodeIdentifier} of the remote platform doing the
     *        call backs.
     * @return the object's identifier used for callback() and setTimeToLive().
     */
    String addCallbackObject(Object callBackObject, NodeIdentifier nodeId);

    /**
     * Gets an already added callback object.
     * 
     * @param objectIdentifier The identifier of the callback object.
     * @return the object or <code>null</code>, if there is none.
     */
    Object getCallbackObject(String objectIdentifier);

    /**
     * Gets the identifier of this already added callback object.
     * 
     * @param callbackObject The callback object to get the identifier for.
     * @return the object identifier or <code>null</code>, if the object is not present.
     */
    String getCallbackObjectIdentifier(Object callbackObject);

    /**
     * Sets the time to live for a bunch of objects represented by its identifier.
     * 
     * @param objectIdentifier The object's identifier to set the TTL for.
     * @param ttl The TTL to set.
     */
    void setTTL(String objectIdentifier, Long ttl);

    /**
     * Invokes the method given by its name on the {@link Object} given by its identifier with the
     * given parameter.
     * 
     * @param objectIdentifier Identifier of the object to call.
     * @param methodName Name of the method to call.
     * @param parameters Parameter of the method to call.
     * @return The return object of the method call.
     * @throws CommunicationException if calling back the object failed.
     */
    Object callback(String objectIdentifier, String methodName, List<? extends Serializable> parameters) throws CommunicationException;

    /**
     * Creates a proxy for an object, which needs to be called back. It delegates each method call
     * to callBack() of the {@link CallbackService}.
     * 
     * @param callbackObject The object to create a callback proxy for.
     * @param objectIdentifier The object's identifier.
     * @param proxyHome The {@link NodeIdentifier} of the remote platform where the proxy will
     *        be located then.
     * @return The created proxy object.
     */
    Object createCallbackProxy(CallbackObject callbackObject, final String objectIdentifier, NodeIdentifier proxyHome);
}
