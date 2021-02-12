/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.internal;

import java.io.IOException;
import java.util.SortedMap;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Reads and writes tuples from/to the memory file.
 * 
 * @author Doreen Seider
 */
public interface EvaluationMemoryAccess {

    /**
     * Adds a evaluation values to the memory file.
     * 
     * @param inputValues values to evaluate
     * @param outputValues evaluation results
     * @throws IOException if writing values to the memory file failed or values don't match the ones in the memory file
     */
    void addEvaluationValues(SortedMap<String, TypedDatum> inputValues, 
        SortedMap<String, TypedDatum> outputValues) throws IOException;
    
    /**
     * Reads a tuple from the memory file by the given key.
     * 
     * @param inputValues values to evaluate
     * @param outputs outputs for evaluation results
     * @param tolerances The tolerances specifying the tolerance intervals around the inputs
     * @param toleranceHandling an object specifying how tolerance intervals around input values should be handled
     * @return evaluation results if they have been stored, <code>null</code> if none exist for the given values
     * @throws IOException if reading values from the memory file failed or values don't match the ones in the memory file
     */
    SortedMap<String, TypedDatum> getEvaluationResult(SortedMap<String, TypedDatum> inputValues,
        SortedMap<String, DataType> outputs, SortedMap<String, Double> tolerances, ToleranceHandling toleranceHandling) throws IOException;

    /**
     * 
     * @param inputs inputs (name, data type) 
     * @param outputs outputs (name, data type)
     * @throws IOException if setting definitions failed
     */
    void setInputsOutputsDefinition(SortedMap<String, DataType> inputs, SortedMap<String, DataType> outputs) throws IOException;

    /**
     * Validates a evaluation memory file: Have all of the key and tuples the same size. Do they match the inputs and outputs definition in
     * the file.
     * 
     * @param inputs inputs (name, data type) expected
     * @param outputs outputs (name, data type) expected
     * @throws IOException if validation fails
     */
    void validateEvaluationMemory(SortedMap<String, DataType> inputs, SortedMap<String, DataType> outputs) throws IOException;
}
