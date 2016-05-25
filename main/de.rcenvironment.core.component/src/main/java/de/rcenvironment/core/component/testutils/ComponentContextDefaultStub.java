/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.io.File;
import java.util.Set;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.execution.api.PersistedComponentData;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Default mock for {@link ComponentContext}.
 * 
 * @author Robert Mischke
 */
public class ComponentContextDefaultStub implements ComponentContext {

    private static final long serialVersionUID = 9197713563968264677L;
    
    private ComponentLog componentLogStub = new ComponentLogDefaultStub();

    @Override
    public String getExecutionIdentifier() {
        return null;
    }

    @Override
    public String getInstanceName() {
        return null;
    }

    @Override
    public NodeIdentifier getNodeId() {
        return null;
    }

    @Override
    public NodeIdentifier getDefaultStorageNodeId() {
        return null;
    }

    @Override
    public Set<String> getReadOnlyConfigurationKeys() {
        return null;
    }

    @Override
    public Set<String> getConfigurationKeys() {
        return null;
    }

    @Override
    public String getConfigurationValue(String key) {
        return null;
    }

    @Override
    public String getConfigurationMetaDataValue(String configKey, String metaDataKey) {
        return null;
    }

    @Override
    public Set<String> getInputs() {
        return null;
    }

    @Override
    public boolean isStaticInput(String inputName) {
        return false;
    }

    @Override
    public boolean isDynamicInput(String inputName) {
        return false;
    }

    @Override
    public String getDynamicInputIdentifier(String inputName) {
        return null;
    }

    @Override
    public DataType getInputDataType(String inputName) {
        return null;
    }

    @Override
    public Set<String> getInputMetaDataKeys(String inputName) {
        return null;
    }

    @Override
    public String getInputMetaDataValue(String inputName, String metaDataKey) {
        return null;
    }

    @Override
    public Set<String> getInputsWithDatum() {
        return null;
    }

    @Override
    public TypedDatum readInput(String inputName) {
        return null;
    }

    @Override
    public Set<String> getOutputs() {
        return null;
    }

    @Override
    public String getDynamicOutputIdentifier(String outputName) {
        return null;
    }

    @Override
    public DataType getOutputDataType(String outputName) {
        return null;
    }

    @Override
    public Set<String> getOutputMetaDataKeys(String outputName) {
        return null;
    }

    @Override
    public String getOutputMetaDataValue(String outputName, String metaDataKey) {
        return null;
    }

    @Override
    public void writeOutput(String outputName, TypedDatum value) {

    }

    @Override
    public void resetOutput(String outputName) {}

    @Override
    public void closeOutput(String outputName) {

    }

    @Override
    public void closeAllOutputs() {

    }

    @Override
    public File getWorkingDirectory() {
        return null;
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        return null;
    }

    @Override
    public int getExecutionCount() {
        return 0;
    }

    @Override
    public PersistedComponentData getPersistedData() {
        return null;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return null;
    }

    @Override
    public String getWorkflowInstanceName() {
        return null;
    }

    @Override
    public NodeIdentifier getWorkflowNodeId() {
        return null;
    }

    @Override
    public void writeIntermediateHistoryData(ComponentHistoryDataItem historyDataItem) {}

    @Override
    public void writeFinalHistoryDataItem(ComponentHistoryDataItem historyDataItem) {}

    @Override
    public String getComponentName() {
        return null;
    }

    @Override
    public String getComponentIdentifier() {
        return null;
    }

    @Override
    public boolean isOutputClosed(String outputName) {
        return false;
    }

    @Override
    public Set<String> getInputsNotConnected() {
        return null;
    }

    @Override
    public boolean isDynamicOutput(String outputName) {
        return false;
    }

    @Override
    public ComponentLog getLog() {
        return componentLogStub;
    }

    @Override
    public void announceExternalProgramStart() {}

    @Override
    public void announceExternalProgramTermination() {}

}
