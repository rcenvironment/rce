/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;

/**
 * Describes the recipient for an {@link EndpointDatum}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
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
     * @return the {@link LogicalNodeId} of the target node
     */
    LogicalNodeId getDestinationNodeId();

    /**
     * @return the {@link NetworkDestination} to send {@link EndpointDatum}s to; it may be a node id or an abstract communication stream
     */
    NetworkDestination getNetworkDestination();
}
