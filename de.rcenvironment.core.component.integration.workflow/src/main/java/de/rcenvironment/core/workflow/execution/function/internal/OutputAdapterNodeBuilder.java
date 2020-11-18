/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInstallationBuilder;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentInterfaceBuilder;
import de.rcenvironment.core.component.model.api.ComponentRevisionBuilder;
import de.rcenvironment.core.component.model.configuration.api.ComponentConfigurationModelFactory;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;
import de.rcenvironment.core.component.model.endpoint.api.ComponentEndpointModelFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.workflow.execution.function.OutputAdapterComponent;

/**
 * Builder class that simplifies constructing {@link WorkflowNode}s containing input adapters.
 * 
 * @author Alexander Weinert
 *
 */
class OutputAdapterNodeBuilder {
    
    private LogicalNodeId hostId;
    
    private String outputDirectoryAbsolutePath;
    
    private Map<String, DataType> externalOutputs = new HashMap<>();
    
    public OutputAdapterNodeBuilder setHostId(LogicalNodeId hostIdParam) {
        this.hostId = hostIdParam;
        return this;
    }
    
    public OutputAdapterNodeBuilder setOutputDirectory(String outputDirectoryAbsolutePathParam) {
        this.outputDirectoryAbsolutePath = outputDirectoryAbsolutePathParam;
        return this;
    }
    
    public OutputAdapterNodeBuilder addExternalOutputName(String externalInputName, DataType externalInputType) {
        this.externalOutputs.put(externalInputName, externalInputType);
        return this;
    }

    public WorkflowNode build() {
        Objects.requireNonNull(this.hostId);
        Objects.requireNonNull(this.outputDirectoryAbsolutePath);

        final Map<String, String> readOnlyConfiguration = new HashMap<>();
        readOnlyConfiguration.put("toolName", "outputAdapter");
        readOnlyConfiguration.put("version", "1.0");
        readOnlyConfiguration.put("hostId", hostId.toString());
        readOnlyConfiguration.put("hostName", "awesomeHostName");
        readOnlyConfiguration.put("isWorkflow", Boolean.TRUE.toString());
        readOnlyConfiguration.put("outputFolder", this.outputDirectoryAbsolutePath);
        final ConfigurationDefinition configuration = ComponentConfigurationModelFactory.createConfigurationDefinition(
            new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), readOnlyConfiguration);

        final Set<EndpointDefinition> outputAdapterInputs = new HashSet<>();
        for (Entry<String, DataType> externalOutput : this.externalOutputs.entrySet()) {
            final EndpointDefinition singleEndpointDefinition = EndpointDefinition
                .inputBuilder()
                .name(externalOutput.getKey())
                .allowedDatatype(externalOutput.getValue())
                .defaultDatatype(externalOutput.getValue())
                .inputExecutionConstraints(Arrays.asList(InputExecutionContraint.Required))
                .defaultInputExecutionConstraint(InputExecutionContraint.Required)
                .inputHandlings(Arrays.asList(InputDatumHandling.Single))
                .defaultInputHandling(InputDatumHandling.Single)
                .build();
            outputAdapterInputs.add(singleEndpointDefinition);
        }
        final EndpointDefinitionsProvider inputAdapterProvider =
            ComponentEndpointModelFactory.createEndpointDefinitionsProvider(outputAdapterInputs);

        final Set<EndpointDefinition> outputAdapterOutputs = new HashSet<>();
        final EndpointDefinitionsProvider outputAdapterProvider =
            ComponentEndpointModelFactory.createEndpointDefinitionsProvider(outputAdapterOutputs);

        final ComponentInterface componentInterface = new ComponentInterfaceBuilder()
            .setIdentifier("OutputAdapter")
            .setIcon16(new byte[] {})
            .setIcon32(new byte[] {})
            .setGroupName("Dummy Tools")
            .setVersion("0.0")
            .setInputDefinitionsProvider(inputAdapterProvider).setOutputDefinitionsProvider(outputAdapterProvider)
            .setConfigurationDefinition(configuration)
            .setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinition>())
            .setColor(ComponentConstants.COMPONENT_COLOR_STANDARD)
            .setShape(ComponentConstants.COMPONENT_SHAPE_STANDARD)
            .setSize(ComponentConstants.COMPONENT_SIZE_STANDARD)
            .setDisplayName("Output Adapter")
            .build();

        ComponentInstallation ci =
            new ComponentInstallationBuilder()
                .setComponentRevision(
                    new ComponentRevisionBuilder()
                        .setComponentInterface(componentInterface)
                        .setClassName(OutputAdapterComponent.class.getCanonicalName())
                        .build())
                .setNodeId(this.hostId)
                .setInstallationId(componentInterface.getIdentifierAndVersion())
                .build();
        final ComponentDescription componentDescription = new ComponentDescription(ci);

        final WorkflowNode nodeToAdd = new WorkflowNode(componentDescription);
        nodeToAdd.setEnabled(true);
        nodeToAdd.setChecked(false);
        nodeToAdd.setImitiationModeActive(false);
        nodeToAdd.setInit(true);
        nodeToAdd.setName("Output Adapter");
        final int newX = 200;
        nodeToAdd.setLocation(newX, 0);
        return nodeToAdd;
    }


}
