/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.rpc.RemoteServiceCallService;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * Default {@link RemoteServiceCallService} implementation.
 * 
 * @author Robert Mischke
 */
public final class RemoteServiceCallServiceImpl implements RemoteServiceCallService {

    private MessageRoutingService routingService;

    public RemoteServiceCallServiceImpl() {}

    @Override
    public ServiceCallResult performRemoteServiceCall(ServiceCallRequest serviceCallRequest) throws CommunicationException {
        Future<NetworkResponse> responseFuture;
        try {
            byte[] serializedRequest = MessageUtils.serializeObject(serviceCallRequest);
            responseFuture =
                routingService.performRoutedRequest(serializedRequest, ProtocolConstants.VALUE_MESSAGE_TYPE_RPC,
                    serviceCallRequest.getRequestedPlatform());
        } catch (SerializationException e) {
            throw new CommunicationException("Failed to serialize service call request", e);
        }
        try {
            NetworkResponse networkResponse = responseFuture.get();
            // create a synthetic CommunicationException for errors that were not thrown by the
            // remote method or the remote service invoker (e.g. routing errors)
            if (!networkResponse.isSuccess()) {
                String errorMessage =
                    String.format("RPC call for method %s.%s() on %s failed with error code %s (trace: %s)",
                        serviceCallRequest.getService(), serviceCallRequest.getServiceMethod(), serviceCallRequest.getRequestedPlatform(),
                        networkResponse.getResultCode(), networkResponse.accessMetaData().getTrace());
                return new ServiceCallResult(new CommunicationException(errorMessage));
            }
            Serializable deserializedContent = networkResponse.getDeserializedContent();
            // TODO review: clarify local/remote exception result handling - misc_ro, Sept 2013
            if (deserializedContent instanceof Throwable) {
                // immediate fix (added in 3.2) to rule out ClassCastExceptions
                return new ServiceCallResult(deserializedContent);
            } else {
                return (ServiceCallResult) deserializedContent;
            }
        } catch (InterruptedException e) {
            throw new CommunicationException(e);
        } catch (ExecutionException e) {
            throw new CommunicationException(e);
        } catch (SerializationException e) {
            throw new CommunicationException(e);
        }
    }

    /**
     * Sets the {@link MessageRoutingService} implementation to use; called by OSGi-DS and unit tests.
     * 
     * @param newInstance the routing service implementation
     */
    public void bindMessageRoutingService(MessageRoutingService newInstance) {
        this.routingService = newInstance;
    }
}
