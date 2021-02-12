/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.testutils.TypedDatumServiceDefaultStub;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.common.AutoCreationMap;

/**
 * Placeholder implementation of {@link ComponentContext} for writing {@link Component} integration tests.
 * 
 * Individual tests may subclass it to add additional featured. Code that can be useful for other tests should be merged into this base
 * class.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 * @author Brigitte Boden
 */
public class ComponentContextMock extends ComponentContextDefaultStub {

    private static final long serialVersionUID = -8621047700219054161L;

    private final Map<String, String> testConfig = new HashMap<>();

    private final Map<String, SimulatedEndpoint> simulatedInputs = new HashMap<>();

    private final Map<String, SimulatedEndpoint> simulatedOutputs = new HashMap<>();

    private Map<String, TypedDatum> inputValues;

    private AutoCreationMap<String, List<TypedDatum>> capturedOutputValues;

    private List<String> capturedOutputClosings;
    
    private boolean resetOutputsCalled;

    private final TypedDatumService typedDatumService;

    private final Map<Class<?>, Object> services = new HashMap<>();

    private int executionCount;

    private final LogicalNodeId node = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("ComponentContextMock")
        .convertToDefaultLogicalNodeId();

    private ComponentHistoryDataItem historyDataItem;

    private Set<String> inputsNotConnected = new HashSet<>();

    /**
     * Defines a dynamic or static endpoint of the component.
     * 
     * @author Robert Mischke
     * @author Sascha Zur
     */
    private final class SimulatedEndpoint {

        private final String endpointId;

        private final DataType dataType;

        private final boolean isDynamic;

        private final Map<String, String> metaData;

        private final EndpointCharacter endpointCharacter;

        SimulatedEndpoint(String endpointId, DataType dataType, boolean isDynamic, Map<String, String> metaData,
            EndpointCharacter endpointCharacter) {
            this.endpointId = endpointId;
            this.dataType = dataType;
            this.isDynamic = isDynamic;
            if (metaData != null) {
                this.metaData = metaData;
            } else {
                this.metaData = new HashMap<>();
            }
            this.endpointCharacter = endpointCharacter;
        }

        private String getEndpointId() {
            return endpointId;
        }

        private DataType getDataType() {
            return dataType;
        }

        private boolean isDynamic() {
            return isDynamic;
        }

        private Map<String, String> getMetaData() {
            return metaData;
        }

        private EndpointCharacter getEndpointCharacter() {
            return endpointCharacter;
        }

    }

    /**
     * Utility class that automatically creates a List<TypedDatum> for every new output id to contain the captured {@link TypedDatum}s.
     * 
     * @author Robert Mischke
     */
    private final class OutputCaptureMap extends AutoCreationMap<String, List<TypedDatum>> {

        @Override
        protected List<TypedDatum> createNewEntry(String key) {
            return new ArrayList<>();
        }
    }

    public ComponentContextMock() {
        // hard-coded common service
        typedDatumService = new TypedDatumServiceDefaultStub();
        services.put(TypedDatumService.class, typedDatumService);
        // initialize holders
        resetInputData();
        resetOutputData();
    }

    /**
     * Method for adding a needed service.
     * 
     * @param <T> generic parameter
     * @param clazz name of the original service
     * @param serviceStub stub of the service
     */
    public <T> void addService(Class<T> clazz, Object serviceStub) {
        services.put(clazz, serviceStub);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz) {
        return (T) services.get(clazz);
    }

    @Override
    public LogicalNodeId getNodeId() {
        return node;
    }

    @Override
    public String getConfigurationValue(String key) {
        return testConfig.get(key);
    }

    @Override
    public Set<String> getConfigurationKeys() {
        return testConfig.keySet();
    }

    @Override
    public Set<String> getInputs() {
        return new HashSet<>(simulatedInputs.keySet());
    }

    @Override
    public Set<String> getOutputs() {
        return new HashSet<>(simulatedOutputs.keySet());
    }

    @Override
    public void closeAllOutputs() {
        for (String outputName : simulatedOutputs.keySet()) {
            capturedOutputClosings.add(outputName);
        }
    }

    @Override
    public void closeOutput(String outputName) {
        capturedOutputClosings.add(outputName);
    }

    @Override
    public void resetOutputs() {
        resetOutputsCalled = true;
    }

    @Override
    public Set<String> getInputsWithDatum() {
        return new HashSet<>(inputValues.keySet());
    }

