/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.evaluationmemory.execution.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;

/**
 * 
 * This class encapsulates all behavior related to tolerance intervals for the Evaluation Memory Component, i.e., it provides methods
 * answering the following two questions:
 * 
 * 1) Given a vector of inputs, a vector of tolerances for these inputs, and a vector of stored inputs for which we already know the
 * resulting output values, do the stored inputs lie in the tolerance interval around the (actual) inputs?
 * 
 * 2) Given a vector of actual inputs and a set of input vectors for which we already know the resulting outputs and which we know to be in
 * the tolerance interval around the given actual inputs, which of these ``candidate'' input vectors shall we use to represent the actual
 * input, i.e., the results associated with which stored input vector shall we return from the evaluation memory component?
 * 
 * As of now, we only support a single strategy for answering the first question, namely checking the vector of inputs pointwise, and only
 * two strategies for answering the second one, namely either we only pick a single vector if there only exists a single one (strict overlap
 * handling), or we pick an arbitrary one from among the candidates (lenient overlap handling).
 * 
 * The class ToleranceHandling itself only implements the single strategy for answering the first question. In the future, if we support
 * more intricate algorithms for answering that question, this may be delegated as well. The second question is answered by subclasses
 * implementing ``pickMostToleratedInputs''. This allows for easy extension in the future by adding more subclasses.
 *
 * @author Alexander Weinert
 */

public abstract class ToleranceHandling {

    /**
     * Lenient overlap handling strategy: If there are multiple overlapping possible input vectors, return an arbitrary one.
     * 
     * @author Alexander Weinert
     */
    static final class LenientToleranceHandling extends ToleranceHandling {

        LenientToleranceHandling(ComponentLog log) {
            super(log);
        }

        @Override
        public SortedMap<String, TypedDatum> choiceAlgorithm(Collection<SortedMap<String, TypedDatum>> candidates,
            SortedMap<String, TypedDatum> inputValue) {
            final Iterator<SortedMap<String, TypedDatum>> iterator = candidates.iterator();
            if (iterator.hasNext()) {
                return candidates.iterator().next();
            } else {
                return null;
            }
        }
    }

    /**
     * Strict overlap handling strategy: If there is exactly one possible input vector, return that unique one. If there is more than one,
     * return none, thus forcing re-evaluation.
     * 
     * @author Alexander Weinert
     */
    static final class StrictToleranceHandling extends ToleranceHandling {

        StrictToleranceHandling(final ComponentLog log) {
            super(log);
        }

        @Override
        public SortedMap<String, TypedDatum> choiceAlgorithm(Collection<SortedMap<String, TypedDatum>> candidates,
            SortedMap<String, TypedDatum> inputValue) {
            if (candidates.size() == 1) {
                return candidates.iterator().next();
            } else {
                return null;
            }
        }

    }

    private final ComponentLog log;
    
    private ToleranceHandling(ComponentLog log) {
        this.log = log;
    }

    /**
     * Factory method.
     * 
     * @param log The logging object of the enclosing EvaluationMemoryComponent
     * @return A ToleranceHandling object implementing the lenient overlap handling strategy.
     */
    public static ToleranceHandling constructLenientHandling(final ComponentLog log) {
        return new LenientToleranceHandling(log);
    }

    /**
     * Factory method.
     * 
     * @param log The logging object of the enclosing EvaluationMemoryComponent
     * @return A ToleranceHandling object implementing the strict overlap handling strategy.
     */
    public static ToleranceHandling constructStrictHandling(final ComponentLog log) {
        return new StrictToleranceHandling(log);
    }

