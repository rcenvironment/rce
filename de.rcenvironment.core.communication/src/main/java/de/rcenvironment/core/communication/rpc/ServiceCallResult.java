/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import java.io.Serializable;

import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Represents the result of a remote service method call. May either contain the method's return value (on a successful call), a declared
 * exception thrown by the method, or a string containing an error message.
 * 
 * @author Robert Mischke
 */
public class ServiceCallResult implements Serializable {

    private static final long serialVersionUID = -7511179849159143497L;

    /**
     * The return value after a successful invocation; may be null if an error occurred or for methods that return "void".
     */
    private Serializable methodReturnValue;

    /**
     * The type of a potential checked exception that was thrown by the method; may be null.
     */
    private String methodExceptionType;

    /**
     * The message of a potential checked exception that was thrown by the method; may be null.
     */
    private String methodExceptionMessage;

    /**
     * A message text representing a non-method error or exception; these are represented on the sender and destination side as
     * {@link RemoteOperationException}s.
     */
    private String errorMessage;

    /**
     * Constructor that takes a return value.
     * 
     * @param returnValue Return value to set.
     */
    public ServiceCallResult(Serializable returnValue, String methodExceptionType, String methodExceptionMessage, String errorMessage) {
        this.methodReturnValue = returnValue;
        // note: the exception's "cause" part is intentionally discarded here
        this.methodExceptionType = methodExceptionType;
        this.methodExceptionMessage = methodExceptionMessage;
        this.errorMessage = errorMessage;
    }

    public Serializable getReturnValue() {
        return methodReturnValue;
    }

    public boolean isSuccess() {
        return methodExceptionType == null && errorMessage == null;
    }

    public boolean isMethodException() {
        return methodExceptionType != null;
    }

    public boolean isRemoteOperationException() {
        return errorMessage != null;
    }

    public String getMethodExceptionType() {
        return methodExceptionType;
    }

    public String getMethodExceptionMessage() {
        return methodExceptionMessage;
    }

    public String getRemoteOperationExceptionMessage() {
        return errorMessage;
    }

}
