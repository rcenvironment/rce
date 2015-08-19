/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;


/**
 * Process {@link EndpointDatum}s which were sent by an {@link EndpointDatumSender}. There is one {@link EndpointDatumProcessor} per node.
 * 
 * @author Doreen Seider
 */
public interface EndpointDatumProcessor {

    /**
     * Called if a new {@link EndpointDatum} was received.
     * 
     * @param serializedEndpointDatum serialized {@link EndpointDatum}
     */
    void onEndpointDatumReceived(String serializedEndpointDatum);
}
