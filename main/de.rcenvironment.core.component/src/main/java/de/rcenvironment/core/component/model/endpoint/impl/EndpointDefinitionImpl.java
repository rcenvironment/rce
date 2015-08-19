/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.model.endpoint.api.InitialDynamicEndpointDefinition;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Provides information about a single endpoint.
 * 
 * @author Doreen Seider
 */
public class EndpointDefinitionImpl implements Serializable, EndpointDefinition {

    private static final String KEY_DATATYPE = "datatype";
    
    private static final String READ_ONLY_NAME = "readOnlyName";

    private static final String KEY_READ_ONLY = "readOnly";

    private static final String KEY_DATATYPES = "dataTypes";

    private static final String KEY_DEFAULT_DATATYPE = "defaultDataType";
    
    private static final String KEY_INPUT_HANDLING_OPTIONS = "inputHandlingOptions";

    private static final String KEY_DEFAULT_INPUT_HANDLING = "defaultInputHandling";
    
    private static final String KEY_EXECUTION_CONSTRAINT_OPTIONS = "inputExecutionConstraintOptions";

    private static final String KEY_DEFAULT_EXECUTION_CONSTRAINT = "defaultInputExecutionConstraint";
    
    private static final String KEY_METADATA = "metaData";
    
    private static final String KEY_INITIAL_ENDPOINTS = "initialEndpoints";

    private static final long serialVersionUID = -3853446362359127472L;

    private Map<String, Object> rawEndpointDefinition;
    
    private Map<String, Object> definition;
    
    private Map<String, Object> rawEndpointDefinitionExtension = new HashMap<>();

    private Map<String, Object> endpointDefinitionExtension;

    private EndpointType endpointType;

    private List<DataType> dataTypes;
    
    private List<InputDatumHandling> inputDatumHandlings;
    
    private List<InputExecutionContraint> inputExecutionContraints;

    private EndpointMetaDataDefinitionImpl metaDataDefinition;
    
    private List<InitialDynamicEndpointDefinitionImpl> initialEndpointDefinitions;

    @JsonIgnore
    @Override
    public String getName() {
        return (String) definition.get(EndpointDefinitionConstants.KEY_NAME);
    }

    @JsonIgnore
    @Override
    public String getIdentifier() {
        return (String) definition.get(EndpointDefinitionConstants.KEY_IDENTIFIER);
    }

    @JsonIgnore
    @Override
    public String getGroupName() {
        return (String) definition.get(EndpointDefinitionConstants.KEY_GROUP);
    }
    
    @JsonIgnore
    @Override
    public boolean isStatic() {
        return getName() != null;
    }

    @JsonIgnore
    @Override
    public boolean isReadOnly() {
        return definition.containsKey(KEY_READ_ONLY) && Boolean.parseBoolean((String) definition.get(KEY_READ_ONLY));
    }
    
    @JsonIgnore
    @Override
    public boolean isNameReadOnly() {
        return isStatic() || definition.containsKey(READ_ONLY_NAME) && Boolean.parseBoolean((String) definition.get(READ_ONLY_NAME));
    }

    @JsonIgnore
    @Override
    public List<DataType> getPossibleDataTypes() {
        return Collections.unmodifiableList(dataTypes);
    }

    @JsonIgnore
    @Override
    public DataType getDefaultDataType() {
        return DataType.valueOf((String) definition.get(KEY_DEFAULT_DATATYPE));
    }
    
    @JsonIgnore
    @Override
    public List<InputDatumHandling> getInputDatumOptions() {
        return Collections.unmodifiableList(inputDatumHandlings);
    }

    @JsonIgnore
    @Override
    public InputDatumHandling getDefaultInputDatumHandling() {
        if (definition.containsKey(KEY_DEFAULT_INPUT_HANDLING)) {
            return InputDatumHandling.valueOf((String) definition.get(KEY_DEFAULT_INPUT_HANDLING));
        } else {
            return getInputDatumOptions().get(0);
        }
    }

    @JsonIgnore
    @Override
    public List<InputExecutionContraint> getInputExecutionConstraintOptions() {
        return Collections.unmodifiableList(inputExecutionContraints);
    }

    @JsonIgnore
    @Override
    public InputExecutionContraint getDefaultInputExecutionConstraint() {
        if (definition.containsKey(KEY_DEFAULT_EXECUTION_CONSTRAINT)) {
            return InputExecutionContraint.valueOf((String) definition.get(KEY_DEFAULT_EXECUTION_CONSTRAINT));
        } else {
            return getInputExecutionConstraintOptions().get(0);
        }
    }
    
    @JsonIgnore
    @Override
    public EndpointMetaDataDefinition getMetaDataDefinition() {
        return metaDataDefinition;
    }
    
