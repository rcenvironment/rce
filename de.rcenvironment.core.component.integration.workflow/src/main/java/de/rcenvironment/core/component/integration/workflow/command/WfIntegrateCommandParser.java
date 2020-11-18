/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;

class WfIntegrateCommandParser {

    private static final String EXPOSE_OUTPUT_FLAG = "--expose-output";

    private static final String EXPOSE_INPUT_FLAG = "--expose-input";

    private static final String EXPOSE_FLAG = "--expose";

    private static final String EXPOSE_INPUTS_FLAG = "--expose-inputs";

    private static final String EXPOSE_OUTPUTS_FLAG = "--expose-outputs";

    private static final String[] VALID_EXPOSE_FLAGS =
        new String[] { EXPOSE_FLAG, EXPOSE_INPUT_FLAG, EXPOSE_OUTPUT_FLAG, EXPOSE_INPUTS_FLAG, EXPOSE_OUTPUTS_FLAG };

    public Collection<ParseResult<EndpointAdapter>> parseEndpointAdapterDefinition(CommandContext context,
        WorkflowDescription desc) {
        final String parameterFlag = context.consumeNextToken();
        if (!Arrays.asList(VALID_EXPOSE_FLAGS).contains(parameterFlag)) {
            return Collections.singletonList(ParseResult.createErrorResult(
                StringUtils.format(
                    "Unexpected exposure flag '%s'. Expected '--expose', '--expose-input[s]', or '--expose-output[s]'. "
                        + "Skipping this token.",
                    parameterFlag)));
        }

        final String definition = context.consumeNextToken();

        final String[] parts = definition.split(":");

        if (parts.length != 1 && parts.length != 2 && parts.length != 3) {
            return Collections.singletonList(ParseResult.createErrorResult(StringUtils.format(
                "Could not parse endpoint adapter definition '%s'. "
                    + "Expected format: Either '<component id>:<endpoint id>:<external name>' or '<component id>:<endpoint id>'",
                definition)));
        }
        final String componentId = parts[0];

        final ParseResult<WorkflowNode> adaptedNodeOptional = findAdaptedNode(desc, componentId);
        if (adaptedNodeOptional.isErrorResult()) {
            return Collections.singletonList(ParseResult.createErrorResult(adaptedNodeOptional.getErrorDisplayMessage()));
        }
        final WorkflowNode adaptedNode = adaptedNodeOptional.getResult();

        final String endpointName;
        final String externalName;
        if (parts.length == 1) {
            final boolean exposeInputs = EXPOSE_FLAG.equals(parameterFlag) || EXPOSE_INPUTS_FLAG.equals(parameterFlag);
            final boolean exposeOutputs = EXPOSE_FLAG.equals(parameterFlag) || EXPOSE_OUTPUTS_FLAG.equals(parameterFlag);
            return buildAllEndpointAdaptersForNode(desc, adaptedNode, exposeInputs, exposeOutputs);
        } else if (parts.length == 2) {
            endpointName = parts[1];
            externalName = parts[1];
            return Collections.singletonList(buildEndpointAdapter(desc, parameterFlag, adaptedNode, endpointName, externalName));
        } else {
            // Since we previously checked that parts.length is one of 1, 2, or 3,
            // we know that the array `parts` is of length 3 in this case
            endpointName = parts[1];
            externalName = parts[2];
            return Collections.singletonList(buildEndpointAdapter(desc, parameterFlag, adaptedNode, endpointName, externalName));
        }
    }

