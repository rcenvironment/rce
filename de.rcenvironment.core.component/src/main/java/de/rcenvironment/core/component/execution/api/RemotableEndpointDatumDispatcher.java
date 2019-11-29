/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Dispatches {@link EndpointDatum}s. There is one {@link RemotableEndpointDatumDispatcher} per node.
 * 
 * @author Doreen Seider
 */
@RemotableService
// TODO this should be renamed to ...Service for clarity -- misc_ro
public interface RemotableEndpointDatumDispatcher {

    /**
     * Dispatches {@link EndpointDatum}s asynchronously but ordered.
     * 
     * @param serializedEndpointDatum serialized {@link EndpointDatum} to dispatch
     * @throws RemoteOperationException if called from remote and remote method call failed
     */
    void dispatchEndpointDatum(String serializedEndpointDatum) throws RemoteOperationException;
}
