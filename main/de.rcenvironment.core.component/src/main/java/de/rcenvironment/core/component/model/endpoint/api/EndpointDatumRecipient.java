/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.api;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;

/**
 * Describes the recipient for an {@link EndpointDatum}.
 * 
 * @author Doreen Seider
 */
public interface EndpointDatumRecipient {

    /**
     * @return name of the target input
     */
    String getInputName();
    
    /**
     * @return component execution identifier of target component
     */
    String getInputsComponentExecutionIdentifier();
    
    /**
     * @return instance name of target component (used for logging purposes)
     */
    String getInputsComponentInstanceName();
    
    /**
     * @return {@link InstanceNodeSessionId} of the target node
     */
    LogicalNodeId getInputsNodeId();
    
}
