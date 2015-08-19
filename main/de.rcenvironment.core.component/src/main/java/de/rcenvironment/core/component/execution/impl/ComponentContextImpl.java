/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.impl;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionControllerCallback;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.PersistedComponentData;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Implementation of {@link ComponentContext}.
 * 
 * @author Doreen Seider
 */
public class ComponentContextImpl implements ComponentContext {

    private static final long serialVersionUID = -8834897231211155860L;

    private String executionIdentifier;

    private String instanceName;

    private final NodeIdentifier node;

    private final String controllersExecutionIdentifier;

    private final String controllersInstanceName;

    private final NodeIdentifier controllersNode;

    private NodeIdentifier defaultStorageNode;

    private File workingDirectory;

    private final ConfigurationDescription configurationDescription;

    private final Set<String> inputs = new HashSet<>();

    private final Set<String> inputsNotConnected = new HashSet<>();

    private final Set<String> outputs = new HashSet<>();

    private final Map<String, String> dynInputsIds = new HashMap<>();

    private final Map<String, String> dynOutputsIds = new HashMap<>();

    private final Map<String, DataType> outputsDataTypes = new HashMap<>();

    private final Map<String, DataType> inputsDataTypes = new HashMap<>();

    private final Map<String, Map<String, String>> inputsMetaData = new HashMap<>();

    private final Map<String, Map<String, String>> outputsMetaData = new HashMap<>();

    private final String componentIdentifier;

    private final String componentName;

    private final ComponentExecutionControllerCallback controllerCallback;

    private final ServiceRegistryAccess serviceRegistryAccess;

    public ComponentContextImpl(ComponentExecutionContext compExeCtx, ComponentExecutionControllerCallback ctrlCallback) {
        executionIdentifier = compExeCtx.getExecutionIdentifier();
        instanceName = compExeCtx.getInstanceName();
        node = compExeCtx.getNodeId();
        controllersNode = compExeCtx.getWorkflowNodeId();
        controllersExecutionIdentifier = compExeCtx.getWorkflowExecutionIdentifier();
        controllersInstanceName = compExeCtx.getWorkflowInstanceName();
        defaultStorageNode = compExeCtx.getWorkflowNodeId();
        workingDirectory = compExeCtx.getWorkingDirectory();
        configurationDescription = compExeCtx.getComponentDescription().getConfigurationDescription();
        for (EndpointDescription ep : compExeCtx.getComponentDescription().getInputDescriptionsManager().getEndpointDescriptions()) {
            String inputExecutionConstraint = ep.getMetaData().get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT);
            if (inputExecutionConstraint == null) {
                inputExecutionConstraint = ep.getDeclarativeEndpointDescription().getDefaultInputExecutionConstraint().name();
            }
            if (inputExecutionConstraint.equals(EndpointDefinition.InputExecutionContraint.Required.name()) || ep.isConnected()) {
                inputs.add(ep.getName());
                dynInputsIds.put(ep.getName(), ep.getDynamicEndpointIdentifier());
            } else if (!ep.isConnected()) {
                inputsNotConnected.add(ep.getName());
            }
            inputsDataTypes.put(ep.getName(), ep.getDataType());
            inputsMetaData.put(ep.getName(), ep.getMetaData());

        }

