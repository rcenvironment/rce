/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import java.util.Set;

import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Provides all {@link EndpointDefinition} of one {@link EndpointType} of a component.
 *
 * @author Doreen Seider
 */
public interface EndpointDefinitionsProvider {

    /**
     * @return all static {@link EndpointDefinition}s
     */
    Set<EndpointDefinition> getStaticEndpointDefinitions();

    /**
     * @param name name of static {@link EndpointDefinition} to get
     * @return {@link EndpointDefinition} with given name
     */
    EndpointDefinition getStaticEndpointDefinition(String name);

    /**
     * @return all dynamic {@link EndpointDefinition}s
     */
    Set<EndpointDefinition> getDynamicEndpointDefinitions();

    /**
     * @param id identifier of dynamic {@link EndpointDefinition} to get
     * @return {@link EndpointDefinition} with given id
     */
    EndpointDefinition getDynamicEndpointDefinition(String id);
    
    /**
     * @return set of dynamic {@link EndpointGroupDefinition}s
     */
    Set<EndpointGroupDefinition> getDynamicEndpointGroupDefinitions();
    
    /**
     * @return set of static {@link EndpointGroupDefinition}s
     */
    Set<EndpointGroupDefinition> getStaticEndpointGroupDefinitions();

    /**
     * @param name name of the dynamic endpoint group
     * @return {@link EndpointGroupDefinition} with given group name or <code>null</code> if there is none
     */
    EndpointGroupDefinition getDynamicEndpointGroupDefinition(String name);

}
