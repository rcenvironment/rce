/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function.internal;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;

// TODO Error handling
class WorkflowEditor {

    private final WorkflowDescription workflowDescription;

    private String inputDirectory;

    private String outputDirectory;

    private Collection<EndpointAdapter> inputAdapterDefinitions = new HashSet<>();

    private Collection<EndpointAdapter> outputAdapterDefinitions = new HashSet<>();

    private WorkflowNode inputAdapterNode;

    private WorkflowNode outputAdapterNode;

    public static class Factory {

        private String inputDirectoryPath;

        private String outputDirectoryPath;

        public Factory setInputDirectory(final File inputDirectory) {
            this.inputDirectoryPath = inputDirectory.getAbsolutePath();
            return this;
        }

        public Factory setOutputDirectory(final File outputDirectory) {
            this.outputDirectoryPath = outputDirectory.getAbsolutePath();
            return this;
        }

        public WorkflowEditor buildFromWorkflowDescription(final WorkflowDescription description) {
            final WorkflowEditor product = new WorkflowEditor(description.clone());
            product.inputDirectory = inputDirectoryPath;
            product.outputDirectory = outputDirectoryPath;
            return product;
        }
    }

    WorkflowEditor(WorkflowDescription description) {
        this.workflowDescription = description;
    }

    public void addInputAdapter(EndpointAdapter definition) {
        assert definition.isInputAdapter();
        this.inputAdapterDefinitions.add(definition);
    }

    public void addOutputAdapter(final EndpointAdapter definition) {
        assert definition.isOutputAdapter();
        this.outputAdapterDefinitions.add(definition);
    }

    public WorkflowDescription getResult() {
        if (!this.inputAdapterDefinitions.isEmpty()) {
            injectInputAdapterIntoWorkflow();
        }
        if (!this.outputAdapterDefinitions.isEmpty()) {
            injectOutputAdapterIntoWorkflow();
        }
        return this.workflowDescription;
    }

    private void injectInputAdapterIntoWorkflow() {
        final InputAdapterNodeBuilder builder = new InputAdapterNodeBuilder()
            .setHostId(this.workflowDescription.getControllerNode())
            .setInputDirectory(this.inputDirectory);

        for (EndpointAdapter definition : this.inputAdapterDefinitions) {
            builder.addExternalInputName(definition.getExternalName(), definition.getDataType());
        }

        this.inputAdapterNode = builder.build();

        workflowDescription.addWorkflowNode(this.inputAdapterNode);
        connectInputAdapter();
    }

    private void connectInputAdapter() {
        for (EndpointAdapter definition : this.inputAdapterDefinitions) {
            final Connection adapterConnection = buildInputAdapterConnection(definition);
            workflowDescription.addConnection(adapterConnection);
        }
    }

    private Connection buildInputAdapterConnection(EndpointAdapter definition) {
        // TODO User Error Handling. Maybe here, maybe it should come earlier, but it should be present in any case
        final WorkflowNode adaptedWorkflowNode = workflowDescription.getWorkflowNodes().stream()
            .filter(node -> node.getIdentifierAsObject().toString().equals(definition.getWorkflowNodeIdentifier()))
            .findAny().get();

        final Set<EndpointDescription> adaptedInputs = new HashSet<>();
        final EndpointDescriptionsManager inputDescriptionsManager =
            adaptedWorkflowNode.getInputDescriptionsManager();
        adaptedInputs.addAll(inputDescriptionsManager.getDynamicEndpointDescriptions());
        adaptedInputs.addAll(inputDescriptionsManager.getStaticEndpointDescriptions());

        if (adaptedInputs.isEmpty()) {
            throw new IllegalStateException(
                StringUtils.format("Found no inputs to adapt on workflow node '%s'", adaptedWorkflowNode));
        }

        final List<EndpointDescription> potentialAdaptedInputs =
            adaptedInputs.stream()
                .filter(input -> {
                    LogFactory.getLog(this.getClass()).info(StringUtils.format("Checking input %s", input.getName()));
                    return input.getName().equals(definition.getInternalName());
                })
                .collect(Collectors.toList());

        if (potentialAdaptedInputs.isEmpty()) {
            throw new IllegalStateException(
                StringUtils.format("Found no inputs of name '%s' to adapt on workflow node '%s'", definition.getInternalName(),
                    adaptedWorkflowNode));
        } else if (potentialAdaptedInputs.size() > 1) {
            throw new IllegalStateException(
                StringUtils.format("Found multiple inputs of name '%s' to adapt on workflow node '%s'", definition.getInternalName(),
                    adaptedWorkflowNode));
        }

        final EndpointDescription inputToAdapt = potentialAdaptedInputs.get(0);

        final EndpointDescription relevantInputAdapterOutput = this.inputAdapterNode
            .getOutputDescriptionsManager().getStaticEndpointDescriptions().stream()
            .filter(endpointDescription -> endpointDescription.getName().equals(definition.getExternalName()))
            .findAny().get();

        final Connection adapterConnection = new Connection(
            this.inputAdapterNode, relevantInputAdapterOutput, adaptedWorkflowNode, inputToAdapt);
        return adapterConnection;
    }

