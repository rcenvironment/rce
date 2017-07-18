/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default implementation of {@link EvaluationMemoryAccess}.
 * 
 * @author Doreen Seider
 */
public class EvaluationMemoryFileAccessImpl implements EvaluationMemoryAccess {

    private static final String VERSION_NUMBER = "1";

    private static final String TYPE = "type";
    
    private static final String VERSION = "version";
    
    private static final String OUTPUTS = "outputs";

    private static final String INPUTS = "inputs";
    
    private static final List<DataType> ALWAYS_VALID_OUTPUT_DATATYPES = new ArrayList<>();

    private final File evalMemoryFile;

    private TypedDatumSerializer typedDatumSerializer;
    
    static {
        ALWAYS_VALID_OUTPUT_DATATYPES.add(DataType.NotAValue);
    }
    
    public EvaluationMemoryFileAccessImpl(String memoryFilePath) {
        evalMemoryFile = new File(memoryFilePath);
    }
    
    @Override
    public synchronized void setInputsOutputsDefinition(SortedMap<String, DataType> inputs, SortedMap<String, DataType> outputs) 
        throws IOException {
        Properties evalMemory = loadEvaluationMemory();
        addInputsDefinition(inputs, evalMemory);
        addOutputsDefinition(outputs, evalMemory);
        
        storeEvaluationMemory(evalMemory);
    }
    
    @Override
    public synchronized void addEvaluationValues(SortedMap<String, TypedDatum> inputValues, 
        SortedMap<String, TypedDatum> outputValues) throws IOException {
        
        Properties evalMemory = loadEvaluationMemory();
        
        validateInputs(evalMemory, getEndpoints(inputValues));
        validateOutputs(evalMemory, getEndpoints(outputValues));
        
        String evalMemoryKey = createEvaluationMemoryKeyForInputValues(inputValues);
        List<String> evalMemoryValues = new ArrayList<>();    
        for (TypedDatum value : outputValues.values()) {
            evalMemoryValues.add(typedDatumSerializer.serialize(value));
        }
        evalMemory.put(evalMemoryKey, StringUtils.escapeAndConcat(evalMemoryValues));
        
        storeEvaluationMemory(evalMemory);
    }
    
    private void storeEvaluationMemory(Properties evalMemory) throws IOException {
        evalMemory.put(VERSION, VERSION_NUMBER);
        evalMemory.put(TYPE, EvaluationMemoryComponentConstants.COMPONENT_ID);
        try (FileOutputStream memoryFileOutputStream = new FileOutputStream(evalMemoryFile)) {
            evalMemory.store(memoryFileOutputStream, null);
        }
    }
    
    @Override
    public synchronized SortedMap<String, TypedDatum> getEvaluationResult(SortedMap<String, TypedDatum> inputValues,
        SortedMap<String, DataType> outputs) throws IOException {
        
        Properties evalMemory = loadEvaluationMemory();
        
        validateInputs(evalMemory, getEndpoints(inputValues));
        validateOutputs(evalMemory, outputs);
        
        SortedMap<String, TypedDatum> outputValues = null;
        String evalMemoryKey = createEvaluationMemoryKeyForInputValues(inputValues);
        if (evalMemory.containsKey(evalMemoryKey)) {
            String[] serializedEvaluationResult = StringUtils.splitAndUnescape(evalMemory.getProperty(evalMemoryKey));
            outputValues = new TreeMap<>();
            int i = 0;
            for (String output : outputs.keySet()) {
                outputValues.put(output, typedDatumSerializer.deserialize(serializedEvaluationResult[i++]));
            }
        }
        return outputValues;
    }
    
    @Override
    public synchronized void validateEvaluationMemory(SortedMap<String, DataType> inputs, SortedMap<String, DataType> outputs)
        throws IOException {
        Properties evalMemory = loadEvaluationMemory();
        validateVersionAndType(evalMemory);
        validateInputs(evalMemory, inputs);
        validateOutputs(evalMemory, outputs);
        validateEvaluationMemoryEntries(inputs, outputs, evalMemory);
    }
    
    private void addInputsDefinition(SortedMap<String, DataType> inputs, Properties evalMemory) {
        evalMemory.put(INPUTS, createEndpointDefinitionEntry(inputs));
    }

    private void addOutputsDefinition(SortedMap<String, DataType> outputs, Properties evalMemory) {
        evalMemory.put(OUTPUTS, createEndpointDefinitionEntry(outputs));
    }

    private String createEndpointDefinitionEntry(Map<String, DataType> endpoints) {
        List<String> parts = new ArrayList<>();
        for (Entry<String, DataType> outputEntry: endpoints.entrySet()) {
            parts.add(outputEntry.getKey());
            parts.add(outputEntry.getValue().name());
        }
        return StringUtils.escapeAndConcat(parts);
    }
    
    private void validateEvaluationMemoryEntries(SortedMap<String, DataType> inputs, SortedMap<String, DataType> outputs,
        Properties evalMemory) throws IOException {
        for (Object key : evalMemory.keySet()) {
            if (!key.equals(INPUTS) && !key.equals(OUTPUTS) && !key.equals(VERSION) && !key.equals(TYPE)) {
                validateEvaluationMemoryEntry(inputs, (String) key);
                validateEvaluationMemoryEntry(outputs, evalMemory.getProperty((String) key));
            }
        }
    }
    
