/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;

/**
 * Sends {@link EndpointDatum}s to target node's {@link EndpointDatumProcessor}.
 * 
 * @author Doreen Seider
 */
public interface EndpointDatumSender {

    /**
     * Sends {@link EndpointDatum}s to target node's specified in within given {@link EndpointDatum}.
     * 
     * @param endpointDatum {@link EndpointDatum} to send
     */
    void sendEndpointDatumOrderedAsync(EndpointDatum endpointDatum);
}
