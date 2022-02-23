/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;

/**
 * In order to integrate a workflow as a component, we have to write an integration file that contains information on how to pass inputs
 * into and obtain outputs from the integrated workflow. That integration file contains both information on the inputs and outputs of the
 * component as seen by the workflow that calls it as well as workflow-as-component-specific parts that detail which inputs of the component
 * correspond to which inputs of components contained in the workflow. Since the configuration file is not represented as a class in RCE due
 * to legacy code, but instead as a simple Map<String, Object>, we opt to unparse these endpoint adapters characterized above manually as
 * well instead of unparsing them directly via Jackson.
 * 
 * Hence, this class transforms {@link EndpointAdapters} into built-in Java {@link List}s and {@link Map}s that can then be
 * unparsed into the configuration file using Jackson in {@link WorkflowIntegrationServiceImpl}.
 * 
 * @author Alexander Weinert
 */
final class EndpointAdapterToJsonConverter {

    private static final String ENDPOINT_DATA_TYPE_KEY = "endpointDataType";

    private static final String REQUIRED = "Required";

    private final EndpointAdapters endpointAdapterDefinitions;

    EndpointAdapterToJsonConverter(EndpointAdapters endpointAdapterDefinitions) {
        this.endpointAdapterDefinitions = endpointAdapterDefinitions;
    }

    public List<Map<String, Object>> toInputDefinitions() {
        return endpointAdapterDefinitions.stream()
            .filter(EndpointAdapter::isInputAdapter)
            .map(this::toInputDefinition)
            .collect(Collectors.toList());
    }

    private Map<String, Object> toInputDefinition(EndpointAdapter definition) {
        final Map<String, Object> inputMap = new HashMap<>();

        inputMap.put("endpointFileName", "");
        inputMap.put(ENDPOINT_DATA_TYPE_KEY, definition.getDataType().name());

        inputMap.put("inputExecutionConstraint", String.valueOf(definition.getInputExecutionConstraint()));
        inputMap.put("defaultInputExecutionConstraint", String.valueOf(definition.getInputExecutionConstraint()));

        inputMap.put("defaultInputHandling", String.valueOf(definition.getInputDatumHandling()));
        inputMap.put("inputHandling", String.valueOf(definition.getInputDatumHandling()));

        inputMap.put("endpointName", definition.getExternalName());
        inputMap.put("endpointFolder", "");

        return inputMap;
    }

    public List<Map<String, Object>> toOutputDefinitions() {
        return endpointAdapterDefinitions.stream()
            .filter(EndpointAdapter::isOutputAdapter)
            .map(this::toOutputDefinition)
            .collect(Collectors.toList());
    }

    private Map<String, Object> toOutputDefinition(EndpointAdapter definition) {
        final Map<String, Object> outputMap = new HashMap<>();

        outputMap.put("inputHandling", "-");
        outputMap.put("endpointFileName", "");
        outputMap.put(ENDPOINT_DATA_TYPE_KEY, definition.getDataType().name());
        outputMap.put("endpointName", definition.getExternalName());
        outputMap.put("inputExecutionConstraint", REQUIRED);
        outputMap.put("endpointFolder", "");

        return outputMap;

    }

    public List<Map<String, Object>> toEndpointAdapterDefinitions() {
        return this.endpointAdapterDefinitions.stream()
            .map(this::toEndpointAdapterConfiguration)
            .collect(Collectors.toList());
    }

    private Map<String, Object> toEndpointAdapterConfiguration(EndpointAdapter definition) {
        final Map<String, Object> returnValue = new HashMap<>();
        if (definition.isInputAdapter()) {
            returnValue.put("type", "INPUT");
            returnValue.put("inputHandling", definition.getInputDatumHandling());
            returnValue.put("inputExecutionConstraint", definition.getInputExecutionConstraint());
        } else {
            returnValue.put("type", "OUTPUT");
        }

        returnValue.put("internalName", definition.getInternalName());
        returnValue.put("externalName", definition.getExternalName());
        returnValue.put("identifier", definition.getWorkflowNodeIdentifier());

        return returnValue;
    }

}
