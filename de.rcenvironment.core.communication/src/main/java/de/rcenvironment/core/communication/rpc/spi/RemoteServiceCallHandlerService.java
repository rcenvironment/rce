/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.spi;

import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;

/**
 * This class is responsible for handling a service call request correctly depending on the {@link ServiceCallRequest}. It can handles it
 * locally or forwards it. In both cases it returns the result object packed in a {@link ServiceCallResult}.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke
 */
// TODO rename to ServiceCallHandlerService for clarity
public interface RemoteServiceCallHandlerService {

    /**
     * Dispatches the service call to the appropriate local service.
     * 
     * @param serviceCallRequest {@link ServiceCallRequest} with all information about the method to call.
     * @return The {@link ServiceCallResult} with the result of the method call.
     * @throws InternalMessagingException on unhandled internal exceptions
     */
    ServiceCallResult dispatchToLocalService(ServiceCallRequest serviceCallRequest) throws InternalMessagingException;
}