        for (EndpointDescription ep : compExeCtx.getComponentDescription().getOutputDescriptionsManager().getEndpointDescriptions()) {
            outputs.add(ep.getName());
            dynOutputsIds.put(ep.getName(), ep.getDynamicEndpointIdentifier());
            outputsDataTypes.put(ep.getName(), ep.getDataType());
            outputsMetaData.put(ep.getName(), ep.getMetaData());
        }
        componentIdentifier = compExeCtx.getComponentDescription().getIdentifier();
        componentName = compExeCtx.getComponentDescription().getName();
        controllerCallback = ctrlCallback;
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
    }

    @Override
    public String getExecutionIdentifier() {
        return executionIdentifier;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public NodeIdentifier getNodeId() {
        return node;
    }

    @Override
    public NodeIdentifier getDefaultStorageNodeId() {
        return defaultStorageNode;
    }

    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setInstanceIdentifier(String instanceIdentifier) {
        this.executionIdentifier = instanceIdentifier;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setDefaultStorageNode(NodeIdentifier defaultStorageNode) {
        this.defaultStorageNode = defaultStorageNode;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public Set<String> getReadOnlyConfigurationKeys() {
        return Collections.unmodifiableSet(configurationDescription.getComponentConfigurationDefinition().getReadOnlyConfiguration()
            .getConfigurationKeys());
    }

    @Override
    public Set<String> getConfigurationKeys() {
        return Collections.unmodifiableSet(configurationDescription.getConfiguration().keySet());
    }

    @Override
    public String getConfigurationValue(String key) {
        if (getConfigurationKeys().contains(key)) {
            return configurationDescription.getConfigurationValue(key);
        } else if (getReadOnlyConfigurationKeys().contains(key)) {
            return configurationDescription.getComponentConfigurationDefinition().getReadOnlyConfiguration().getValue(key);
        } else {
            return null;
        }
    }

    @Override
    public String getConfigurationMetaDataValue(String configKey, String metaDataKey) {
        return configurationDescription.getComponentConfigurationDefinition().getConfigurationMetaDataDefinition()
            .getMetaDataValue(configKey, metaDataKey);
    }

    @Override
    public Set<String> getInputs() {
        return Collections.unmodifiableSet(inputs);
    }

    @Override
    public boolean isStaticInput(String inputName) {
        return getDynamicInputIdentifier(inputName) == null;
    }

    @Override
    public boolean isDynamicInput(String inputName) {
        return getDynamicInputIdentifier(inputName) != null;
    }

    @Override
    public Set<String> getInputsWithDatum() {
        return Collections.unmodifiableSet(controllerCallback.getInputsWithDatum());
    }

    @Override
    public DataType getInputDataType(String inputName) {
        return inputsDataTypes.get(inputName);
    }

    @Override
    public Set<String> getInputMetaDataKeys(String inputName) {
        return Collections.unmodifiableSet(inputsMetaData.keySet());
    }

    @Override
    public String getInputMetaDataValue(String inputName, String metaDataKey) {
        return inputsMetaData.get(inputName).get(metaDataKey);
    }

    @Override
    public TypedDatum readInput(String inputName) {
        return controllerCallback.readInput(inputName);
    }

    @Override
    public Set<String> getOutputs() {
        return Collections.unmodifiableSet(outputs);
    }

    @Override
    public DataType getOutputDataType(String outputName) {
        return outputsDataTypes.get(outputName);
    }

    @Override
    public Set<String> getOutputMetaDataKeys(String outputName) {
        return Collections.unmodifiableSet(outputsMetaData.keySet());
    }

    @Override
    public String getOutputMetaDataValue(String outputName, String metaDataKey) {
        return outputsMetaData.get(outputName).get(metaDataKey);
    }

    @Override
    public void writeOutput(String outputName, TypedDatum value) {
        controllerCallback.writeOutput(outputName, value);
    }

    @Override
    public void resetOutput(String outputName) {
        controllerCallback.resetOutput(outputName);
    }

    @Override
    public void closeOutput(String outputName) {
        controllerCallback.closeOutput(outputName);
    }

    @Override
    public void closeAllOutputs() {
        controllerCallback.closeAllOutputs();
    }

    @Override
    public boolean isOutputClosed(String outputName) {
        return controllerCallback.isOutputClosed(outputName);
    }

    @Override
    public void printConsoleLine(String line, Type consoleLineType) {
        controllerCallback.printConsoleRow(line, consoleLineType);
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        return serviceRegistryAccess.getService(clazz);
    }

    @Override
    public int getExecutionCount() {
        return controllerCallback.getExecutionCount();
    }

    @Override
    public NodeIdentifier getWorkflowNodeId() {
        return controllersNode;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return controllersExecutionIdentifier;
    }

    @Override
    public String getWorkflowInstanceName() {
        return controllersInstanceName;
    }

    @Override
    public PersistedComponentData getPersistedData() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void writeIntermediateHistoryData(ComponentHistoryDataItem historyDataItem) {
        controllerCallback.writeIntermediateHistoryData(historyDataItem);
    }

    @Override
    public void writeFinalHistoryDataItem(ComponentHistoryDataItem historyDataItem) {
        controllerCallback.writeFinalHistoryDataItem(historyDataItem);
    }

    @Override
    public String getDynamicInputIdentifier(String inputName) {
        return dynInputsIds.get(inputName);
    }

    @Override
    public String getDynamicOutputIdentifier(String inputName) {
        return dynOutputsIds.get(inputName);
    }

    @Override
    public String getComponentIdentifier() {
        return componentIdentifier;
    }

    @Override
    public String getComponentName() {
        return componentName;
    }

    public Long getComponentExecutionDataManagementId() {
        return controllerCallback.getComponentExecutionDataManagementId();
    }

    @Override
    public Set<String> getInputsNotConnected() {
        return inputsNotConnected;
    }

}
