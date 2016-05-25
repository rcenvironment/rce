/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
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
public interface RemoteServiceCallHandlerService {

    /**
     * Handles the incoming service call requests. It checks if it has to be done local or will be forwarded.
     * 
     * @param serviceCallRequest {@link ServiceCallRequest} with all information about the method to call.
     * @return The {@link ServiceCallResult} with the result of the method call.
     * @throws InternalMessagingException on unhandled internal exceptions
     */
    ServiceCallResult handle(ServiceCallRequest serviceCallRequest) throws InternalMessagingException;
}
