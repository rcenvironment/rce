/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import java.io.Serializable;
import java.util.List;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Representation of a remote service method call request.
 * 
 * @author Robert Mischke
 */
public class ServiceCallRequest implements Serializable {

    private static final long serialVersionUID = 7198189060827978171L;

    private static final String ERROR_PARAMETERS_NULL = "The parameter \"%s\" must not be null.";

    /**
     * The id of the logical node by which the call should be processed. Note that at this time, there is no point of using a
     * {@link LogicalNodeSessionId} here, as the session part is validated by the RPC layer already, and provides no additional information.
     * If instance sessions and logical node sessions are separated in the future, this may be more useful then (but requires adaptations to
     * resolve it).
     */
    private LogicalNodeSessionId target;

    /**
     * The id of the logical node that initiated the call. See {@link #target} for remarks about the id type used.
     */
    private LogicalNodeSessionId caller;

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
     * @param targetNodeId the target node's {@link InstanceNodeSessionId}
     * @param callerNodeId the calling node's {@link InstanceNodeSessionId}
     * @param serviceName the FQN of the remote service interface to call
     * @param methodName the name of the remote method to call
     * @param parameters the method parameters
     */
    public ServiceCallRequest(LogicalNodeSessionId targetNodeId, LogicalNodeSessionId callerNodeId,
        String serviceName, String methodName, List<? extends Serializable> parameters) {

        Assertions.isDefined(targetNodeId, StringUtils.format(ERROR_PARAMETERS_NULL, "destination"));
        Assertions.isDefined(callerNodeId, StringUtils.format(ERROR_PARAMETERS_NULL, "sender"));
        Assertions.isDefined(serviceName, StringUtils.format(ERROR_PARAMETERS_NULL, "serviceName"));
        Assertions.isDefined(methodName, StringUtils.format(ERROR_PARAMETERS_NULL, "methodName"));

        this.target = targetNodeId;
        this.caller = callerNodeId;
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.parameters = parameters;
    }

    /**
     * @return the target node as an identifier object
     */
    public LogicalNodeSessionId getTargetNodeId() {
        return target;
    }

    /**
     * @return the calling node as an identifier object
     */
    public LogicalNodeSessionId getCallerNodeId() {
        return caller;
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
