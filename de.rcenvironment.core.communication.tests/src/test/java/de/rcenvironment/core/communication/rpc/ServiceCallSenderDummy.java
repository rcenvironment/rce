/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;
import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.rpc.internal.OSGiServiceCallHandlerImplTest;
import de.rcenvironment.core.communication.rpc.internal.ServiceCallSender;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Dummy service call request sender to simulate endless loop.
 * 
 * @author Doreen Seider
 */
public class ServiceCallSenderDummy implements ServiceCallSender {

    @Override
    public ServiceCallResult send(ServiceCallRequest serviceCallRequest) throws RemoteOperationException {
        try {
            return OSGiServiceCallHandlerImplTest.getCallHandler().dispatchToLocalService(serviceCallRequest);
        } catch (InternalMessagingException e) {
            return ServiceCallResultFactory.representInternalErrorAtHandler(serviceCallRequest, "Exception in mock handler", e);
        }
    }

    @Override
    public void initialize(NetworkContact contact) throws CommunicationException {}

}
