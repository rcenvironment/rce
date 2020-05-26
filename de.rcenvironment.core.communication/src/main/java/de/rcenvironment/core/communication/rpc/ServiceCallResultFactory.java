/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.internal.PayloadTestFuzzer;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Represents all possible outcomes of a remote service call (on either side) into {@link ServiceCallResult} instances.
 * 
 * @author Robert Mischke
 */
public final class ServiceCallResultFactory {

    private static final Log sharedLog = LogFactory.getLog(ServiceCallResultFactory.class);

    private ServiceCallResultFactory() {}

    /**
     * Represents a low-level network error (e.g. a timeout) on the service call level. For the caller, this is represented as a
     * {@link RemoteOperationException} with a message describing the error.
     * 
     * Note that the network error is not logged by this method, as this is expected to have happened already. (TODO 7.0.0 review)
     * 
     * @param serviceCallRequest the related request
     * @param networkResponse the {@link NetworkResponse} representing the error
     * @return the generated ServiceCallResult
     */
    public static ServiceCallResult representNetworkErrorAsRemoteOperationException(ServiceCallRequest serviceCallRequest,
        NetworkResponse networkResponse) {

        final String resultCodeMessage = networkResponse.getResultCode().toString();
        String additionalErrorInformation = null;
        try {
            Serializable deserializedContent = networkResponse.getDeserializedContent();
            if (deserializedContent != null) {
                if (deserializedContent instanceof String) {
                    additionalErrorInformation = (String) deserializedContent;
                    additionalErrorInformation = parseEncodedErrorInformation(additionalErrorInformation);
                } else {
                    additionalErrorInformation =
                        "Internal error: deserialized extended error information, but it is not a string: "
                            + deserializedContent.toString();
                }
            }
        } catch (SerializationException e) {
            additionalErrorInformation = "Internal error: failed to deserialize the extended error message: " + e.getMessage();
        }
        // TODO add local metadata again? decide if useful
        // LogUtils.logErrorAndAssignUniqueMarker(sharedLog,
        // StringUtils.format("Network error while handling service request for '%s#%s' on '%s': %s (message trace: %s)",
        // serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), serviceCallRequest.getDestination(),
        // resultCodeMessage, networkResponse.accessMetaData().getTrace()));

        final String userMessage;
        if (additionalErrorInformation != null) {
            userMessage = StringUtils.format("%s: %s", resultCodeMessage, additionalErrorInformation);
        } else {
            userMessage = resultCodeMessage;
        }
        // throw a RemoteOperationException from the called service's proxy
        return new ServiceCallResult(null, null, null, userMessage);
    }

    private static String parseEncodedErrorInformation(String additionalErrorInformation) {
        String[] parts = StringUtils.splitAndUnescape(additionalErrorInformation);
        if (parts.length != 2) {
            LogFactory.getLog(ServiceCallResultFactory.class).error(
                "Received unexpected content in additional error information: " + additionalErrorInformation);
            return additionalErrorInformation;
        }
        String errorId = parts[0];
        String nodeIdString = parts[1];
        String reportingNodeRepresentation;
        if (!StringUtils.isNullorEmpty(nodeIdString)) {
            try {
                reportingNodeRepresentation = NodeIdentifierUtils.parseInstanceNodeSessionIdString(nodeIdString).toString();
            } catch (IdentifierException e) {
                // emergency fallback - should not happen
                reportingNodeRepresentation = StringUtils.format("[Failed to parse received node id '%s': %s]", nodeIdString, e.toString());
            }
        } else {
            reportingNodeRepresentation = null;
        }
        if (reportingNodeRepresentation != null) {
            if (StringUtils.isNullorEmpty(errorId)) {
                return StringUtils.format("The error was reported by %s", reportingNodeRepresentation);
            } else {
                return StringUtils.format(
                    "The error was reported by %s; technical details were logged there as error '%s'",
                    reportingNodeRepresentation, errorId);
            }
        } else {
            // note: both cases are unusual
            if (StringUtils.isNullorEmpty(errorId)) {
                return "No further information available";
            } else {
                return StringUtils.format("Technical details were logged on the target instance as error '%s'", errorId);
            }
        }
    }

    /**
     * Represents an internal, non-network error on the caller's instance. Once example would be a failed permission check before sending
     * the actual request across the network.
     * 
     * For the caller, this is represented as a {@link RemoteOperationException} with a standard error message. The given message is logged
     * by this method, given a error correlation id, and that id is included in the generated message.
     * 
     * @param serviceCallRequest the related request
     * @param errorMessage an internal description that is logged; it is *not* directly presented to the user
     * @return the generated ServiceCallResult
     */
    public static ServiceCallResult representInternalErrorAtSender(ServiceCallRequest serviceCallRequest, String errorMessage) {
        return representInternalErrorAtSender(serviceCallRequest, errorMessage, null);
    }

