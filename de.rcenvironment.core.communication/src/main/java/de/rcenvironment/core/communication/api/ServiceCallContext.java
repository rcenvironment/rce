/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.communication.common.LogicalNodeSessionId;

/**
 * The context of a remote service call, as seen by the receiving side.
 * 
 * @author Robert Mischke
 */
public interface ServiceCallContext {

    // boolean isInstanceInternalCall();

    /**
     * @return the {@link LogicalNodeSessionId} used while invoking the local service method
     */
    LogicalNodeSessionId getCallingNode();

    /**
     * @return the {@link LogicalNodeSessionId} under which the service method was called
     */
    LogicalNodeSessionId getReceivingNode();

    /**
     * @return the name of the invoked service
     */
    String getServiceName();

    /**
     * @return the name of the invoked method
     */
    String getMethodName();
}