    private ParseResult<EndpointAdapter> buildEndpointAdapter(WorkflowDescription desc, final String parameterFlag,
        final WorkflowNode adaptedNode, final String endpointName, final String externalName) {

        final EndpointDescriptionsManager inputDescriptionsManager = adaptedNode.getInputDescriptionsManager();
        final EndpointDescriptionsManager outputDescriptionsManager = adaptedNode.getOutputDescriptionsManager();

        final List<EndpointDescription> potentiallyAdaptedInputs;

        if (EXPOSE_FLAG.equals(parameterFlag) || EXPOSE_INPUT_FLAG.equals(parameterFlag)) {
            potentiallyAdaptedInputs = Stream.concat(
                inputDescriptionsManager.getStaticEndpointDescriptions().stream(),
                inputDescriptionsManager.getDynamicEndpointDescriptions().stream())
                .filter(endpointDescription -> endpointDescription.getName().equals(endpointName))
                .collect(Collectors.toList());
        } else {
            potentiallyAdaptedInputs = new LinkedList<>();
        }

        final List<EndpointDescription> potentiallyAdaptedOutputs;
        if (EXPOSE_FLAG.equals(parameterFlag) || EXPOSE_OUTPUT_FLAG.equals(parameterFlag)) {
            potentiallyAdaptedOutputs = Stream.concat(
                outputDescriptionsManager.getStaticEndpointDescriptions().stream(),
                outputDescriptionsManager.getDynamicEndpointDescriptions().stream())
                .filter(endpointDescription -> endpointDescription.getName().equals(endpointName))
                .collect(Collectors.toList());
        } else {
            potentiallyAdaptedOutputs = new LinkedList<>();
        }

        final int numberOfPotentiallyAdaptedEndpoints = potentiallyAdaptedInputs.size() + potentiallyAdaptedOutputs.size();
        if (numberOfPotentiallyAdaptedEndpoints == 0) {
            final StringBuilder errorMessage = new StringBuilder(
                StringUtils.format("Given endpoint '%s' is not present on node '%s' [ID: %s].", endpointName, adaptedNode.getName(),
                    adaptedNode.getIdentifierAsObject().toString()));

            final String inputs = Stream.concat(
                inputDescriptionsManager.getStaticEndpointDescriptions().stream(),
                inputDescriptionsManager.getDynamicEndpointDescriptions().stream())
                .map(description -> StringUtils.format("%s [ID: %s]", description.getName(), description.getIdentifier()))
                // We pad each entry except for the first one here in order to account for the header "Inputs" that is later added to this
                // list
                .collect(Collectors.joining("\n           "));

            final String outputs = Stream.concat(
                outputDescriptionsManager.getStaticEndpointDescriptions().stream(),
                outputDescriptionsManager.getDynamicEndpointDescriptions().stream())
                .map(description -> StringUtils.format("%s [ID: %s]", description.getName(), description.getIdentifier()))
                // We pad each entry except for the first one here in order to account for the header "Outputs" that is later added to this
                // list
                .collect(Collectors.joining("\n           "));

            if (inputs.isEmpty() && outputs.isEmpty()) {
                errorMessage.append(" That node does not contain any endpoints.");
            } else {
                errorMessage.append(" The following endpoints are present on that node:\n");
                if (!inputs.isEmpty()) {
                    errorMessage.append("  Inputs:  ");
                    errorMessage.append(inputs);
                }

                if (!outputs.isEmpty()) {
                    errorMessage.append("  Outputs: ");
                    errorMessage.append(outputs);
                }
            }

            return ParseResult.createErrorResult(errorMessage.toString());
        } else if (numberOfPotentiallyAdaptedEndpoints > 1) {
            final List<String> errorLines = new ArrayList<>(numberOfPotentiallyAdaptedEndpoints + 1);
            errorLines
                .add(StringUtils.format("Given endpoint '%s' is ambiguous on node '%s' [ID: %s]. Candidates:", endpointName,
                    adaptedNode.getName(), adaptedNode.getIdentifierAsObject().toString()));
            errorLines.addAll(
                Stream.concat(potentiallyAdaptedInputs.stream(), potentiallyAdaptedOutputs.stream())
                    .map(endpointDefinition -> endpointDefinitionToDisplayString(endpointDefinition))
                    .collect(Collectors.toList()));
            return ParseResult.createErrorResult(String.join("\n", errorLines));
        }

        final EndpointDescription adaptedEndpoint =
            Stream.concat(potentiallyAdaptedInputs.stream(), potentiallyAdaptedOutputs.stream()).findFirst().get();

        final EndpointAdapter.Builder definitionBuilder;
        if (potentiallyAdaptedInputs.contains(adaptedEndpoint)) {
            definitionBuilder = EndpointAdapter.inputAdapterBuilder();
        } else {
            definitionBuilder = EndpointAdapter.outputAdapterBuilder();
        }
        definitionBuilder.workflowNodeIdentifier(adaptedNode.getIdentifierAsObject().toString())
            .internalEndpointName(endpointName)
            .externalEndpointName(externalName)
            .dataType(adaptedEndpoint.getDataType());

        if (potentiallyAdaptedInputs.contains(adaptedEndpoint)) {
            return finishBuildingInputAdapter(definitionBuilder, adaptedEndpoint);
        } else {
            return finishBuildingOutputAdapter(definitionBuilder);
        }
    }

    private ParseResult<EndpointAdapter> finishBuildingOutputAdapter(final EndpointAdapter.Builder definitionBuilder) {
        return ParseResult.createSuccessfulResult(definitionBuilder.build());
    }