    private void injectOutputAdapterIntoWorkflow() {
        final OutputAdapterNodeBuilder builder = new OutputAdapterNodeBuilder()
            .setHostId(this.workflowDescription.getControllerNode())
            .setOutputDirectory(this.outputDirectory);

        for (EndpointAdapter definition : this.outputAdapterDefinitions) {
            builder.addExternalOutputName(definition.getExternalName(), definition.getDataType());
        }

        this.outputAdapterNode = builder.build();

        workflowDescription.addWorkflowNode(this.outputAdapterNode);
        connectOutputAdapter();
    }

    private void connectOutputAdapter() {
        for (EndpointAdapter definition : this.outputAdapterDefinitions) {
            final Connection adapterConnection = buildOutputAdapterConnection(definition);
            workflowDescription.addConnection(adapterConnection);
        }
    }

    private Connection buildOutputAdapterConnection(EndpointAdapter definition) {
        // TODO User Error Handling. Maybe here, maybe it should come earlier, but it should be present in any case
        final WorkflowNode adaptedWorkflowNode = workflowDescription.getWorkflowNodes().stream()
            .filter(node -> node.getIdentifierAsObject().toString().equals(definition.getWorkflowNodeIdentifier()))
            .findAny().get();

        final Set<EndpointDescription> adaptedOutputs = new HashSet<>();
        final EndpointDescriptionsManager outputDescriptionsManager =
            adaptedWorkflowNode.getOutputDescriptionsManager();
        adaptedOutputs.addAll(outputDescriptionsManager.getDynamicEndpointDescriptions());
        adaptedOutputs.addAll(outputDescriptionsManager.getStaticEndpointDescriptions());

        if (adaptedOutputs.isEmpty()) {
            throw new IllegalStateException(
                StringUtils.format("Found no outputs to adapt on workflow node '%s'", adaptedWorkflowNode));
        }

        final List<EndpointDescription> potentialAdaptedOutputs =
            adaptedOutputs.stream()
                .filter(output -> output.getName().equals(definition.getInternalName()))
                .collect(Collectors.toList());

        if (potentialAdaptedOutputs.isEmpty()) {
            throw new IllegalStateException(
                StringUtils.format("Found no outputs of name '%s' to adapt on workflow node '%s'", definition.getInternalName(),
                    adaptedWorkflowNode));
        } else if (potentialAdaptedOutputs.size() > 1) {
            throw new IllegalStateException(
                StringUtils.format("Found multiple outputs of name '%s' to adapt on workflow node '%s'", definition.getInternalName(),
                    adaptedWorkflowNode));
        }

        final EndpointDescription outputToAdapt = potentialAdaptedOutputs.get(0);

        // TODO Wrap the output adapter node in an object to simplify this access
        final EndpointDescription relevantOutputAdapterInput = this.outputAdapterNode
            .getInputDescriptionsManager().getStaticEndpointDescriptions().stream()
            .filter(endpointDescription -> endpointDescription.getName().equals(definition.getExternalName()))
            .findAny().get();

        final Connection adapterConnection = new Connection(
            adaptedWorkflowNode, outputToAdapt, this.outputAdapterNode, relevantOutputAdapterInput);
        return adapterConnection;
    }

}
