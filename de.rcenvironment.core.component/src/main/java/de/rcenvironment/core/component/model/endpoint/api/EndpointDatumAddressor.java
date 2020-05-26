/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.api;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;

/**
 * Describes the sender of an {@link EndpointDatum}.
 * 
 * @author Doreen Seider
 */
public interface EndpointDatumAddressor {

    /**
     * @return component execution identifier of source component
     */
    String getOutputsComponentExecutionIdentifier();
    
    /**
     * @return {@link InstanceNodeSessionId} of the source node
     */
    LogicalNodeId getOutputsNodeId();
    
}