    @Override
    public DataType getInputDataType(String inputName) {
        return simulatedInputs.get(inputName).getDataType();
    }

    @Override
    public DataType getOutputDataType(String outputName) {
        return simulatedOutputs.get(outputName).getDataType();
    }

    @Override
    public String getOutputMetaDataValue(String outputName, String metaDataKey) {
        return simulatedOutputs.get(outputName).getMetaData().get(metaDataKey);
    }

    @Override
    public String getInputMetaDataValue(String inputName, String metaDataKey) {
        return simulatedInputs.get(inputName).getMetaData().get(metaDataKey);
    }

    @Override
    public void writeOutput(String outputName, TypedDatum value) {
        if (simulatedOutputs.get(outputName) == null) {
            throw new RuntimeException(StringUtils.format("Output \"%s\" is not defined.", outputName));
        }
        if (value.getDataType() != simulatedOutputs.get(outputName).getDataType() && !value.getDataType().equals(DataType.NotAValue)
            && !typedDatumService.getConverter().isConvertibleTo(value, simulatedOutputs.get(outputName).getDataType())) {
            throw new RuntimeException(StringUtils.format("DataType %s of given value not convertible to defined output DataType %s",
                value.getDataType(), simulatedOutputs.get(outputName).getDataType()));
        }

        capturedOutputValues.get(outputName).add(value);
    }

    @Override
    public boolean isDynamicInput(String inputName) {
        return simulatedInputs.get(inputName).isDynamic();
    }

    @Override
    public String getDynamicOutputIdentifier(String outputName) {
        return simulatedOutputs.get(outputName).getEndpointId();
    }

    @Override
    public String getDynamicInputIdentifier(String inputName) {
        return simulatedInputs.get(inputName).getEndpointId();
    }

    @Override
    public TypedDatum readInput(String key) {
        return inputValues.get(key);
    }

    @Override
    public int getExecutionCount() {
        return executionCount;
    }

    /**
     * Adds a key/value pair to the component's configuration. Should be called only before starting the component.
     * 
     * @param key entry key
     * @param value entry value
     */
    public void setConfigurationValue(String key, String value) {
        testConfig.put(key, value);
    }

    /**
     * Defines/adds a simulated input endpoint for the component. Should be called only before starting the component.
     * 
     * @param name the name of the input (as visible to the user in actual workflows)
     * @param endpointId the internal endpoint ID
     * @param dataType the {@link DataType} of the input
     * @param isDynamic true for a dynamic input, false for a static one
     * @param metaData the metadata map of the intput; can be null as a shortcut for an empty map
     */
    public void addSimulatedInput(String name, String endpointId, DataType dataType, boolean isDynamic, Map<String, String> metaData) {
        simulatedInputs.put(name,
            new SimulatedEndpoint(endpointId, dataType, isDynamic, metaData, EndpointCharacter.SAME_LOOP));
    }

    /**
     * Defines/adds a simulated input endpoint for the component. Should be called only before starting the component.
     * 
     * @param name the name of the input (as visible to the user in actual workflows)
     * @param endpointId the internal endpoint ID
     * @param dataType the {@link DataType} of the input
     * @param isDynamic true for a dynamic input, false for a static one
     * @param metaData the metadata map of the intput; can be null as a shortcut for an empty map
     * @param inputCharacter {@link EndpointCharacter} of the input
     */
    public void addSimulatedInput(String name, String endpointId, DataType dataType, boolean isDynamic, Map<String, String> metaData,
        EndpointCharacter inputCharacter) {
        simulatedInputs.put(name, new SimulatedEndpoint(endpointId, dataType, isDynamic, metaData, inputCharacter));
    }

    /**
     * Defines/adds a simulated output endpoint for the component. Should be called only before starting the component.
     * 
     * @param name the name of the output (as visible to the user in actual workflows)
     * @param endpointId the internal endpoint ID
     * @param dataType the {@link DataType} of the input
     * @param isDynamic true for a dynamic output, false for a static one
     * @param metaData the metadata map of the output; can be null as a shortcut for an empty map
     */
    public void addSimulatedOutput(String name, String endpointId, DataType dataType, boolean isDynamic, Map<String, String> metaData) {
        simulatedOutputs.put(name,
            new SimulatedEndpoint(endpointId, dataType, isDynamic, metaData, EndpointCharacter.SAME_LOOP));
    }