    /**
     * All three input maps must have the same key set. Moreover, there may be no tolerance given for Boolean input values.
     * 
     * @param inputValues The input values actually received by the EvaluationMemoryComponent
     * @param tolerances The tolerances for the input values. A mapping to null denotes that no tolerance is given for that value.
     * @param storedValues The values that should be checked for being in the tolerance intervals of inputValues.
     * @return True if storedValues is in the tolerance interval around inputValues, false if it is not
     */
    public boolean isInToleranceInterval(Map<String, TypedDatum> inputValues, Map<String, Double> tolerances,
        Map<String, TypedDatum> storedValues) {
        final Collection<String> inputs = inputValues.keySet();
        boolean allDataInToleranceInterval = true;

        for (final String input : inputs) {
            // Since we take the set inputs as the keySet of inputValues, we are certain that inputValues.get(input) does not yield an error
            final TypedDatum inputValue = inputValues.get(input);

            if (!tolerances.containsKey(input)) {
                final String errorString = String.format("Internal error: No tolerance found in Metadata for input %s", input);
                log.componentError(errorString);
                // We return false here since this may cause a re-evaluation, thus erring on the save side
                return false;
            }
            final Double tolerance = tolerances.get(input);

            if (!storedValues.containsKey(input)) {
                final String errorString = String.format("Internal error: No value found in stored vector for input %s", input);
                log.componentError(errorString);
                // We return false here since this may cause a re-evaluation, thus erring on the save side
                return false;
            }
            final TypedDatum storedValue = storedValues.get(input);

            allDataInToleranceInterval &= singleDatumInToleranceInterval(inputValue, tolerance, storedValue);
        }

        return allDataInToleranceInterval;
    }

    /**
     * @param inputValue The current input value, for which we check whether we have already stored a value
     * @param tolerance May be null, indicating that the inputValue needs to match the stored value precisely.
     * @param storedValue The stored value for which it shall be checked whether it is in the tolerance interval of inputValue.
     * @return True if ceil(inputValue - (1 + tolerance)) <= storedValue <= floor(inputValue + (1 + tolerance)), false otherwise
     */
    private boolean singleDatumInToleranceInterval(final TypedDatum inputValue, final Double tolerance, final TypedDatum storedValue) {
        if (tolerance == null || tolerance.floatValue() == 0.0) {
            return inputValue.equals(storedValue);
        } else {
            final float toleranceValue = tolerance.floatValue();

            final DataType inputType = inputValue.getDataType();
            final boolean singleDatumBelowUpperBound;
            final boolean singleDatumAboveLowerBound;

            // Since we only enable giving tolerances for floats and integers, we know at this point that ``input'' can only be of
            // either of these two types
            if (inputType.equals(DataType.Float)) {
                final double upperBound = ((FloatTD) inputValue).getFloatValue() * (1.0 + toleranceValue);
                singleDatumBelowUpperBound = ((FloatTD) storedValue).getFloatValue() <= upperBound;

                final double lowerBound = ((FloatTD) inputValue).getFloatValue() * (1.0 - toleranceValue);
                singleDatumAboveLowerBound = lowerBound <= ((FloatTD) storedValue).getFloatValue();
            } else {
                // Via the preconditions we know that the inputType is integer at this point
                final int upperBound = (int) (((IntegerTD) inputValue).getIntValue() * (1.0 + toleranceValue));
                singleDatumBelowUpperBound = ((IntegerTD) storedValue).getIntValue() <= upperBound;

                /*
                 * Since primitive conversion of doubles to integers always rounds down, but we aim to round the lower bound up in order
                 * to stay sound, we have to ``manually'' increment the lower bound after the conversion.
                 */
                final int lowerBound = (int) (((IntegerTD) inputValue).getIntValue() * (1.0 - toleranceValue));
                singleDatumAboveLowerBound = (lowerBound + 1) <= ((IntegerTD) storedValue).getIntValue();
            }

            return singleDatumBelowUpperBound && singleDatumAboveLowerBound;
        }
    }

    /**
     * 
     * @param candidates The candidate input vectors that are in the tolerance interval of inputValue. May be empty, but must not be null.
     * @param inputValue The concrete input value given in the current execution.
     * @return An element of ``candidates'' that is to be used for fetching stored results. May be null, which shall trigger a re-evaluation
     *         of the loop with the exact input values given.
     */
    public SortedMap<String, TypedDatum> pickMostToleratedInputs(Collection<SortedMap<String, TypedDatum>> candidates,
        SortedMap<String, TypedDatum> inputValue) {
        final SortedMap<String, TypedDatum> returnValue = choiceAlgorithm(candidates, inputValue);
        if (returnValue != null) {
            final String logMessage =
                String.format("Picked stored value '%s' as representative within tolerance interval of '%s'", returnValue, inputValue);
            log.componentInfo(logMessage);
        }
        return returnValue;
    }

    protected abstract SortedMap<String, TypedDatum> choiceAlgorithm(Collection<SortedMap<String, TypedDatum>> candidates,
        SortedMap<String, TypedDatum> inputValue);

}
