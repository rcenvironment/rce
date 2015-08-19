/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import de.rcenvironment.core.communication.common.CommunicationException;

/**
 * This class is responsible for handling a service call request correctly depending on the
 * {@link ServiceCallRequest}. It can handles it locally or forwards it. In both cases it returns
 * the result object packed in a {@link ServiceCallResult}.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public interface ServiceCallHandler {

    /**
     * Handles the incoming service call requests. It checks if it has to be done local or will be
     * forwarded.
     * 
     * @param serviceCallRequest {@link ServiceCallRequest} with all information about the method to
     *        call.
     * @return The {@link ServiceCallResult} with the result of the method call.
     * @throws CommunicationException Thrown if the call failed.
     */
    ServiceCallResult handle(ServiceCallRequest serviceCallRequest) throws CommunicationException;
}
