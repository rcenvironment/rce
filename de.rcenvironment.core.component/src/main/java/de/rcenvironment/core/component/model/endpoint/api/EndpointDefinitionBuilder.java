/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.model.endpoint.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * 
 * @author Alexander Weinert
 *
 */
public class EndpointDefinitionBuilder {

    private final EndpointType type;

    private final Map<String, Object> rawDefinition = new HashMap<>();

    public EndpointDefinitionBuilder(EndpointType type) {
        this.type = type;
        rawDefinition.put(EndpointDefinitionConstants.KEY_DATATYPES, new LinkedList<String>());
    }

    /**
     * @param defaultDatatype The default datatype of this endpoint
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder defaultDatatype(DataType defaultDatatype) {
        this.rawDefinition.put(EndpointDefinitionConstants.KEY_DEFAULT_DATATYPE, defaultDatatype.name());
        return this;
    }

    /**
     * Convenience method for setting a single allowed datatype.
     * 
     * @param datatype The single allowed datatype for this endpoint
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder allowedDatatype(DataType datatype) {
        return this.allowedDatatypes(Arrays.asList(datatype));
    }

    /**
     * @param datatypes A collection of allowed datatypes for this endpoint
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder allowedDatatypes(Collection<DataType> datatypes) {
        final List<String> stringList = datatypes.stream()
            .map(DataType::name)
            .collect(Collectors.toList());

        rawDefinition.put(EndpointDefinitionConstants.KEY_DATATYPES, stringList);
        return this;
    }

    /**
     * @param name The name of this endpoint
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder name(String name) {
        rawDefinition.put(EndpointDefinitionConstants.KEY_NAME, name);
        return this;
    }

    /**
     * @param inputHandlings A collection of allowed handling types for this endpoint
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder inputHandlings(Collection<InputDatumHandling> inputHandlings) {
        final List<String> stringList = inputHandlings.stream()
            .map(InputDatumHandling::name)
            .collect(Collectors.toList());

        rawDefinition.put(EndpointDefinitionConstants.KEY_INPUT_HANDLING_OPTIONS , stringList);
        return this;
    }
    
    /**
     * @param handling The default handling of the input data arriving at this endpoint
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder defaultInputHandling(InputDatumHandling handling) {
        rawDefinition.put(EndpointDefinitionConstants.KEY_DEFAULT_INPUT_HANDLING, handling.name());
        return this;
    }
    
    /**
     * @param constraint The default execution constraint for input data arriving at this endpoint
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder defaultInputExecutionConstraint(InputExecutionContraint constraint) {
        rawDefinition.put(EndpointDefinitionConstants.KEY_DEFAULT_EXECUTION_CONSTRAINT, constraint.name());
        return this;
    }

    /**
     * @param constraintList A collection of valid execution constraints for data arriving at this endpoint
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder inputExecutionConstraints(Collection<InputExecutionContraint> constraintList) {
        final List<String> stringList = constraintList.stream()
            .map(InputExecutionContraint::name)
            .collect(Collectors.toList());

        rawDefinition.put(EndpointDefinitionConstants.KEY_EXECUTION_CONSTRAINT_OPTIONS , stringList);
        return this;
    }
    
    /**
     * @param metadata The metadata to store in this endpoint.
     * @return This object for daisy-chaining
     */
    public EndpointDefinitionBuilder metadata(Map<String, Map<String, Object>> metadata) {
        rawDefinition.put(EndpointDefinitionConstants.KEY_METADATA, metadata);
        return this;
    }

    /**
     * @return The EndpointDefinition as defined by previous calls to this object
     */
    public EndpointDefinition build() {
        if (!rawDefinition.containsKey(EndpointDefinitionConstants.KEY_DEFAULT_DATATYPE)) {
            throw new IllegalStateException("Cannot build an EndpointDefinition without having set a default datatype");
        }

        return ComponentEndpointModelFactory.createEndpointDefinition(rawDefinition, type);
    }

}
