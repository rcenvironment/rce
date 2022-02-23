/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceCallResultFactory;
import de.rcenvironment.core.communication.rpc.api.RemoteServiceCallSenderService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.Assertions;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Default {@link RemoteServiceCallSenderService} implementation.
 * 
 * @author Robert Mischke
 */
@Component
public final class RemoteServiceCallSenderServiceImpl implements RemoteServiceCallSenderService {

    private MessageRoutingService routingService;

    private ReliableRPCStreamService reliableRPCStreamService;

    // NOTE: used in several locations
    private final boolean forceLocalRPCSerialization = System
        .getProperty(NodeConfigurationService.SYSTEM_PROPERTY_FORCE_LOCAL_RPC_SERIALIZATION) != null;

    private final boolean verboseRequestLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("RemoteServiceCalls");

    private final ServiceCallSerializer serviceCallSerializer = new ServiceCallSerializer();

    private final Log log = LogFactory.getLog(getClass());

    public RemoteServiceCallSenderServiceImpl() {}

    @Override
    public ServiceCallResult performRemoteServiceCall(ServiceCallRequest serviceCallRequest) {
        try {
            if (forceLocalRPCSerialization) { // TODO review: shouldn't this check if this is an actual local call?
                log.debug(StringUtils.format("Handling local RPC with forced serialization: %s#%s()", serviceCallRequest.getServiceName(),
                    serviceCallRequest.getMethodName()));
            }

            final NetworkResponse networkResponse;
            if (serviceCallRequest.getReliableRPCStreamId() != null) {
                return reliableRPCStreamService.performRequest(serviceCallRequest);
            } else {
                // perform a direct call without reliability and ordering guarantees
                byte[] serializedRequest = serviceCallSerializer.getSerializedForm(serviceCallRequest);
                networkResponse = routingService.performRoutedRequest(serializedRequest, ProtocolConstants.VALUE_MESSAGE_TYPE_RPC,
                    serviceCallRequest.getTargetNodeId().convertToInstanceNodeSessionId());
                return deserializeSCRNetworkResponse(serviceCallRequest, networkResponse);
            }
        } catch (SerializationException | RuntimeException e) {
            return ServiceCallResultFactory.representInternalErrorAtSender(serviceCallRequest,
                "Uncaught exception while performing a service call to " + formatServiceRequest(serviceCallRequest), e);
        }
    }

    @Override
    public Object performRemoteServiceCallAsProxy(ServiceCallRequest serviceCallRequest) throws Throwable {
        ServiceCallResult serviceCallResult = performRemoteServiceCall(serviceCallRequest);

        if (serviceCallResult == null) {
            // this should not happen anymore; left in for safety
            throw new RemoteOperationException(StringUtils.format(
                "Received a null object as result for service call to %s", formatServiceRequest(serviceCallRequest)));
        }

        if (serviceCallResult.isSuccess()) {
            return serviceCallResult.getReturnValue();
        } else if (serviceCallResult.isRemoteOperationException()) {
            final String errorMessage = serviceCallResult.getRemoteOperationExceptionMessage();
            // the exception will usually cause an upstream warning already, so log the details at DEBUG level
            log.debug(StringUtils.format("A remote call to %s#%s() on %s failed: %s", serviceCallRequest.getServiceName(),
                serviceCallRequest.getMethodName(), serviceCallRequest.getTargetNodeId(), errorMessage));
            throw new RemoteOperationException(StringUtils.format("%s; the destination instance was %s", errorMessage,
                serviceCallRequest.getTargetNodeId()));
        } else {
            final Throwable methodException = reconstructMethodException(serviceCallRequest, serviceCallResult);
            log.debug(StringUtils.format("Re-throwing method exception returned from a from call to %s: %s",
                formatServiceRequest(serviceCallRequest), methodException.toString()));
            // note: equality check is safe here, as ROEs are always constructed locally
            if (methodException.getClass() == RemoteOperationException.class) {
                // re-wrap ROEs to add destination node information to message
                throw new RemoteOperationException(StringUtils.format("%s; the destination instance was %s", methodException.getMessage(),
                    serviceCallRequest.getTargetNodeId()));
            } else {
                throw methodException;
            }
        }
    }

