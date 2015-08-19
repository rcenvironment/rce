/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointMetaDataDefinitionImpl;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Creates component endpoint model objects from raw JSON data.
 * 
 * @author Doreen Seider
 */
public final class ComponentEndpointModelFactory {

    private ComponentEndpointModelFactory() {}

    /**
     * 
     * @param rawDefinition raw definition information
     * 
     * @param endpointType {@link EndpointType#INPUT} or {@link EndpointType#OUTPUT}
     * 
     * @return {@link EndpointDefinition} object
     */

    public static EndpointDefinition createEndpointDefinition(Map<String, Object> rawDefinition, EndpointType endpointType) {
        EndpointDefinitionImpl endpointDefinition = new EndpointDefinitionImpl();
        endpointDefinition.setRawEndpointDefinition(rawDefinition);
        endpointDefinition.setEndpointType(endpointType);
        return endpointDefinition;

    }

    /**
     * @param rawMetaData raw meta data information
     * @return {@link EndpointMetaDataDefinition} object
     */
    public static EndpointMetaDataDefinition createEndpointMetaDataDefinition(Map<String, Map<String, Object>> rawMetaData) {
        EndpointMetaDataDefinitionImpl endpointMetaDataDefinition = new EndpointMetaDataDefinitionImpl();
        endpointMetaDataDefinition.setRawMetaData(rawMetaData);
        return endpointMetaDataDefinition;
    }

    /**
     * @param endpointDefinitions endpoint definitions information
     * @return {@link EndpointDefinitionsProvider} object
     */
    public static EndpointDefinitionsProvider createEndpointDefinitionsProvider(Set<EndpointDefinition> endpointDefinitions) {
        EndpointDefinitionsProviderImpl endpointDefinitionsProvider = new EndpointDefinitionsProviderImpl();
        Set<EndpointDefinitionImpl> endpointDefinitionImpls = new HashSet<EndpointDefinitionImpl>();
        for (EndpointDefinition definition : endpointDefinitions) {
            endpointDefinitionImpls.add((EndpointDefinitionImpl) definition);
        }
        endpointDefinitionsProvider.setEndpointDefinitions(endpointDefinitionImpls);
        return endpointDefinitionsProvider;

    }

}
