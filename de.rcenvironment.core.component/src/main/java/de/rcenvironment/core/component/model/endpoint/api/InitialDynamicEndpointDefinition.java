/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.api;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Defines the initial dynamic endpoints.
 * 
 * @author Doreen Seider
 */
public interface InitialDynamicEndpointDefinition extends Serializable {
    
    /**
     * @return name of enpoint
     */
    String getName();
    
    /**
     * @return {@link DataType} of endpoint
     */
    DataType getDataType();
}