    // TODO (p2) avoid static method or move it to a utils class
    protected static ServiceCallResult deserializeSCRNetworkResponse(ServiceCallRequest serviceCallRequest,
        final NetworkResponse networkResponse)
        throws SerializationException {
        // if a low-level network error occurred, the message's payload is either null, or a serialized String with additional error
        // information; in both cases, convert it into a synthetic ServiceCallResult that will cause a ROE to be thrown

        // create a synthetic CommunicationException for errors that were not thrown by the
        // remote method or the remote service invoker (e.g. routing errors)
        if (!networkResponse.isSuccess()) {
            return ServiceCallResultFactory.representNetworkErrorAsRemoteOperationException(serviceCallRequest, networkResponse);
        }

        Serializable deserializedContent = networkResponse.getDeserializedContent();
        // TODO find out how this can be reached without the response code being != SUCCESS, which is caught above - misc_ro
        if (deserializedContent == null) {
            String errorMessage =
                StringUtils.format("Received null service call result for RPC to %s; response code is %s",
                    formatServiceRequest(serviceCallRequest), networkResponse.getResultCode());
            return ServiceCallResultFactory.representInternalErrorAtSender(serviceCallRequest, errorMessage);
        }
        if (!(deserializedContent instanceof ServiceCallResult)) {
            return ServiceCallResultFactory.representInternalErrorAtSender(serviceCallRequest,
                "Received a serialized response of unexpected type " + deserializedContent.getClass().getName());
        }

        return (ServiceCallResult) deserializedContent;
    }

    protected static String formatServiceRequest(ServiceCallRequest serviceCallRequest) {
        return StringUtils.format("%s#%s() on %s", serviceCallRequest.getServiceName(),
            serviceCallRequest.getMethodName(), serviceCallRequest.getTargetNodeId());
    }

    private Throwable reconstructMethodException(ServiceCallRequest serviceCallRequest, ServiceCallResult serviceCallResult)
        throws Throwable {
        final String methodExceptionType = serviceCallResult.getMethodExceptionType();
        final String methodExceptionMessage = serviceCallResult.getMethodExceptionMessage();
        final Class<?> exceptionClass;
        try {
            exceptionClass = Class.forName(methodExceptionType);
        } catch (ClassNotFoundException e) {
            log.error("Received an unknown Exception class, generating a RemoteOperationException instead: " + methodExceptionType);
            throw new RemoteOperationException(StringUtils.format(
                "The destination instance sent an error type that is not known on the local instance: %s: %s", methodExceptionType,
                methodExceptionMessage));
        }
        // sanity check to make sure the remote node cannot use this to instantiate arbitrary classes - misc_ro
        if (!Throwable.class.isAssignableFrom(exceptionClass)) {
            // TODO add more information
            log.error("The destination node sent a non-Throwable type as the alleged method exception: " + methodExceptionType);
            return new RemoteOperationException(StringUtils.format(
                "The destination instance sent an invalid exception type (unusual behavior): %s: %s", methodExceptionType,
                methodExceptionMessage));
        }

        final Constructor<?> stringOnlyConstructor;
        try {
            stringOnlyConstructor = exceptionClass.getConstructor(String.class);
            Assertions.isDefined(stringOnlyConstructor, "Unexpected: getConstructor() should never return null");
        } catch (NoSuchMethodException e) {
            log.error(StringUtils.format(
                "The destination instance sent a known exception type, but it has no accessible String-only constructor: %s: %s",
                methodExceptionType, methodExceptionMessage));
            throw new RemoteOperationException(StringUtils.format("%s: %s", methodExceptionType, methodExceptionMessage));
        }

        final Throwable reconstructedException;
        try {
            reconstructedException = (Throwable) stringOnlyConstructor.newInstance(methodExceptionMessage);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.error(
                StringUtils.format("Error reconstructing a method exception (%s: %s)", methodExceptionType, methodExceptionMessage), e);
            throw new RemoteOperationException(StringUtils.format("%s: %s", methodExceptionType, methodExceptionMessage));
        }
        throw reconstructedException;
    }

    /**
     * Sets the {@link MessageRoutingService} implementation to use; called by OSGi-DS and unit tests.
     * 
     * @param newInstance the routing service implementation
     */
    @Reference
    public void bindMessageRoutingService(MessageRoutingService newInstance) {
        this.routingService = newInstance;
    }

    /**
     * Sets the {@link ReliableRPCStreamService} implementation to use; called by OSGi-DS and unit tests.
     * 
     * @param newInstance the service implementation
     */
    @Reference
    public void bindReliableRPCStreamService(ReliableRPCStreamService newInstance) {
        this.reliableRPCStreamService = newInstance;
    }

}
