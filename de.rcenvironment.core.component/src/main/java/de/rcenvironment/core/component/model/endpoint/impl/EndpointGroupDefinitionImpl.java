/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.impl;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDefinition;

/**
 * Implementation of {@link EndpointGroupDefinition}.
 * 
 * @author Doreen Seider
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointGroupDefinitionImpl implements Serializable, EndpointGroupDefinition {

    private static final long serialVersionUID = -6777818685549261071L;

    private static final String KEY_TYPE = "type";
    
    protected Map<String, Object> rawEndpointGroupDefinition;
    
    @JsonIgnore
    @Override
    public String getIdentifier() {
        return (String) rawEndpointGroupDefinition.get(EndpointDefinitionConstants.KEY_IDENTIFIER);
    }
    
    @JsonIgnore
    @Override
    public String getName() {
        return (String) rawEndpointGroupDefinition.get(EndpointDefinitionConstants.KEY_NAME);
    }

    @JsonIgnore
    @Override
    public LogicOperation getLogicOperation() {
        return LogicOperation.valueOf((String) rawEndpointGroupDefinition.get(KEY_TYPE));
    }

    @JsonIgnore
    @Override
    public String getParentGroupName() {
        return (String) rawEndpointGroupDefinition.get(EndpointDefinitionConstants.KEY_GROUP);
    }
    
    public void setRawEndpointGroupDefinition(Map<String, Object> rawEndpointDefinition) {
        this.rawEndpointGroupDefinition = rawEndpointDefinition;
    }
    
    public Map<String, Object> getRawEndpointGroupDefinition() {
        return rawEndpointGroupDefinition;
    }
    
}
