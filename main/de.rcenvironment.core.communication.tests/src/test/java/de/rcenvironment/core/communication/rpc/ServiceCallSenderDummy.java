/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;
import de.rcenvironment.core.communication.rpc.internal.OSGiServiceCallHandlerImplTest;
import de.rcenvironment.core.communication.rpc.internal.ServiceCallSender;

/**
 * 
 * Dummy service call request sender to simulate endless loop.
 * 
 * @author Doreen Seider
 */
public class ServiceCallSenderDummy implements ServiceCallSender {

    @Override
    public ServiceCallResult send(ServiceCallRequest serviceCallRequest) throws CommunicationException {
        return OSGiServiceCallHandlerImplTest.getCallHandler().handle(serviceCallRequest);
    }

    @Override
    public void initialize(NetworkContact contact) throws CommunicationException {}

}
