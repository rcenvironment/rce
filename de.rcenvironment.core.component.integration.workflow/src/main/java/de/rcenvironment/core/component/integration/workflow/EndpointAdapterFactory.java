/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.integration.workflow;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter.Builder;

public class EndpointAdapterFactory {

    private static final String TYPE_KEY = "type";

    private static final String IDENTIFIER_KEY = "identifier";

    private static final String INTERNAL_NAME_KEY = "internalName";

    private static final String EXTERNAL_NAME_KEY = "externalName";

    private final WorkflowDescription description;

    private Map<String, String> configurationMap;

    private Collection<String> errorMessages = new LinkedList<>();

    public EndpointAdapterFactory(final WorkflowDescription descriptionParam) {
        this.description = descriptionParam;
    }

    public EndpointAdapter buildFromMap(Map<String, String> map) throws ComponentException {
        final EndpointAdapter.Builder builder;

        this.configurationMap = map;

        if ("INPUT".equals(this.configurationMap.get(TYPE_KEY))) {
            builder = EndpointAdapter.inputAdapterBuilder();
        } else {
            builder = EndpointAdapter.outputAdapterBuilder();
        }
        // final EndpointAdapterDefinition product = new EndpointAdapterDefinition();

        initializeEndpointNames(builder);
        initializeAdaptedDataType(builder);
        initializeInputHandling(builder);
        initializeInputExecutionConstraint(builder);

        if (!this.configurationMap.containsKey(TYPE_KEY)) {
            errorMessages.add("Configuration does not key 'type', which must be set to ether 'INPUT' or 'OUTPUT'");
        } else if (!("INPUT".equals(this.configurationMap.get(TYPE_KEY)) || "OUTPUT".equals(this.configurationMap.get(TYPE_KEY)))) {
            final String errorMessage = StringUtils
                .format("Endpoint adapter defined as unknown type %s. Supported types: 'INPUT', 'OUTPUT'",
                    this.configurationMap.get(TYPE_KEY));
            errorMessages.add(errorMessage);
        }

        throwExceptionIfErrorOccurred();

        if ("INPUT".equals(this.configurationMap.get(TYPE_KEY))) {
            return builder.build();
        } else {
            return builder.build();
        }
    }

    private void initializeInputExecutionConstraint(Builder builder) {
        if (!this.configurationMap.containsKey(IDENTIFIER_KEY)) {
            // If the workflow node to adapt is not present in the workflow, we have already logged an error earlier. Thus, a simple
            // early exit suffices here.
            return;
        }

        final boolean isInputAdapter = "INPUT".equals(this.configurationMap.get(TYPE_KEY));
        
        if (!isInputAdapter) {
            return;
        }
        
        final Optional<EndpointDescription> adaptedInput = getAdaptedEndpoint();
        if (!adaptedInput.isPresent()) {
            // If the adapted input does not exist, we have already loged an error earlier during parsing. Thus, an early exit suffices
            return;
        }
        
        builder.inputExecutionConstraint(
            EndpointDefinition.InputExecutionContraint.valueOf(this.configurationMap.get("inputExecutionConstraint")));
    }

    private void initializeInputHandling(Builder builder) {
        if (!this.configurationMap.containsKey(IDENTIFIER_KEY)) {
            // If the workflow node to adapt is not present in the workflow, we have already logged an error earlier. Thus, a simple
            // early exit suffices here.
            return;
        }

        final boolean isInputAdapter = "INPUT".equals(this.configurationMap.get(TYPE_KEY));
        
        if (!isInputAdapter) {
            return;
        }
        
        final Optional<EndpointDescription> adaptedInput = getAdaptedEndpoint();
        if (!adaptedInput.isPresent()) {
            // If the adapted input does not exist, we have already loged an error earlier during parsing. Thus, an early exit suffices
            return;
        }
        
        builder.inputHandling(InputDatumHandling.valueOf(this.configurationMap.get("inputHandling")));
    }

