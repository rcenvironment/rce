/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDefinition;

/**
 * Provides endpoint definitions of a component.
 * 
 * @author Doreen Seider
 */
public class EndpointDefinitionsProviderImpl implements Serializable, EndpointDefinitionsProvider {

    private static final long serialVersionUID = -386695878188756473L;

    private Set<EndpointDefinitionImpl> endpointDefinitions = new HashSet<>();

    private Map<String, EndpointDefinition> staticEndpointDefinitions = new HashMap<>();

    private Map<String, EndpointDefinition> dynamicEndpointDefinitions = new HashMap<>();
    
    private Set<EndpointGroupDefinitionImpl> endpointGroupDefinitions = new HashSet<>();
    
    private Map<String, EndpointGroupDefinition> staticEndpointGroupDefinitions = new HashMap<>();
    
    private Map<String, EndpointGroupDefinition> dynamicEndpointGroupDefinitions = new HashMap<>();
    
    private Map<String, EndpointGroupDefinition> endpointGroups = new HashMap<>();

    @JsonIgnore
    @Override
    public Set<EndpointDefinition> getStaticEndpointDefinitions() {
        return new HashSet<EndpointDefinition>(staticEndpointDefinitions.values());
    }

    @JsonIgnore
    @Override
    public EndpointDefinition getStaticEndpointDefinition(String name) {
        return staticEndpointDefinitions.get(name);
    }

    @JsonIgnore
    @Override
    public Set<EndpointDefinition> getDynamicEndpointDefinitions() {
        return new HashSet<EndpointDefinition>(dynamicEndpointDefinitions.values());
    }

    @JsonIgnore
    @Override
    public EndpointDefinition getDynamicEndpointDefinition(String id) {
        return dynamicEndpointDefinitions.get(id);
    }
    
    @JsonIgnore
    @Override
    public Set<EndpointGroupDefinition> getDynamicEndpointGroupDefinitions() {
        return new HashSet<EndpointGroupDefinition>(dynamicEndpointGroupDefinitions.values());
    }

    @JsonIgnore
    @Override
    public Set<EndpointGroupDefinition> getStaticEndpointGroupDefinitions() {
        return new HashSet<EndpointGroupDefinition>(staticEndpointGroupDefinitions.values());
    }

    @JsonIgnore
    @Override
    public EndpointGroupDefinition getDynamicEndpointGroupDefinition(String id) {
        return dynamicEndpointGroupDefinitions.get(id);
    }
    
    public Set<EndpointGroupDefinition> getEndpointGroupDefinitions() {
        return new HashSet<EndpointGroupDefinition>(endpointGroupDefinitions);
    }

    public Set<EndpointDefinition> getEndpointDefinitions() {
        return new HashSet<EndpointDefinition>(endpointDefinitions);
    }
    
    /**
     * Assumes that at most one {@link EndpointGroupDefinition} with name "*" is given. If there is more than
     * one given the very last one is set as the dynamic one.
     * 
     * @param endpointGroupDefinitionImpls all {@link EndpointGroupDefinition}s (static and at most one
     *        dynamic)
     */
    public void setEndpointGroupDefinitions(Set<EndpointGroupDefinitionImpl> endpointGroupDefinitionImpls) {
        endpointGroupDefinitions = endpointGroupDefinitionImpls;
        for (EndpointGroupDefinition endpointDefinition : endpointGroupDefinitionImpls) {
            if (endpointDefinition.getIdentifier() != null) {
                dynamicEndpointGroupDefinitions.put(endpointDefinition.getIdentifier(), endpointDefinition);
            } else {
                staticEndpointGroupDefinitions.put(endpointDefinition.getName(), endpointDefinition);
            }
        }
    }

    /**
     * Assumes that at most one endpoint description with name "*" is given. If there is more than
     * one given the very last one is set as the dynamic one.
     * 
     * @param endpointDefinitionImpls all {@link EndpointDefinition}s (static and at most one
     *        dynamic)
     */
    public void setEndpointDefinitions(Set<EndpointDefinitionImpl> endpointDefinitionImpls) {
        endpointDefinitions = endpointDefinitionImpls;
        for (EndpointDefinition endpointInterface : endpointDefinitionImpls) {
            if (endpointInterface.getIdentifier() != null) {
                dynamicEndpointDefinitions.put(endpointInterface.getIdentifier(), endpointInterface);
            } else {
                staticEndpointDefinitions.put(endpointInterface.getName(), endpointInterface);
            }
        }
    }
    
    public Set<EndpointGroupDefinition> getEndpointGroups() {
        return new HashSet<>(endpointGroups.values());
    }
    
    /**
     * @param endpointGroups set of {@link EndpointGroupDefinitionImpl} to set
     */
    public void setEndpointGroups(Set<EndpointGroupDefinitionImpl> endpointGroups) {
        for (EndpointGroupDefinitionImpl endpointGroup : endpointGroups) {
            this.endpointGroups.put(endpointGroup.getIdentifier(), endpointGroup);
        }
    }

}
