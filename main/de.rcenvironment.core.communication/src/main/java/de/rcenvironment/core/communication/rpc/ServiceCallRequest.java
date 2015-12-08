/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Representation of a remote service method call request.
 * 
 * @author Robert Mischke
 */
public class ServiceCallRequest implements Serializable {

    private static final long serialVersionUID = -9120629516281659775L;

    private static final String ERROR_PARAMETERS_NULL = "The parameter \"{0}\" must not be null.";

    /**
     * The {@link NodeIdentifier} on which the call should be processed.
     */
    private final NodeIdentifier destination;

    /**
     * The {@link NodeIdentifier} that initiated the call.
     */
    private final NodeIdentifier sender;

    /**
     * The FQN of the service interface to call.
     */
    private final String serviceName;

    /**
     * The name of the service method to call.
     */
    private final String methodName;

    /**
     * The list of method parameters.
     */
    private final List<? extends Serializable> parameters;

    /**
     * @param destination the destination's {@link NodeIdentifier}
     * @param sender the sender's {@link NodeIdentifier}
     * @param serviceName the FQN of the remote service interface to call
     * @param methodName the name of the remote method to call
     * @param parameters the method parameters
     */
    public ServiceCallRequest(NodeIdentifier destination, NodeIdentifier sender,
        String serviceName, String methodName, List<? extends Serializable> parameters) {

        Assertions.isDefined(destination, MessageFormat.format(ERROR_PARAMETERS_NULL, "destination"));
        Assertions.isDefined(sender, MessageFormat.format(ERROR_PARAMETERS_NULL, "sender"));
        Assertions.isDefined(serviceName, MessageFormat.format(ERROR_PARAMETERS_NULL, "serviceName"));
        Assertions.isDefined(methodName, MessageFormat.format(ERROR_PARAMETERS_NULL, "methodName"));

        this.destination = destination;
        this.sender = sender;
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.parameters = parameters;
    }

    public NodeIdentifier getDestination() {
        return destination;
    }

    public NodeIdentifier getSender() {
        return sender;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<? extends Serializable> getParameterList() {
        return parameters;
    }

}