    private ParseResult<EndpointAdapter> finishBuildingInputAdapter(final EndpointAdapter.Builder definitionBuilder,
        final EndpointDescription adaptedEndpoint) {

        final String inputDatumHandlingString =
            adaptedEndpoint.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING);
        final String inputExecutionConstraintString =
            adaptedEndpoint.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT);

        if (inputDatumHandlingString != null && inputExecutionConstraintString != null) {
            definitionBuilder.inputHandling(InputDatumHandling.valueOf(inputDatumHandlingString));
            definitionBuilder.inputExecutionConstraint(InputExecutionContraint.valueOf(inputExecutionConstraintString));

            // The endpoint definition may be unavailable if the component is unavailable, e.g., due to absent network
            // connections. If neither the meta data nor the endpoint definition can provide us with datum handling or
            // the execution constraint, we have to abort the integration
        } else if (adaptedEndpoint.getEndpointDefinition() != null) {
            definitionBuilder.inputHandling(adaptedEndpoint.getEndpointDefinition().getDefaultInputDatumHandling());
            definitionBuilder.inputExecutionConstraint(adaptedEndpoint.getEndpointDefinition().getDefaultInputExecutionConstraint());
        } else {
            return ParseResult.createErrorResult(StringUtils.format(
                "Could not determine input datum handling of endpoint '%s' [ID: %s]. "
                    + "This may be due to unavailability of the component. "
                    + "Please make sure that all components whose endpoints are adapted are available at the time of integration.",
                adaptedEndpoint.getName(), adaptedEndpoint.getIdentifier()));
        }

        return ParseResult.createSuccessfulResult(definitionBuilder.build());
    }

    private String endpointDefinitionToDisplayString(EndpointDescription endpointDefinition) {
        return StringUtils.format("  %s [ID: %s]", endpointDefinition.getName(),
            endpointDefinition.getIdentifier());
    }

    private ParseResult<WorkflowNode> findAdaptedNode(WorkflowDescription desc, final String componentId) {
        final Stream<WorkflowNode> adaptedNodesByName = desc.getWorkflowNodes().stream()
            .filter(node -> node.getName().equals(componentId));

        final Stream<WorkflowNode> adaptedNodesById = desc.getWorkflowNodes().stream()
            .filter(node -> node.getIdentifierAsObject().toString().equals(componentId));

        final List<WorkflowNode> adaptedNodes = Stream.concat(adaptedNodesByName, adaptedNodesById).collect(Collectors.toList());

        if (adaptedNodes.isEmpty()) {
            return ParseResult.createErrorResult(StringUtils.format("Given node '%s' is not present in workflow", componentId));
        } else if (adaptedNodes.size() > 1) {
            final Collection<String> errorLines = new ArrayList<>(adaptedNodes.size() + 1);
            errorLines.add(StringUtils.format("Given node %s is ambiguous. Candidates:", componentId));
            errorLines.addAll(adaptedNodes.stream()
                .map(wfNode -> StringUtils.format("  %s [ID: %s]", wfNode.getName(), wfNode.getIdentifierAsObject()))
                .collect(Collectors.toList()));

            return ParseResult.createErrorResult(String.join("\n", errorLines));
        }

        final WorkflowNode adaptedNode = adaptedNodes.get(0);
        return ParseResult.createSuccessfulResult(adaptedNode);
    }

    private Collection<ParseResult<EndpointAdapter>> buildAllEndpointAdaptersForNode(final WorkflowDescription desc,
        final WorkflowNode adaptedNode, final boolean exposeInputs, final boolean exposeOutputs) {

        final EndpointDescriptionsManager inputDescriptionsManager = adaptedNode.getInputDescriptionsManager();
        final EndpointDescriptionsManager outputDescriptionsManager = adaptedNode.getOutputDescriptionsManager();

        final List<EndpointDescription> potentiallyAdaptedInputs;

        if (exposeInputs) {
            potentiallyAdaptedInputs = Stream.concat(
                inputDescriptionsManager.getStaticEndpointDescriptions().stream(),
                inputDescriptionsManager.getDynamicEndpointDescriptions().stream())
                .collect(Collectors.toList());
        } else {
            potentiallyAdaptedInputs = new LinkedList<>();
        }

        final List<EndpointDescription> potentiallyAdaptedOutputs;
        if (exposeOutputs) {
            potentiallyAdaptedOutputs = Stream.concat(
                outputDescriptionsManager.getStaticEndpointDescriptions().stream(),
                outputDescriptionsManager.getDynamicEndpointDescriptions().stream())
                .collect(Collectors.toList());
        } else {
            potentiallyAdaptedOutputs = new LinkedList<>();
        }

        final int numberOfPotentiallyAdaptedEndpoints = potentiallyAdaptedInputs.size() + potentiallyAdaptedOutputs.size();
        if (numberOfPotentiallyAdaptedEndpoints == 0) {
            return Collections.singletonList(ParseResult
                .createErrorResult(StringUtils.format("There are no endpoints present to expose on node '%s'", adaptedNode.getName())));
        }

        final List<ParseResult<EndpointAdapter>> returnValue = new LinkedList<>();
        for (EndpointDescription adaptedInput : potentiallyAdaptedInputs) {

            final EndpointAdapter.Builder definitionBuilder = EndpointAdapter.inputAdapterBuilder()
                .workflowNodeIdentifier(adaptedNode.getIdentifierAsObject().toString())
                .internalEndpointName(adaptedInput.getName())
                .externalEndpointName(adaptedInput.getName())
                .dataType(adaptedInput.getDataType());

            returnValue.add(finishBuildingInputAdapter(definitionBuilder, adaptedInput));
        }

        for (EndpointDescription adaptedOutput : potentiallyAdaptedOutputs) {
            final EndpointAdapter.Builder definitionBuilder = EndpointAdapter.outputAdapterBuilder()
                .workflowNodeIdentifier(adaptedNode.getIdentifierAsObject().toString())
                .internalEndpointName(adaptedOutput.getName())
                .externalEndpointName(adaptedOutput.getName())
                .dataType(adaptedOutput.getDataType());

            returnValue.add(finishBuildingOutputAdapter(definitionBuilder));
        }

        return returnValue;
    }
}
