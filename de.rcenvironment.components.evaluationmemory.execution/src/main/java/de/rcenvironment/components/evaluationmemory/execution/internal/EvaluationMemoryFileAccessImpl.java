/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Logger;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentLog;
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
    
    private static final List<DataType> ALWAYS_VALID_OUTPUT_DATATYPES = new ArrayList<>();

    private final File evalMemoryFile;

    private TypedDatumSerializer typedDatumSerializer;

    private ComponentLog componentLog;
    
    static {
        ALWAYS_VALID_OUTPUT_DATATYPES.add(DataType.NotAValue);
    }
    
    public EvaluationMemoryFileAccessImpl(String memoryFilePath) {
        evalMemoryFile = new File(memoryFilePath);
    }
    
    @Override
    public synchronized void setInputsOutputsDefinition(SortedMap<String, DataType> inputs, SortedMap<String, DataType> outputs) 
        throws IOException {
        EvaluationMemoryProperties evalMemory = loadEvaluationMemory();
        addInputsDefinition(inputs, evalMemory);
        addOutputsDefinition(outputs, evalMemory);
        
        storeEvaluationMemory(evalMemory);
    }
    
    @Override
    public synchronized void addEvaluationValues(SortedMap<String, TypedDatum> inputValues, 
        SortedMap<String, TypedDatum> outputValues) throws IOException {
        
        EvaluationMemoryProperties evalMemory = loadEvaluationMemory();
        
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
    
    private void storeEvaluationMemory(EvaluationMemoryProperties evalMemory) throws IOException {
        evalMemory.setVersion(VERSION_NUMBER);
        evalMemory.setType(EvaluationMemoryComponentConstants.COMPONENT_ID);
        try (FileOutputStream memoryFileOutputStream = new FileOutputStream(evalMemoryFile)) {
            evalMemory.store(memoryFileOutputStream, null);
        }
    }
    
    @Override
    public synchronized SortedMap<String, TypedDatum> getEvaluationResult(SortedMap<String, TypedDatum> inputValues,
        SortedMap<String, DataType> outputs, SortedMap<String, Double> tolerances, ToleranceHandling toleranceHandling) throws IOException {
        
        EvaluationMemoryProperties evalMemory = loadEvaluationMemory();
        
        validateInputs(evalMemory, getEndpoints(inputValues));
        validateOutputs(evalMemory, outputs);
        
        // An exact match of stored results always takes precedence over inputs in the tolerance interval, hence we first check for an exact
        // match
        final SortedMap<String, TypedDatum> exactStoredResult = tryGetStoredResults(inputValues, outputs, evalMemory);
        if (exactStoredResult != null) {
            return exactStoredResult;
        }

        // At this point, we know that we have not found an exact match for the input values, so we delegate to the tolerance handling to
        // figure out whether we have stored tolerated input values. Since we have no further fallback, we directly return the result
        // instead of checking it for null in-between.
        return tryGetToleratedStoredResults(inputValues, outputs.keySet(), tolerances, evalMemory, toleranceHandling);
    }

    /**
     * Checks whether the exact given inputs have already been stored in the given property file and returns them if this is the case.
     * 
     * @param inputValues The input values to be checked for existing cached output values
     * @param outputs The labels of the outputs that we check for being stored
     * @param evalMemory The file containing the stored input/output relations
     * @return A map of stored output values for the given inputs, if there exists an exact match in the eval memory. Null otherwise.
     */
    private SortedMap<String, TypedDatum> tryGetStoredResults(SortedMap<String, TypedDatum> inputValues,
        SortedMap<String, DataType> outputs, Properties evalMemory) {
        String evalMemoryKey = createEvaluationMemoryKeyForInputValues(inputValues);
        if (evalMemory.containsKey(evalMemoryKey)) {
            final String evaluationResult = evalMemory.getProperty(evalMemoryKey);
            return splitDeserializeAndZip(outputs.keySet(), evaluationResult);
        } else {
            return null;
        }
    }

    private SortedMap<String, TypedDatum> tryGetToleratedStoredResults(SortedMap<String, TypedDatum> inputValues,
        Set<String> outputs, Map<String, Double> tolerances, EvaluationMemoryProperties evalMemory,
        ToleranceHandling toleranceHandling) {
        final Set<String> inputLabels = inputValues.keySet();
        final Iterable<String> recordKeys = evalMemory.getRecordKeys();
        final Predicate<SortedMap<String, TypedDatum>> isTolerated =
            stored -> toleranceHandling.isInToleranceInterval(inputValues, tolerances, stored);
        final Collection<SortedMap<String, TypedDatum>> toleratedStoredInputs =
            collectToleratedInputs(inputLabels, recordKeys, isTolerated);
        
        final SortedMap<String, TypedDatum> mostToleratedStoredInput =
            toleranceHandling.pickMostToleratedInputs(toleratedStoredInputs, inputValues);

        if (mostToleratedStoredInput == null) {
            return null;
        } else {
            // Review: This should be logged further up in the component logger in order to be shown to the user
            final String infoMessage =
                String.format("Found evaluation results for values '%s' that are within tolerance intervals of actual values '%s'",
                    mostToleratedStoredInput, inputValues);
            Logger.getLogger(this.getClass().getCanonicalName()).info(infoMessage);

            final String lookupKey = createEvaluationMemoryKeyForInputValues(mostToleratedStoredInput);
            return splitDeserializeAndZip(outputs, evalMemory.getProperty(lookupKey));
        }
    }

    private Collection<SortedMap<String, TypedDatum>> collectToleratedInputs(final Set<String> inputLabels,
        final Iterable<String> recordKeys, final Predicate<SortedMap<String, TypedDatum>> isTolerated) {
        final Collection<SortedMap<String, TypedDatum>> candidateStoredInputs = new HashSet<>();
        for (String inputString : recordKeys) {
            final SortedMap<String, TypedDatum> potentialCandidateInput = splitDeserializeAndZip(inputLabels, inputString);
            if (isTolerated.test(potentialCandidateInput)) {
                candidateStoredInputs.add(potentialCandidateInput);
            }
        }
        return candidateStoredInputs;
    }

    /**
     * @param keySet The (ordered) set of keys. Must have as many entries as there are values encoded in inputString
     * @param inputString A string containing the serialized and joined representation of values for the inputs/outputs given in keySet
     * @return A map assigning to each input/output in keySet the value encoded in inputString
     */
    private SortedMap<String, TypedDatum> splitDeserializeAndZip(final Set<String> keySet, final String inputString) {
        String[] serializedInputs = StringUtils.splitAndUnescape(inputString);
        final SortedMap<String, TypedDatum> potentialCandidateInput = new TreeMap<>();
        int i = 0;
        for (String input : keySet) {
            potentialCandidateInput.put(input, typedDatumSerializer.deserialize(serializedInputs[i++]));
        }
        return potentialCandidateInput;
    }

    @Override
    public synchronized void validateEvaluationMemory(SortedMap<String, DataType> inputs, SortedMap<String, DataType> outputs)
        throws IOException {
        EvaluationMemoryProperties evalMemory = loadEvaluationMemory();
        validateVersionAndType(evalMemory);
        validateInputs(evalMemory, inputs);
        validateOutputs(evalMemory, outputs);
        validateEvaluationMemoryEntries(inputs, outputs, evalMemory);
    }
    
    private void addInputsDefinition(SortedMap<String, DataType> inputs, EvaluationMemoryProperties evalMemory) {
        evalMemory.setInputSpecification(createEndpointDefinitionEntry(inputs));
    }

    private void addOutputsDefinition(SortedMap<String, DataType> outputs, EvaluationMemoryProperties evalMemory) {
        evalMemory.setOutputSpecification(createEndpointDefinitionEntry(outputs));
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
        EvaluationMemoryProperties evalMemory) throws IOException {
        for (String key : evalMemory.getRecordKeys()) {
            validateEvaluationMemoryEntry(inputs, key);
            validateEvaluationMemoryEntry(outputs, evalMemory.getProperty((String) key));
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
    
    private void validateVersionAndType(EvaluationMemoryProperties evalMemory) throws IOException {
        if (evalMemory.getVersion() == null) {
            throw new IOException("Version information is missing");
        }
        if (!evalMemory.getVersion().equals(VERSION_NUMBER)) {
            throw new IOException(StringUtils.format("Version '%s' not supported; expected version: %s", 
                evalMemory.getVersion(), VERSION_NUMBER));
        }
        if (evalMemory.getType() == null) {
            throw new IOException("Type information is missing");
        }
        if (!evalMemory.getType().equals(EvaluationMemoryComponentConstants.COMPONENT_ID)) {
            throw new IOException(StringUtils.format("Type '%s' not supported; expected type: %s",
                evalMemory.getType(), EvaluationMemoryComponentConstants.COMPONENT_ID));
        }
    }
    
    private void validateInputs(EvaluationMemoryProperties evalMemory, Map<String, DataType> inputs) throws IOException {
        validateEndpoints(getEndpoints(evalMemory, evalMemory.getInputSpecificationKey()), inputs, true);
    }
    
    private void validateOutputs(EvaluationMemoryProperties evalMemory, Map<String, DataType> outputs) throws IOException {
        validateEndpoints(getEndpoints(evalMemory, evalMemory.getOutputSpecificationKey()), outputs, false);
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

    private void validateEndpoints(Map<String, DataType> endpointsExpected, Map<String, DataType> actualEndpoints, boolean inputs)
        throws IOException {
        if (!areEndpointsEqual(endpointsExpected, actualEndpoints, inputs)) {
            throw new IOException(StringUtils.format("Input(s)/output(s) don't match input(s)/output(s) in evaluation memory file"
                + " - expected: %s actual: %s", endpointsExpected, actualEndpoints));
        }
    }
    
    private boolean areEndpointsEqual(Map<String, DataType> endpointsExpected, Map<String, DataType> actualEndpoints, boolean isInput) {
        final boolean sameSize = endpointsExpected.size() == actualEndpoints.size();
        if (!sameSize) {
            return false;
        }

        for (Entry<String, DataType> endpointEntry : actualEndpoints.entrySet()) {
            final DataType endpointType = endpointEntry.getValue();
            final boolean endpointValidByDefault = !isInput && ALWAYS_VALID_OUTPUT_DATATYPES.contains(endpointType);
            if (endpointValidByDefault) {
                continue;
            }

            final String endpointLabel = endpointEntry.getKey();
            final boolean endpointIsExpected = endpointsExpected.containsKey(endpointLabel);
            if (!endpointIsExpected) {
                return false;
            }

            final DataType expectedType = endpointsExpected.get(endpointLabel);
            final boolean endpointIsEqual = expectedType.equals(endpointType);
            if (!endpointIsEqual) {
                return false;
            }
        }

        return true;
    }
    
    private EvaluationMemoryProperties loadEvaluationMemory() throws IOException {
        EvaluationMemoryProperties evalMemory = new EvaluationMemoryProperties();
        if (!evalMemoryFile.exists()) {
            throw new FileNotFoundException(
                "Evaluation memory file not found; either deleted or not created due to invalid file name");
        }
        try (FileInputStream memFileInputStream = new FileInputStream(evalMemoryFile)) {
            evalMemory.load(memFileInputStream);
        }
        return evalMemory;
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
