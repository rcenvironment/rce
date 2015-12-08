/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.utils.common.rpc.RemotableService;


/**
 * Dispatches {@link EndpointDatum}s. There is one {@link EndpointDatumDispatcher} per node.
 * 
 * @author Doreen Seider
 */
@RemotableService
public interface EndpointDatumDispatcher {

    /**
     * Dispatches {@link EndpointDatum}s asynchronously but ordered.
     * 
     * @param endpointDatum {@link EndpointDatum} to dispatch
     */
    void dispatchEndpointDatum(EndpointDatum endpointDatum);
}
