/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.api;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Describes the target input for a {@link EndpointDatum}.
 * 
 * @author Doreen Seider
 */
public interface EndpointDatumRecipient extends Serializable {

    /**
     * @return name of the target input
     */
    String getInputName();
    
    /**
     * @return inputIdentifier target input identifier (is the name of the input)
     */
    String getInputComponentExecutionIdentifier();
    
    /**
     * @return {@link NodeIdentifier} of the target node
     */
    NodeIdentifier getInputsNodeId();
    
}