    private void validateEvaluationMemoryEntry(SortedMap<String, DataType> endpoints, String evalMemoryEntry) throws IOException {
        String[] typedDatumParts = StringUtils.splitAndUnescape(evalMemoryEntry);
        List<TypedDatum> typedDatums = new ArrayList<>();
        for (String value : typedDatumParts) {
            try {
                typedDatums.add(typedDatumSerializer.deserialize(value));
            } catch (IllegalArgumentException e) {
                throw new IOException("Failed to read values from evaluation memory file", e);
            }
        }
        if (typedDatums.size() != endpoints.size()) {
            throwIOException(endpoints, typedDatums);
        }
        int i = 0;
        for (DataType dataType : endpoints.values()) {
            if (ALWAYS_VALID_OUTPUT_DATATYPES.contains(typedDatums.get(i).getDataType())) {
                continue;
            }
            if (dataType != typedDatums.get(i).getDataType()) {
                throwIOException(endpoints, typedDatums);
            }
            i++;
        }
    }
    
    private void throwIOException(SortedMap<String, DataType> endpoints, List<TypedDatum> tuple) throws IOException {
        throw new IOException(StringUtils.format("Input/output data type(s) don't match input/output data type(s) "
            + "in evaluation memory file - expected: %s actual: %s",
            endpoints, tuple));
    }
    
    private void validateVersionAndType(Properties evalMemory) throws IOException {
        if (evalMemory.getProperty(VERSION) == null) {
            throw new IOException("Version information is missing");
        }
        if (!evalMemory.getProperty(VERSION).equals(VERSION_NUMBER)) {
            throw new IOException(StringUtils.format("Version '%s' not supported; expected version: %s", 
                evalMemory.getProperty(VERSION), VERSION_NUMBER));
        }
        if (evalMemory.getProperty(TYPE) == null) {
            throw new IOException("Type information is missing");
        }
        if (!evalMemory.getProperty(TYPE).equals(EvaluationMemoryComponentConstants.COMPONENT_ID)) {
            throw new IOException(StringUtils.format("Type '%s' not supported; expected type: %s",
                evalMemory.getProperty(TYPE), EvaluationMemoryComponentConstants.COMPONENT_ID));
        }
    }
    
    private void validateInputs(Properties evalMemory, Map<String, DataType> inputs) throws IOException {
        validateEndoints(getEndpoints(evalMemory, INPUTS), inputs, true);
    }
    
    private void validateOutputs(Properties evalMemory, Map<String, DataType> outputs) throws IOException {
        validateEndoints(getEndpoints(evalMemory, OUTPUTS), outputs, false);
    }
    
    private void validateEndoints(Map<String, DataType> endpointsExpected, Map<String, DataType> actualEndpoints, boolean inputs)
        throws IOException {
        if (!areEndpointsEqual(endpointsExpected, actualEndpoints, inputs)) {
            throw new IOException(StringUtils.format("Input(s)/output(s) don't match input(s)/output(s) in evaluation memory file"
                + " - expected: %s actual: %s", endpointsExpected, actualEndpoints));
        }
    }
    
    private boolean areEndpointsEqual(Map<String, DataType> endpointsExpected, Map<String, DataType> actualEndpoints, boolean inputs) {
        boolean equals = endpointsExpected.size() == actualEndpoints.size();
        if (equals) {
            for (Entry<String, DataType> endpointEntry : actualEndpoints.entrySet()) {
                if (!inputs && ALWAYS_VALID_OUTPUT_DATATYPES.contains(endpointEntry.getValue())) {
                    continue;
                }
                if (!endpointsExpected.containsKey(endpointEntry.getKey())
                    || !endpointsExpected.get(endpointEntry.getKey()).equals(endpointEntry.getValue())) {
                    equals = false;
                    break;                        
                }
            }            
        }
        return equals;
    }
    
    private Properties loadEvaluationMemory() throws IOException {
        Properties evalMemory = new Properties();
        if (!evalMemoryFile.exists()) {
            throw new FileNotFoundException(
                "Evaluation memory file not found; either deleted or not created due to invalid file name");
        }
        try (FileInputStream memFileInputStream = new FileInputStream(evalMemoryFile)) {
            evalMemory.load(memFileInputStream);
        }
        return evalMemory;
    }
    
    private Map<String, DataType> getEndpoints(Properties evalMemory, String key) throws IOException {
        String endpointsEntry = evalMemory.getProperty(key);
        if (endpointsEntry == null) {
            throw new IOException(StringUtils.format("'%s' definition is missing in evaluation memory file: %s (it is required to ensure"
                + " correct evaluation memory handling, is written by the component, and must not be removed)",
                key, evalMemoryFile));
        }
        
        String[] parts = StringUtils.splitAndUnescape(endpointsEntry);
        Map<String, DataType> endpoints = new HashMap<>();
        for (int i = 0; i < parts.length; i = i + 2) {
            endpoints.put(parts[i], DataType.valueOf(parts[i + 1]));
        }
        return endpoints;
    }
    
    private SortedMap<String, DataType> getEndpoints(SortedMap<String, TypedDatum> endpoints) {
        SortedMap<String, DataType> endpointsWithDataType = new TreeMap<>();
        for (Entry<String, TypedDatum> endpointEntry : endpoints.entrySet()) {
            endpointsWithDataType.put(endpointEntry.getKey(), endpointEntry.getValue().getDataType());
        }
        return endpointsWithDataType;
    }
    
    private String createEvaluationMemoryKeyForInputValues(SortedMap<String, TypedDatum> valuesToEvaluate) {
        List<String> serializedValues = new ArrayList<>();
        Iterator<TypedDatum> valuesIterator = valuesToEvaluate.values().iterator();
        while (valuesIterator.hasNext()) {
            serializedValues.add(typedDatumSerializer.serialize(valuesIterator.next()));
        }
        return StringUtils.escapeAndConcat(serializedValues);
    }
    
    protected void setTypedDatumSerializer(TypedDatumSerializer serializer) {
        this.typedDatumSerializer = serializer;
    }
    
}
