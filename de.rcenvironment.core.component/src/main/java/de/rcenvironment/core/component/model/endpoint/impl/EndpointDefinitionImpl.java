/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.model.endpoint.api.InitialDynamicEndpointDefinition;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Provides information about a single endpoint.
 * 
 * @author Doreen Seider
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointDefinitionImpl extends EndpointGroupDefinitionImpl implements Serializable, EndpointDefinition {

    private static final String KEY_CHARACTER = "character";

    private static final String KEY_DATATYPE = "datatype";

    private static final String READ_ONLY_NAME = "readOnlyName";

    private static final String KEY_READ_ONLY = "readOnly";

    private static final String KEY_INITIAL_ENDPOINTS = "initialEndpoints";

    private static final long serialVersionUID = -3853446362359127472L;

    private Map<String, Object> rawEndpointDefinition;

    private Map<String, Object> rawEndpointDefinitionExtension = new HashMap<>();

    private EndpointType endpointType;

    @JsonIgnore
    private Map<String, Object> definition;

    @JsonIgnore
    private Map<String, Object> endpointDefinitionExtension;

    @JsonIgnore
    private List<DataType> dataTypes;

    @JsonIgnore
    private List<InputDatumHandling> inputDatumHandlings;

    @JsonIgnore
    private List<InputExecutionContraint> inputExecutionContraints;

    @JsonIgnore
    private EndpointMetaDataDefinitionImpl metaDataDefinition;

    @JsonIgnore
    private List<InitialDynamicEndpointDefinitionImpl> initialEndpointDefinitions;

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
        return DataType.valueOf((String) definition.get(EndpointDefinitionConstants.KEY_DEFAULT_DATATYPE));
    }

    @JsonIgnore
    @Override
    public List<InputDatumHandling> getInputDatumOptions() {
        return Collections.unmodifiableList(inputDatumHandlings);
    }

    @JsonIgnore
    @Override
    public InputDatumHandling getDefaultInputDatumHandling() {
        if (definition.containsKey(EndpointDefinitionConstants.KEY_DEFAULT_INPUT_HANDLING)) {
            return InputDatumHandling.valueOf((String) definition.get(EndpointDefinitionConstants.KEY_DEFAULT_INPUT_HANDLING));
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
        if (definition.containsKey(EndpointDefinitionConstants.KEY_DEFAULT_EXECUTION_CONSTRAINT)) {
            return InputExecutionContraint.valueOf((String) definition.get(EndpointDefinitionConstants.KEY_DEFAULT_EXECUTION_CONSTRAINT));
        } else {
            return getInputExecutionConstraintOptions().get(0);
        }
    }

    @JsonIgnore
    @Override
    public EndpointMetaDataDefinition getMetaDataDefinition() {
        return metaDataDefinition;
    }

    @JsonIgnore
    @Override
    public EndpointCharacter getEndpointCharacter() {
        if (definition.containsKey(KEY_CHARACTER)) {
            return EndpointCharacter.fromEndpointDefinitionValue((String) definition.get(KEY_CHARACTER));
        } else {
            return EndpointCharacter.SAME_LOOP;
        }
    }

    @JsonIgnore
    @Override
    public List<InitialDynamicEndpointDefinition> getInitialDynamicEndpointDefinitions() {
        return new ArrayList<InitialDynamicEndpointDefinition>(initialEndpointDefinitions);
    }

    @Override
    public EndpointType getEndpointType() {
        return endpointType;
    }

    public Map<String, Object> getRawEndpointDefinition() {
        return rawEndpointDefinition;
    }

    public Map<String, Object> getRawEndpointDefinitionExtension() {
        return rawEndpointDefinitionExtension;
    }

    public void setEndpointType(EndpointType type) {
        this.endpointType = type;
    }

    /**
     * Initializes fields with raw endpoint information.
     * 
     * @param rawEndpointDefinition raw endpoint information
     */
    @SuppressWarnings("unchecked")
    public void setRawEndpointDefinition(Map<String, Object> rawEndpointDefinition) {
        this.rawEndpointDefinition = rawEndpointDefinition;
        this.rawEndpointGroupDefinition = rawEndpointDefinition;
        this.definition = new HashMap<>(rawEndpointDefinition);

        this.dataTypes = new ArrayList<>();
        for (String dataType : (List<String>) definition.get(EndpointDefinitionConstants.KEY_DATATYPES)) {
            dataTypes.add(DataType.valueOf(dataType));
        }
        Collections.sort(dataTypes);

        this.inputDatumHandlings = new ArrayList<>();
        if (definition.containsKey(EndpointDefinitionConstants.KEY_INPUT_HANDLING_OPTIONS)) {
            for (String inputDatumHandling : (List<String>) definition.get(EndpointDefinitionConstants.KEY_INPUT_HANDLING_OPTIONS)) {
                inputDatumHandlings.add(InputDatumHandling.valueOf(inputDatumHandling));
            }
        } else {
            inputDatumHandlings.add(InputDatumHandling.Single);
        }

        this.inputExecutionContraints = new ArrayList<>();
        if (definition.containsKey(EndpointDefinitionConstants.KEY_EXECUTION_CONSTRAINT_OPTIONS)) {
            for (String inputExecutionContraint : (List<String>) definition
                .get(EndpointDefinitionConstants.KEY_EXECUTION_CONSTRAINT_OPTIONS)) {
                inputExecutionContraints.add(InputExecutionContraint.valueOf(inputExecutionContraint));
            }
        } else {
            inputExecutionContraints.add(InputExecutionContraint.Required);
        }

        Map<String, Map<String, Object>> metaData =
            (Map<String, Map<String, Object>>) definition.get(EndpointDefinitionConstants.KEY_METADATA);
        if (metaData == null) {
            metaData = new HashMap<>();
        }
        this.metaDataDefinition = new EndpointMetaDataDefinitionImpl();
        metaDataDefinition.setRawMetaData(metaData);

        definition.remove(EndpointDefinitionConstants.KEY_METADATA);

        // sanity checks
        if (!dataTypes.contains(getDefaultDataType())) {
            throw new IllegalArgumentException(StringUtils.format("Declared default data type '%s' not in declared list of allowed data "
                + "types '%s'", getDefaultDataType(), dataTypes));
        }
        if (!inputDatumHandlings.contains(getDefaultInputDatumHandling())) {
            throw new IllegalArgumentException(StringUtils.format("Declared default input handling option '%s' not in declared list of "
                + "allowed input handling options '%s'", getDefaultInputDatumHandling(), inputDatumHandlings));
        }
        if (!inputExecutionContraints.contains(getDefaultInputExecutionConstraint())) {
            throw new IllegalArgumentException(StringUtils.format("Declared default input execution constraint option '%s' not in "
                + "declared list of allowed input execution constraint options '%s'", getDefaultInputExecutionConstraint(),
                inputExecutionContraints));
        }

        initialEndpointDefinitions = new ArrayList<>();
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
        this.endpointDefinitionExtension = new HashMap<>(rawEndpointDefinitionExtension);
        Map<String, Map<String, Object>> metaData =
            (Map<String, Map<String, Object>>) endpointDefinitionExtension.get(EndpointDefinitionConstants.KEY_METADATA);
        if (metaData == null) {
            metaData = new HashMap<>();
        }
        metaDataDefinition.setRawMetaDataExtensions(metaData);
    }

}
