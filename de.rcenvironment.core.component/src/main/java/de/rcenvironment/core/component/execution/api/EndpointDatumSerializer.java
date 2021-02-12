/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;

/**
 * (De-)Serializes {@link EndpointDatum} objects. Used when sent between components.
 * 
 * @author Doreen Seider
 *
 */
public interface EndpointDatumSerializer {

    /**
     * Serializes an {@link EndpointDatum}.
     * @param endpoint {@link EndpointDatum} to serialize
     * @return serialized {@link EndpointDatum}
     */
    String serializeEndpointDatum(EndpointDatum endpoint);
    
    /**
     * Deserializes an {@link EndpointDatum}.
     * 
     * @param serializedEndpoint {@link EndpointDatum} to deserialize
     * @return deserialized {@link EndpointDatum} object
     */
    EndpointDatum deserializeEndpointDatum(String serializedEndpoint);
}
