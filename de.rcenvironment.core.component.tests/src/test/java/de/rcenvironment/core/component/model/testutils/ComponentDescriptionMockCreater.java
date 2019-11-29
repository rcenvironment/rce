/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.testutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * 
 * Utility Class for tests regarding validators.
 *
 * @author Jascha Riedel
 */
public class ComponentDescriptionMockCreater {

    private static final String IS_CONNECTED = "is_connected";

    private static final String DYNAMIC_ENDPOINT_IDENTIFIER = "dynamic_endpoint_0eb99b7eff1d";

    private final List<String> inputNames;

    private final List<DataType> inputDataTypes;

    private final List<Map<String, String>> inputsMetaData;

    private final List<String> outputNames;

    private final List<DataType> outputDataTypes;

    private final List<Map<String, String>> outputsMetaData;

    private final Map<String, String> configurationMap;

    public ComponentDescriptionMockCreater() {
        inputNames = new ArrayList<>();
        inputDataTypes = new ArrayList<>();
        inputsMetaData = new ArrayList<>();
        outputNames = new ArrayList<>();
        outputDataTypes = new ArrayList<>();
        outputsMetaData = new ArrayList<>();
        configurationMap = new HashMap<>();
    }

    /**
     * 
     * Adds a simulated Input.
     * 
     * @param name of the input
     * @param dataType of the input
     * @param metaData of the input
     * @param executionConstraint of that input
     * @param isConnected whether the input is connected or not.
     */
    public void addSimulatedInput(String name, DataType dataType, Map<String, String> metaData,
        EndpointDefinition.InputExecutionContraint executionConstraint, boolean isConnected) {
        inputNames.add(name);
        inputDataTypes.add(dataType);
        inputsMetaData.add(metaData);
        inputsMetaData.get(inputsMetaData.size() - 1).put(IS_CONNECTED, Boolean.toString(isConnected));
        inputsMetaData.get(inputsMetaData.size() - 1).put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
            executionConstraint.toString());
    }

    /**
     * 
     * 
     * @param inputName where the meta data should be added.
     * @param key of the meta data value
     * @param value the meta data
     */
    public void addMetaDataToInput(String inputName, String key, String value) {
        int index = inputNames.indexOf(inputName);
        if (index >= 0) {
            inputsMetaData.get(index).put(key, value);
        }
    }

    /**
     * 
     * Adds a simulated Output.
     * 
     * @param name of the output
     * @param dataType of the output
     * @param metaData of the output
     * @param isConnected whether the output is connected or not
     */
    public void addSimulatedOutput(String name, DataType dataType, Map<String, String> metaData, boolean isConnected) {
        outputNames.add(name);
        outputDataTypes.add(dataType);
        outputsMetaData.add(metaData);
        outputsMetaData.get(outputsMetaData.size() - 1).put(IS_CONNECTED, Boolean.toString(isConnected));
    }

    /**
     * 
     * Adds a simulated Output.
     * 
     * @param name of the output
     * @param dataType of the output
     * @param metaData of the output
     * @param isConnected whether the output is connected or not
     * @param dynamicEndpointIdentifier of the endpoint
     */
    public void addSimulatedOutput(String name, DataType dataType, Map<String, String> metaData, boolean isConnected,
        String dynamicEndpointIdentifier) {
        outputNames.add(name);
        outputDataTypes.add(dataType);
        outputsMetaData.add(metaData);
        outputsMetaData.get(outputsMetaData.size() - 1).put(IS_CONNECTED, Boolean.toString(isConnected));
        outputsMetaData.get(outputsMetaData.size() - 1).put(DYNAMIC_ENDPOINT_IDENTIFIER, dynamicEndpointIdentifier);
    }

    /**
     * 
     * 
     * @param outputName where the meta data should be added.
     * @param key of the meta data value
     * @param value the meta data
     */
    public void addMetaDataToOutput(String outputName, String key, String value) {
        int index = inputNames.indexOf(outputName);
        if (index >= 0) {
            outputsMetaData.get(index).put(key, value);
        }
    }

    /**
     * 
     * Adds a configuration value.
     * 
     * @param property of the configuration value.
     * @param value of the property.
     */
    public void addConfigurationValue(String property, String value) {
        configurationMap.put(property, value);
    }

    /**
     * 
     * Creates a componentDescription Mock for the current configuration provided.
     * 
     * @return a {@link ComponentDescription} usable for validator tests.
     */
    public ComponentDescription createComponentDescriptionMock() {

        ComponentDescription componentDescription;

        componentDescription = EasyMock.createNiceMock(ComponentDescription.class);

        addConfigurationMock(componentDescription);

        EasyMock.expect(componentDescription.getInputDescriptionsManager())
            .andStubReturn(createIOManagerMock(inputNames, inputDataTypes, inputsMetaData));

        EasyMock.expect(componentDescription.getOutputDescriptionsManager())
            .andStubReturn(createIOManagerMock(outputNames, outputDataTypes, outputsMetaData));

        EasyMock.replay(componentDescription);
        return componentDescription;
    }

    private void addConfigurationMock(ComponentDescription componentDescription) {
        ConfigurationDescription configurationDescription = EasyMock.createStrictMock(ConfigurationDescription.class);
        EasyMock.expect(configurationDescription.getConfigurationValue(EasyMock.anyObject(String.class))).andAnswer(new IAnswer<String>() {

            @Override
            public String answer() throws Throwable {
                return configurationMap.get(EasyMock.getCurrentArguments()[0]);
            }

        }).anyTimes();

        EasyMock.expect(componentDescription.getConfigurationDescription()).andReturn(configurationDescription).anyTimes();
        EasyMock.replay(configurationDescription);
    }

    private EndpointDescriptionsManager createIOManagerMock(List<String> names,
        List<DataType> dataTypes, final List<Map<String, String>> metaDatas) {
        Set<EndpointDescription> endpointDescriptions = new HashSet<EndpointDescription>();

        for (int index = 0; index < names.size(); index++) {
            final int i = index;

            EndpointDefinition endpointDefinition = EasyMock.createStrictMock(EndpointDefinition.class);
            if (metaDatas.get(i).containsKey(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                EasyMock.expect(endpointDefinition.getDefaultInputExecutionConstraint())
                    .andStubReturn(
                        InputExecutionContraint
                            .valueOf(metaDatas.get(i).get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)));
            }
            EndpointDescription endpointDescription = EasyMock.createStrictMock(EndpointDescription.class);
            EasyMock.expect(endpointDescription.getMetaDataValue(EasyMock.anyObject(String.class))).andStubAnswer(new IAnswer<String>() {

                @Override
                public String answer() throws Throwable {
                    return metaDatas.get(i).get(EasyMock.getCurrentArguments()[0]);
                }

            });
            EasyMock.expect(endpointDescription.getEndpointDefinition()).andStubReturn(endpointDefinition);
            EasyMock.expect(endpointDescription.getDataType()).andStubReturn(dataTypes.get(i));
            EasyMock.expect(endpointDescription.isConnected()).andStubReturn(Boolean.valueOf(metaDatas.get(i).get(IS_CONNECTED)));
            EasyMock.expect(endpointDescription.getMetaData()).andStubReturn(metaDatas.get(i));
            EasyMock.expect(endpointDescription.getName()).andStubReturn(names.get(i));
            EasyMock.expect(endpointDescription.getDynamicEndpointIdentifier())
                .andStubReturn(metaDatas.get(i).get(DYNAMIC_ENDPOINT_IDENTIFIER));
            endpointDescriptions.add(endpointDescription);
            EasyMock.replay(endpointDefinition, endpointDescription);
        }

        EndpointDescriptionsManager ioManager = EasyMock.createStrictMock(EndpointDescriptionsManager.class);
        EasyMock.expect(ioManager.getEndpointDescriptions()).andStubReturn(endpointDescriptions);

        EasyMock.replay(ioManager);

        return ioManager;
    }

}
