/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.api;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Data which is sent from an ouput to an input.
 * 
 * @author Doreen Seider
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
     * @return node the associated workflow (controller)
     */
    NodeIdentifier getWorkflowNodeId();
    
    /**
     * @return node the associated workflow (controller)
     */
    Long getDataManagementId();
    
    /**
     * @return recipient information of this {@link EndpointDatum}.
     */
    EndpointDatumRecipient getEndpointDatumRecipient();
}
