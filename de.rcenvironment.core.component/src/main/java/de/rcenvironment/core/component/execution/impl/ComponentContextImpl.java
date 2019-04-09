/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.impl;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.api.ServiceCallContextUtils;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.ConsoleRow.WorkflowLifecyleEventType;
import de.rcenvironment.core.component.execution.api.PersistedComponentData;
import de.rcenvironment.core.component.execution.internal.ComponentContextBridge;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionStorageBridge;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Implementation of {@link ComponentContext}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ComponentContextImpl implements ComponentContext {

    private static final long serialVersionUID = -8834897231211155860L;

    private String compExeId;

    private String instanceName;

    private final LogicalNodeId node;

    private final String wfExeCrtlId;

    private final String wfCtrlInstanceName;

    private final LogicalNodeId wfCtrlNode;

    private final LogicalNodeId storageNodeId;

    private final NetworkDestination storageNetworkDestination;

    private File workingDirectory;

    private final ConfigurationDescription configurationDescription;

    private final Set<String> inputs = new HashSet<>();

    private final Set<String> inputsNotConnected = new HashSet<>();

    private final Set<String> outputs = new HashSet<>();

    private final Map<String, String> dynInputsIds = new HashMap<>();

    private final Map<String, String> dynOutputsIds = new HashMap<>();

    private final Map<String, DataType> outputsDataTypes = new HashMap<>();

    private final Map<String, DataType> inputsDataTypes = new HashMap<>();

    private final Map<String, EndpointCharacter> outputsCharacter = new HashMap<>();

    private final Map<String, EndpointCharacter> inputsCharacter = new HashMap<>();

    private final Map<String, Map<String, String>> inputsMetaData = new HashMap<>();

    private final Map<String, Map<String, String>> outputsMetaData = new HashMap<>();

    private final String componentIdentifier;

    private final String componentName;

    private final ComponentContextBridge compExeCtxBridge;

    private final ServiceRegistryAccess serviceRegistryAccess;

    private final ComponentLog log = new ComponentLog() {

        @Override
        public void toolStdout(String message) {
            compExeCtxBridge.printConsoleRow(message, Type.TOOL_OUT);
        }

        @Override
        public void toolStderr(String message) {
            compExeCtxBridge.printConsoleRow(message, Type.TOOL_ERROR);
        }

        @Override
        public void componentWarn(String message) {
            compExeCtxBridge.printConsoleRow(message, Type.COMPONENT_WARN);
        }

        @Override
        public void componentInfo(String message) {
            compExeCtxBridge.printConsoleRow(message, Type.COMPONENT_INFO);
        }

        @Override
        public void componentError(String message) {
            compExeCtxBridge.printConsoleRow(message, Type.COMPONENT_ERROR);
        }

        @Override
        public void componentError(String message, Throwable t, String errorId) {
            compExeCtxBridge.printConsoleRow(StringUtils.format("%s: %s (%s)", message, t.getMessage(), errorId), Type.COMPONENT_ERROR);
        }
    };

    public ComponentContextImpl(ComponentExecutionContext compExeCtx, ComponentContextBridge compExeCtxBridge,
        ComponentExecutionStorageBridge compExeStorageBridge) {
        compExeId = compExeCtx.getExecutionIdentifier();
        instanceName = compExeCtx.getInstanceName();
        node = compExeCtx.getNodeId();
        wfCtrlNode = compExeCtx.getWorkflowNodeId();
        wfExeCrtlId = compExeCtx.getWorkflowExecutionIdentifier();
        wfCtrlInstanceName = compExeCtx.getWorkflowInstanceName();
        storageNodeId = compExeCtx.getStorageNodeId();
        storageNetworkDestination = compExeStorageBridge.getStorageNetworkDestination();
        // compExeCtxBridge.
        workingDirectory = compExeCtx.getWorkingDirectory();
        configurationDescription = compExeCtx.getComponentDescription().getConfigurationDescription();
        for (EndpointDescription ep : compExeCtx.getComponentDescription().getInputDescriptionsManager().getEndpointDescriptions()) {
            String inputExecutionConstraint = ep.getMetaData().get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT);
            if (inputExecutionConstraint == null) {
                inputExecutionConstraint = ep.getEndpointDefinition().getDefaultInputExecutionConstraint().name();
            }
            if (inputExecutionConstraint.equals(EndpointDefinition.InputExecutionContraint.Required.name()) || ep.isConnected()) {
                inputs.add(ep.getName());
                dynInputsIds.put(ep.getName(), ep.getDynamicEndpointIdentifier());
            } else if (!ep.isConnected()) {
                inputsNotConnected.add(ep.getName());
            }
            inputsDataTypes.put(ep.getName(), ep.getDataType());
            inputsCharacter.put(ep.getName(), ep.getEndpointDefinition().getEndpointCharacter());
            inputsMetaData.put(ep.getName(), ep.getMetaData());

        }

        for (EndpointDescription ep : compExeCtx.getComponentDescription().getOutputDescriptionsManager().getEndpointDescriptions()) {
            outputs.add(ep.getName());
            dynOutputsIds.put(ep.getName(), ep.getDynamicEndpointIdentifier());
            outputsDataTypes.put(ep.getName(), ep.getDataType());
            outputsCharacter.put(ep.getName(), ep.getEndpointDefinition().getEndpointCharacter());
            outputsMetaData.put(ep.getName(), ep.getMetaData());
        }
        componentIdentifier = compExeCtx.getComponentDescription().getIdentifier();
        componentName = compExeCtx.getComponentDescription().getName();

        this.compExeCtxBridge = compExeCtxBridge;

        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
    }

    @Override
    public String getExecutionIdentifier() {
        return compExeId;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public LogicalNodeId getNodeId() {
        return node;
    }

    @Override
    public LogicalNodeId getStorageNodeId() {
        return storageNodeId;
    }

    @Override
    public NetworkDestination getStorageNetworkDestination() {
        return storageNetworkDestination;
    }

    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setInstanceIdentifier(String instanceIdentifier) {
        this.compExeId = instanceIdentifier;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
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
    public boolean isDynamicOutput(String outputName) {
        return getDynamicOutputIdentifier(outputName) != null;
    }

    @Override
    public Set<String> getInputsWithDatum() {
        return Collections.unmodifiableSet(compExeCtxBridge.getInputsWithDatum());
    }

    @Override
    public DataType getInputDataType(String inputName) {
        return inputsDataTypes.get(inputName);
    }

    @Override
    public Set<String> getInputMetaDataKeys(String inputName) {
        return Collections.unmodifiableSet(inputsMetaData.get(inputName).keySet());
    }

    @Override
    public String getInputMetaDataValue(String inputName, String metaDataKey) {
        return inputsMetaData.get(inputName).get(metaDataKey);
    }

    @Override
    public TypedDatum readInput(String inputName) {
        return compExeCtxBridge.readInput(inputName);
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
        return Collections.unmodifiableSet(outputsMetaData.get(outputName).keySet());
    }

    @Override
    public String getOutputMetaDataValue(String outputName, String metaDataKey) {
        return outputsMetaData.get(outputName).get(metaDataKey);
    }

    @Override
    public void writeOutput(String outputName, TypedDatum value) {
        compExeCtxBridge.writeOutput(outputName, value);
    }

    @Override
    public void resetOutputs() {
        compExeCtxBridge.resetOutputs();
    }

    @Override
    public void closeOutput(String outputName) {
        compExeCtxBridge.closeOutput(outputName);
    }

    @Override
    public void closeAllOutputs() {
        compExeCtxBridge.closeAllOutputs();
    }

    @Override
    public boolean isOutputClosed(String outputName) {
        return compExeCtxBridge.isOutputClosed(outputName);
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        return serviceRegistryAccess.getService(clazz);
    }

    @Override
    public int getExecutionCount() {
        return compExeCtxBridge.getExecutionCount();
    }

    @Override
    public LogicalNodeId getWorkflowNodeId() {
        return wfCtrlNode;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return wfExeCrtlId;
    }

    @Override
    public String getWorkflowInstanceName() {
        return wfCtrlInstanceName;
    }

    @Override
    public PersistedComponentData getPersistedData() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void writeIntermediateHistoryData(ComponentHistoryDataItem historyDataItem) {
        compExeCtxBridge.writeIntermediateHistoryData(historyDataItem);
    }

    @Override
    public void writeFinalHistoryDataItem(ComponentHistoryDataItem historyDataItem) {
        compExeCtxBridge.writeFinalHistoryDataItem(historyDataItem);
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
        return compExeCtxBridge.getComponentExecutionDataManagementId();
    }

    @Override
    public Set<String> getInputsNotConnected() {
        return inputsNotConnected;
    }

    @Override
    public ComponentLog getLog() {
        return log;
    }

    @Override
    public ServiceCallContext getServiceCallContext() {
        return ServiceCallContextUtils.getCurrentServiceCallContext();
    }

    @Override
    public void announceExternalProgramStart() {
        compExeCtxBridge.printConsoleRow(WorkflowLifecyleEventType.TOOL_STARTING.name(), ConsoleRow.Type.LIFE_CYCLE_EVENT);
    }

    @Override
    public void announceExternalProgramTermination() {
        compExeCtxBridge.printConsoleRow(WorkflowLifecyleEventType.TOOL_FINISHED.name(), ConsoleRow.Type.LIFE_CYCLE_EVENT);
    }

    @Override
    public EndpointCharacter getInputCharacter(String inputName) {
        return inputsCharacter.get(inputName);
    }

    @Override
    public EndpointCharacter getOutputCharacter(String outputName) {
        return outputsCharacter.get(outputName);
    }

    @Override
    public List<String> getDynamicInputsWithIdentifier(String identifier) {
        List<String> result = new LinkedList<>();
        for (String endpoint : inputs) {
            if (isDynamicInput(endpoint) && getDynamicInputIdentifier(endpoint).equals(identifier)) {
                result.add(endpoint);
            }
        }
        return result;
    }

    @Override
    public List<String> getDynamicOutputsWithIdentifier(String identifier) {
        List<String> result = new LinkedList<>();
        for (String endpoint : outputs) {
            if (isDynamicOutput(endpoint) && getDynamicOutputIdentifier(endpoint).equals(identifier)) {
                result.add(endpoint);
            }
        }
        return result;
    }
}