    private void throwExceptionIfErrorOccurred() throws ComponentException {
        if (errorMessages.isEmpty()) {
            return;
        }

        final String unparsedConfigurationMap = this.configurationMap.entrySet().stream()
            .map(entry -> StringUtils.format("%s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(", ", "{", "}"));
        final String joinedErrorMessages = this.errorMessages.stream()
            .collect(Collectors.joining(". "));

        throw new ComponentException(StringUtils.format("Invalid endpoint adapter definition: %s. Specific errors: %s.",
            unparsedConfigurationMap, joinedErrorMessages));

    }

    private void initializeEndpointNames(final EndpointAdapter.Builder builder) throws ComponentException {
        if (!this.configurationMap.containsKey(IDENTIFIER_KEY)) {
            errorMessages.add("Configuration does not contain required parameter 'identifier'");
        } else {
            builder.workflowNodeIdentifier(this.configurationMap.get(IDENTIFIER_KEY));
        }

        if (!this.configurationMap.containsKey(INTERNAL_NAME_KEY)) {
            errorMessages.add("Configuration does not contain required parameter 'internalName'");
        } else {
            builder.internalEndpointName(this.configurationMap.get(INTERNAL_NAME_KEY));
        }

        if (!this.configurationMap.containsKey(EXTERNAL_NAME_KEY)) {
            errorMessages.add("Configuration does not contain required parameter 'externalName'");
        } else {
            builder.externalEndpointName(this.configurationMap.get(EXTERNAL_NAME_KEY));
        }
    }

    private void initializeAdaptedDataType(final EndpointAdapter.Builder builder) {
        if (!this.configurationMap.containsKey(IDENTIFIER_KEY)) {
            // If the workflow node to adapt is not present in the workflow, we have already logged an error earlier. Thus, a simple
            // early exit suffices here.
            return;
        }

        final Optional<EndpointDescription> internalEndpointDescription = getAdaptedEndpoint();

        if (!internalEndpointDescription.isPresent()) {
            errorMessages.add(StringUtils.format(
                "Endpoint with name '%s' at workflow node with ID '%s' is configured to pass values from or to the external workflow, "
                    + "but that endpoint does not exist on that node",
                this.configurationMap.get(INTERNAL_NAME_KEY), this.configurationMap.get(IDENTIFIER_KEY)));
            return;
        }

        builder.dataType(internalEndpointDescription.get().getDataType());
    }

    private Optional<EndpointDescription> getAdaptedEndpoint() {
        final boolean isInputAdapter = "INPUT".equals(this.configurationMap.get(TYPE_KEY));

        final String workflowNodeIdentifier = this.configurationMap.get(IDENTIFIER_KEY);
        final Optional<WorkflowNode> adaptedWorkflowNode = description.getWorkflowNodes().stream()
            .filter(node -> workflowNodeIdentifier.equals(node.getIdentifierAsObject().toString()))
            .findAny();

        if (!adaptedWorkflowNode.isPresent()) {
            errorMessages.add(StringUtils.format(
                "Workflow node with identifier '%s' is configured to pass values from or to the external workflow, "
                    + "but that node does not exist in the workflow",
                workflowNodeIdentifier));
            return Optional.empty();
        }

        final EndpointDescriptionsManager endpointDescriptions;
        if (isInputAdapter) {
            endpointDescriptions = adaptedWorkflowNode.get().getInputDescriptionsManager();
        } else {
            endpointDescriptions = adaptedWorkflowNode.get().getOutputDescriptionsManager();
        }

        final Stream<EndpointDescription> allEndpointDescriptions = Stream.concat(
            endpointDescriptions.getDynamicEndpointDescriptions().stream(),
            endpointDescriptions.getStaticEndpointDescriptions().stream());
        final Optional<EndpointDescription> internalEndpointDescription = allEndpointDescriptions
            .filter(endpointDescription -> endpointDescription.getName().equals(this.configurationMap.get(INTERNAL_NAME_KEY)))
            .findAny();
        return internalEndpointDescription;
    }
}