    /**
     * Represents an internal, non-network error on the caller's instance. Once example would be a failed permission check before sending
     * the actual request across the network.
     * 
     * For the caller, this is represented as a {@link RemoteOperationException} with a standard error message. The given message and
     * exception cause are logged by this method, given a error correlation id, and that id is included in the generated message.
     * 
     * @param serviceCallRequest the related request
     * @param errorMessage an internal description that is logged; it is *not* directly presented to the user
     * @param throwable a cause parameter that is included in the internal logging; it is *not* directly presented to the user
     * @return the generated ServiceCallResult
     */
    public static ServiceCallResult representInternalErrorAtSender(ServiceCallRequest serviceCallRequest,
        final String errorMessage, final Throwable throwable) {

        @SuppressWarnings("unused")// suppress Eclipse warning when the ENABLED constant is false - misc_ro
        final boolean suppressStacktrace = PayloadTestFuzzer.ENABLED && throwable.getClass() == SerializationException.class;

        final String errorId;
        if (suppressStacktrace) {
            errorId = LogUtils.logErrorAndAssignUniqueMarker(sharedLog,
                StringUtils.format("Local error at sender while performing service request to '%s#%s' on target node '%s': %s: %s",
                    serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), serviceCallRequest.getTargetNodeId(),
                    errorMessage, throwable.toString()));

        } else {
            errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(sharedLog,
                StringUtils.format("Local error at sender while performing service request to '%s#%s' on target node '%s': %s",
                    serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), serviceCallRequest.getTargetNodeId(),
                    errorMessage), throwable);
        }

        final String userMessage = StringUtils.format("There was a local error while performing this operation; "
            + "you can find more information by looking for the marker '%s' in your instance's log files", errorId);
        return new ServiceCallResult(null, null, null, userMessage);
    }

    /**
     * Wraps the result of a successful method invocation.
     * 
     * @param returnValue the return value
     * @return the generated ServiceCallResult
     */
    public static ServiceCallResult wrapReturnValue(Serializable returnValue) {
        return new ServiceCallResult(returnValue, null, null, null);
    }

    /**
     * Wraps an exception thrown by the invoked method. Note that only checked (ie, declared) exceptions should be wrapped by this method;
     * unexpected {@link Throwable}s should be wrapped with {@link #representInternalErrorAtHandler(ServiceCallRequest, String, Throwable)}
     * instead.
     * 
     * @param methodException the thrown exception
     * @return the generated ServiceCallResult
     */
    public static ServiceCallResult wrapMethodException(Exception methodException) {
        // FIXME review for 7.0.0: "downgrade" to declared exception types to ensure reconstruction at client?
        String methodExceptionType = methodException.getClass().getName();
        String methodExceptionMessage = methodException.getMessage();
        return new ServiceCallResult(null, methodExceptionType, methodExceptionMessage, null);
    }

    /**
     * Represents a failed permission check on the receiving instance.
     * 
     * The failure is logged on the receiving instance, and a {@link RemoteOperationException} with a generic message is thrown for the
     * caller.
     * 
     * @param serviceCallRequest the related request
     * @param internalInfo internal log information to help with debugging
     * @return the generated ServiceCallResult
     */
    public static ServiceCallResult representInvalidRequestAtHandler(ServiceCallRequest serviceCallRequest, String internalInfo) {
        final String errorId = LogUtils.logErrorAndAssignUniqueMarker(sharedLog,
            StringUtils.format("Refused request for invalid method '%s#%s()' sent by '%s': %s", serviceCallRequest.getServiceName(),
                serviceCallRequest.getMethodName(), serviceCallRequest.getCallerNodeId(), internalInfo));

        final String userMessage = StringUtils.format("Request refused by destination instance (remote error id: %s)", errorId);
        return new ServiceCallResult(null, null, null, userMessage);
    }

    /**
     * Represents an internal, non-network error on the receiving instance. A typical example is a {@link RuntimeException} leaking from the
     * invoked method.
     * 
     * For the caller, this is represented as a {@link RemoteOperationException} with a standard error message. The given message and
     * exception cause are logged on the receiving instance by this method, given a error correlation id, and that id is included in the
     * generated message.
     * 
     * @param serviceCallRequest the related request
     * @param errorMessage an internal description that is logged; it is *not* directly presented to the user
     * @return the generated ServiceCallResult
     */
    public static ServiceCallResult representInternalErrorAtHandler(ServiceCallRequest serviceCallRequest,
        final String errorMessage) {
        return representInternalErrorAtHandler(serviceCallRequest, errorMessage, null);
    }

    /**
     * Represents an internal, non-network error on the receiving instance. A typical example is a {@link RuntimeException} leaking from the
     * invoked method.
     * 
     * For the caller, this is represented as a {@link RemoteOperationException} with a standard error message. The given message is logged
     * on the receiving instance by this method, given a error correlation id, and that id is included in the generated message.
     * 
     * @param serviceCallRequest the related request
     * @param errorMessage an internal description that is logged; it is *not* directly presented to the user
     * @param throwable a cause parameter that is included in the internal logging; it is *not* directly presented to the user
     * @return the generated ServiceCallResult
     */
    public static ServiceCallResult representInternalErrorAtHandler(ServiceCallRequest serviceCallRequest,
        final String errorMessage, final Throwable throwable) {

        final String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(sharedLog,
            StringUtils.format("Error while handling service request to '%s#%s' from '%s': %s",
                serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), serviceCallRequest.getCallerNodeId(),
                errorMessage),
            throwable);

        final String userMessage = StringUtils.format(
            "There was an error performing this remote operation; if you have access to the log files of %s, "
                + "you can find more information by looking for the marker '%s' in its log files",
            serviceCallRequest.getTargetNodeId(), errorId);
        return new ServiceCallResult(null, null, null, userMessage);
    }
}
