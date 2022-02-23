/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function.internal;

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
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionsProvider;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.workflow.execution.function.InputAdapterComponent;

/**
 * Builder class that simplifies constructing {@link WorkflowNode}s containing input adapters.
 * 
 * @author Alexander Weinert
 *
 */
class InputAdapterNodeBuilder {
    
    private LogicalNodeId hostId;
    
    private String inputDirectoryAbsolutePath;
    
    private Map<String, DataType> externalInputs = new HashMap<>();
    
    public InputAdapterNodeBuilder setHostId(LogicalNodeId hostIdParam) {
        this.hostId = hostIdParam;
        return this;
    }
    
    public InputAdapterNodeBuilder setInputDirectory(String inputDirectoryAbsolutePathParam) {
        this.inputDirectoryAbsolutePath = inputDirectoryAbsolutePathParam;
        return this;
    }
    
    public InputAdapterNodeBuilder addExternalInputName(String externalInputName, DataType externalInputType) {
        this.externalInputs.put(externalInputName, externalInputType);
        return this;
    }

    public WorkflowNode build() {
        Objects.requireNonNull(this.hostId);
        Objects.requireNonNull(this.inputDirectoryAbsolutePath);

        final Map<String, String> readOnlyConfiguration = new HashMap<>();
        readOnlyConfiguration.put("toolName", "inputAdapter");
        readOnlyConfiguration.put("version", "1.0");
        readOnlyConfiguration.put("hostId", hostId.toString());
        readOnlyConfiguration.put("hostName", "awesomeHostName");
        readOnlyConfiguration.put("isWorkflow", Boolean.TRUE.toString());
        readOnlyConfiguration.put("inputFolder", this.inputDirectoryAbsolutePath);
        final ConfigurationDefinition configuration = ComponentConfigurationModelFactory.createConfigurationDefinition(
            new LinkedList<>(), new LinkedList<>(), new LinkedList<>(), readOnlyConfiguration);

        final Set<EndpointDefinition> inputAdapterInputs = new HashSet<>();
        final EndpointDefinitionsProvider inputAdapterProvider =
            ComponentEndpointModelFactory.createEndpointDefinitionsProvider(inputAdapterInputs);

        final Set<EndpointDefinition> inputAdapterOutputs = new HashSet<>();
        for (Entry<String, DataType> externalInput : this.externalInputs.entrySet()) {
            final EndpointDefinition singleEndpointDefinition = EndpointDefinition
                .outputBuilder()
                .name(externalInput.getKey())
                .allowedDatatype(externalInput.getValue())
                .defaultDatatype(externalInput.getValue())
                .build();
            inputAdapterOutputs.add(singleEndpointDefinition);
        }
        final EndpointDefinitionsProvider outputAdapterProvider =
            ComponentEndpointModelFactory.createEndpointDefinitionsProvider(inputAdapterOutputs);

        final ComponentInterface componentInterface = new ComponentInterfaceBuilder()
            .setIdentifier("InputAdapter")
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
            .setDisplayName("Input Adapter")
            .build();

        ComponentInstallation ci =
            new ComponentInstallationBuilder()
                .setComponentRevision(
                    new ComponentRevisionBuilder()
                        .setComponentInterface(componentInterface)
                        .setClassName(InputAdapterComponent.class.getCanonicalName())
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
        nodeToAdd.setName("Input Adapter");
        return nodeToAdd;
    }


}
