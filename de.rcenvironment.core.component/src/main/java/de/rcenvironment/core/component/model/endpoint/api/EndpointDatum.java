/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Data which is sent from an ouput to an input.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface EndpointDatum extends EndpointDatumAddressor, EndpointDatumRecipient {

    /**
     * @return {@link TypedDatum} of the {@link EndpointDatum}. It is the payload.
     */
    TypedDatum getValue();

    /**
     * @return execution identifier of the associated workflow
     */
    String getWorkflowExecutionIdentifier();

    /**
     * @return the location of the associated workflow controller
     */
    LogicalNodeId getWorkflowControllerLocation();

    /**
     * @return TODO describe (had incorrect description)
     */
    Long getDataManagementId();

    /**
     * @return recipient information of this {@link EndpointDatum}.
     */
    EndpointDatumRecipient getEndpointDatumRecipient();
}