    @Override
    public EndpointType getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(EndpointType type) {
        this.endpointType = type;
    }
    
    public Map<String, Object> getRawEndpointDefinition() {
        return rawEndpointDefinition;
    }
    
    public Map<String, Object> getRawEndpointDefinitionExtension() {
        return rawEndpointDefinitionExtension;
    }
    
    /**
     * Initializes fields with raw endpoint information.
     * 
     * @param rawEndpointDefinition raw endpoint information
     */
    @SuppressWarnings("unchecked")
    public void setRawEndpointDefinition(Map<String, Object> rawEndpointDefinition) {
        this.rawEndpointDefinition = rawEndpointDefinition;
        this.definition = new HashMap<String, Object>(rawEndpointDefinition);
        
        this.dataTypes = new ArrayList<DataType>();
        for (String dataType : (List<String>) definition.get(KEY_DATATYPES)) {
            dataTypes.add(DataType.valueOf(dataType));
        }
        Collections.sort(dataTypes);
        
        this.inputDatumHandlings = new ArrayList<InputDatumHandling>();
        if (definition.containsKey(KEY_INPUT_HANDLING_OPTIONS)) {
            for (String inputDatumHandling : (List<String>) definition.get(KEY_INPUT_HANDLING_OPTIONS)) {
                inputDatumHandlings.add(InputDatumHandling.valueOf(inputDatumHandling));
            }
        } else {
            inputDatumHandlings.add(InputDatumHandling.Single);
        }
        
        this.inputExecutionContraints = new ArrayList<InputExecutionContraint>();
        if (definition.containsKey(KEY_EXECUTION_CONSTRAINT_OPTIONS)) {
            for (String inputExecutionContraint : (List<String>) definition.get(KEY_EXECUTION_CONSTRAINT_OPTIONS)) {
                inputExecutionContraints.add(InputExecutionContraint.valueOf(inputExecutionContraint));
            }
        } else {
            inputExecutionContraints.add(InputExecutionContraint.Required);
        }
        
        Map<String, Map<String, Object>> metaData = (Map<String, Map<String, Object>>) definition.get(KEY_METADATA);
        if (metaData == null) {
            metaData = new HashMap<String, Map<String, Object>>();
        }
        this.metaDataDefinition = new EndpointMetaDataDefinitionImpl();
        ((EndpointMetaDataDefinitionImpl) metaDataDefinition).setRawMetaData(metaData);

        definition.remove(KEY_METADATA);

        // sanity check
        DataType.valueOf((String) definition.get(KEY_DEFAULT_DATATYPE));
        
        initialEndpointDefinitions = new ArrayList<InitialDynamicEndpointDefinitionImpl>();
        List<Map<String, Object>> rawInitialEndpoints = (List<Map<String, Object>>) definition.get(KEY_INITIAL_ENDPOINTS);
        if (rawInitialEndpoints != null) {
            for (Map<String, Object> defaultEndpoint : rawInitialEndpoints) {
                DataType dataType = getDefaultDataType();
                if (defaultEndpoint.containsKey(KEY_DATATYPE)) {
                    dataType = DataType.valueOf((String) defaultEndpoint.get(KEY_DATATYPE));
                }
                initialEndpointDefinitions.add(new InitialDynamicEndpointDefinitionImpl((String) defaultEndpoint.get(
                    EndpointDefinitionConstants.KEY_NAME), dataType));
            }            
        }
    }

    /**
     * Initializes fields with raw endpoint information.
     * 
     * @param rawEndpointDefinitionExtension raw extended endpoint information
     */
    @SuppressWarnings("unchecked")
    public void setRawEndpointDefinitionExtension(Map<String, Object> rawEndpointDefinitionExtension) {
        this.rawEndpointDefinitionExtension = rawEndpointDefinitionExtension;
        this.endpointDefinitionExtension = new HashMap<String, Object>(rawEndpointDefinitionExtension);
        Map<String, Map<String, Object>> metaData = (Map<String, Map<String, Object>>) endpointDefinitionExtension.get(KEY_METADATA);
        if (metaData == null) {
            metaData = new HashMap<String, Map<String, Object>>();
        }
        metaDataDefinition.setRawMetaDataExtensions(metaData);
    }
    
    @Override
    public List<InitialDynamicEndpointDefinition> getInitialDynamicEndpointDefinitions() {
        return new ArrayList<InitialDynamicEndpointDefinition>(initialEndpointDefinitions);
    }
    
    public void setInitialDynamicEndpointDefinitions(List<InitialDynamicEndpointDefinitionImpl> definitions) {
        initialEndpointDefinitions = definitions;
    }

}