    /**
     * Defines/adds a simulated output endpoint for the component. Should be called only before starting the component.
     * 
     * @param name the name of the output (as visible to the user in actual workflows)
     * @param endpointId the internal endpoint ID
     * @param dataType the {@link DataType} of the input
     * @param isDynamic true for a dynamic output, false for a static one
     * @param metaData the metadata map of the output; can be null as a shortcut for an empty map
     * @param outputCharacter {@link EndpointCharacter} of the input
     */
    public void addSimulatedOutput(String name, String endpointId, DataType dataType, boolean isDynamic, Map<String, String> metaData,
        EndpointCharacter outputCharacter) {
        simulatedOutputs.put(name, new SimulatedEndpoint(endpointId, dataType, isDynamic, metaData, outputCharacter));
    }

    /**
     * Clears all remaining input values and all captured output values.
     * 
     * @deprecated Should not be needed anymore when using ComponentTestWrapper
     */
    @Deprecated
    public void resetEndpointData() {
        resetInputData();
        resetOutputData();
    }

    /**
     * Sets an input value for future consumption by the component. Only one input can be set per input; existing values are overwritten.
     * 
     * @param inputName the name of the input
     * @param datum the TypedDatum to "send"
     */
    public void setInputValue(String inputName, TypedDatum datum) {
        inputValues.put(inputName, datum);
    }

    /**
     * Returns a list of all captured {@link TypedDatum}s sent to the given output by the component. If no output was generated, an empty
     * list is returned.
     * 
     * @param outputName the name if the output
     * @return a list of the captured outputs; may be empty, but not null
     */
    public List<TypedDatum> getCapturedOutput(String outputName) {
        return capturedOutputValues.get(outputName);
    }

    /**
     * Returns a list of all output names, which were closed. If no output was closed, an empty list is returned.
     * 
     * @return a list of the captured outputs, which were closed; may be empty, but not <code>null</code>
     */
    public List<String> getCapturedOutputClosings() {
        return capturedOutputClosings;
    }

    /**
     * @return Returns <code>true</code> if a reset was send out of the component's outputs.
     */
    public boolean isResetOutputsCalled() {
        return resetOutputsCalled;
    }

    /**
     * Clears all defined input data; automatically called by {@link ComponentTestWrapper}.
     */
    protected void resetInputData() {
        inputValues = new HashMap<>();
    }

    /**
     * Clears all captured output data; automatically called by {@link ComponentTestWrapper}.
     */
    protected void resetOutputData() {
        capturedOutputValues = new OutputCaptureMap();
    }

    /**
     * Clears all captured output closings; automatically called by {@link ComponentTestWrapper}.
     */
    protected void resetOutputClosings() {
        capturedOutputClosings = new ArrayList<>();
    }

    /**
     * Clears all captured output resets; automatically called by {@link ComponentTestWrapper}.
     */
    protected void resetOutputResets() {
        resetOutputsCalled = false;
    }

    /**
     * Increments the execution counter; automatically called by {@link ComponentTestWrapper}.
     */
    protected void incrementExecutionCount() {
        executionCount++;
    }

    @Override
    public void writeFinalHistoryDataItem(ComponentHistoryDataItem newHistoryDataItem) {
        this.historyDataItem = newHistoryDataItem;
    }

    public ComponentHistoryDataItem getHistoryDataItem() {
        return historyDataItem;
    }

    @Override
    public boolean isDynamicOutput(String outputName) {
        return simulatedOutputs.get(outputName).isDynamic();
    }

    @Override
    public Set<String> getInputsNotConnected() {
        return inputsNotConnected;
    }

    @Override
    public EndpointCharacter getInputCharacter(String inputName) {
        return simulatedInputs.get(inputName).getEndpointCharacter();
    }

    @Override
    public EndpointCharacter getOutputCharacter(String outputName) {
        return simulatedOutputs.get(outputName).getEndpointCharacter();
    }

    /**
     * Adds an input that should not be connected.
     * 
     * @param name of input
     */
    public void addInputNotConnected(String name) {
        inputsNotConnected.add(name);
    }

    @Override
    public List<String> getDynamicInputsWithIdentifier(String identifier) {
        List<String> result = new LinkedList<>();
        for (String endpoint : simulatedInputs.keySet()) {
            if (isDynamicInput(endpoint) && getDynamicInputIdentifier(endpoint).equals(identifier)) {
                result.add(endpoint);
            }
        }
        return result;
    }
}
