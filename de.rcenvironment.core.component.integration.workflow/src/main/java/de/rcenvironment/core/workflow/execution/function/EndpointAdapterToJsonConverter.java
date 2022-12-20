/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationConstants;
import de.rcenvironment.core.component.integration.workflow.internal.WorkflowIntegrationServiceImpl;

/**
 * In order to integrate a workflow as a component, we have to write an integration file that contains information on how to pass inputs
 * into and obtain outputs from the integrated workflow. That integration file contains both information on the inputs and outputs of the
 * component as seen by the workflow that calls it as well as workflow-as-component-specific parts that detail which inputs of the component
 * correspond to which inputs of components contained in the workflow. Since the configuration file is not represented as a class in RCE due
 * to legacy code, but instead as a simple Map<String, Object>, we opt to unparse these endpoint adapters characterized above manually as
 * well instead of unparsing them directly via Jackson.
 * 
 * Hence, this class transforms {@link EndpointAdapters} into built-in Java {@link List}s and {@link Map}s that can then be unparsed into
 * the configuration file using Jackson in {@link WorkflowIntegrationServiceImpl}.
 * 
 * @author Alexander Weinert
 * @author Jan Flink
 */
public final class EndpointAdapterToJsonConverter {

    private static final String VALUE_REQUIRED = "Required";

    private final EndpointAdapters endpointAdapterDefinitions;

    public EndpointAdapterToJsonConverter(EndpointAdapters endpointAdapterDefinitions) {
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

        inputMap.put(IntegrationConstants.KEY_ENDPOINT_FILENAME, "");
        inputMap.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_DATA_TYPE_KEY, definition.getDataType().name());

        inputMap.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INPUT_EXECUTION_CONSTRAINT,
            String.valueOf(definition.getInputExecutionConstraint()));
        inputMap.put(IntegrationConstants.KEY_DEFAULT_INPUT_EXECUTION_CONSTRAINT, String.valueOf(definition.getInputExecutionConstraint()));

        inputMap.put(IntegrationConstants.KEY_DEFAULT_INPUT_HANDLING, String.valueOf(definition.getInputDatumHandling()));
        inputMap.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INPUT_HANDLING, String.valueOf(definition.getInputDatumHandling()));

        inputMap.put(IntegrationConstants.KEY_ENDPOINT_NAME, definition.getExternalName());
        inputMap.put(IntegrationConstants.KEY_ENDPOINT_FOLDER, "");

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

        outputMap.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INPUT_HANDLING, "-");
        outputMap.put(IntegrationConstants.KEY_ENDPOINT_FILENAME, "");
        outputMap.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_DATA_TYPE_KEY, definition.getDataType().name());
        outputMap.put("endpointName", definition.getExternalName());
        outputMap.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INPUT_EXECUTION_CONSTRAINT, VALUE_REQUIRED);
        outputMap.put(IntegrationConstants.KEY_ENDPOINT_FOLDER, "");

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
            returnValue.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_TYPE,
                WorkflowIntegrationConstants.VALUE_ENDPOINT_ADAPTER_INPUT);
            returnValue.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INPUT_HANDLING, definition.getInputDatumHandling());
            returnValue.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INPUT_EXECUTION_CONSTRAINT,
                definition.getInputExecutionConstraint());
        } else {
            returnValue.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_TYPE,
                WorkflowIntegrationConstants.VALUE_ENDPOINT_ADAPTER_OUTPUT);
        }

        returnValue.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_INTERNAL_NAME, definition.getInternalName());
        returnValue.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_EXTERNAL_NAME, definition.getExternalName());
        returnValue.put(WorkflowIntegrationConstants.KEY_ENDPOINT_ADAPTER_NODE_IDENTIFIER, definition.getWorkflowNodeIdentifier());

        return returnValue;
    }

}
