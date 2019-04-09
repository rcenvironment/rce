/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.api;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.spi.CallbackObject;

/**
 * Service handling call backs from one platform to another.
 * 
 * @author Doreen Seider
 */
public interface CallbackService extends RemotableCallbackService {

    /**
     * Adds an object so that call backs on this object can be invoked.
     * 
     * @param callBackObject The object to invoke call backs on.
     * @param nodeId The {@link InstanceNodeSessionId} of the remote platform doing the call backs.
     * @return the object's identifier used for callback() and setTimeToLive().
     */
    String addCallbackObject(Object callBackObject, InstanceNodeSessionId nodeId);

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
     * Creates a proxy for an object, which needs to be called back. It delegates each method call to callBack() of the
     * {@link CallbackService}.
     * 
     * @param callbackObject The object to create a callback proxy for.
     * @param objectIdentifier The object's identifier.
     * @param proxyHome The {@link InstanceNodeSessionId} of the remote platform where the proxy will be located then.
     * @return The created proxy object.
     */
    Object createCallbackProxy(CallbackObject callbackObject, final String objectIdentifier, InstanceNodeSessionId proxyHome);
}
