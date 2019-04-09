/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.api;

import java.io.Serializable;
import java.util.List;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Remote-accessible methods for callback handling.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (extracted)
 */
@RemotableService
public interface RemotableCallbackService {

    /**
     * Invokes the method given by its name on the {@link Object} given by its identifier with the given parameter.
     * 
     * @param objectIdentifier Identifier of the object to call.
     * @param methodName Name of the method to call.
     * @param parameters Parameter of the method to call.
     * @return The return object of the method call.
     * @throws RemoteOperationException standard remote operation exception
     */
    Object callback(String objectIdentifier, String methodName, List<? extends Serializable> parameters) throws RemoteOperationException;

    /**
     * Sets the time to live for a bunch of objects represented by its identifier.
     * 
     * @param objectIdentifier The object's identifier to set the TTL for.
     * @param ttl The TTL to set.
     * @throws RemoteOperationException standard remote operation exception
     */
    void setTTL(String objectIdentifier, Long ttl) throws RemoteOperationException;

}
