/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link InputConsumptionStrategy} considering a run, if certain or all {@link Input}s have a
 * value. If no specific inputs are used for construction (via
 * {@link CyclicInputConsumptionStrategy#CyclicInputConsumptionStrategy(String...)}), all
 * {@link Input}s must have a value.
 * 
 * @author Christian Weiss
 */
public class CyclicInputConsumptionStrategy {

    private Set<String> inputNames;

    private final Map<String, Deque<Input>> inputs = new HashMap<String, Deque<Input>>();

    public CyclicInputConsumptionStrategy(final String... inputNames) {
        for (final String inputName : inputNames) {
            inputs.put(inputName, null);
        }
    }

    public CyclicInputConsumptionStrategy(final Collection<String> inputNames) {
        this(inputNames.toArray(new String[0]));
    }

    /**
     * Sets the input names.
     * 
     * @param names the input name
     */
    public void setInputDefinitions(final Set<String> names) {
        this.inputNames = names;
        final boolean useAll = inputs.isEmpty();
        for (final String inputName : names) {
            if (useAll || inputs.containsKey(inputName)) {
                inputs.put(inputName, new LinkedBlockingDeque<Input>());
            }
        }
    }

    /**
     * Adds an {@link Input}.
     * 
     * @param input the {@link Input} to add
     */
    public void addInput(final Input input) {
        checkState();
        final String inputName = input.getName();
        assert inputNames.contains(inputName);
        if (!inputs.containsKey(inputName)) {
            throw new IllegalArgumentException(StringUtils.format("Unknown input (name='%s')"));
        }
        inputs.get(inputName).add(input);
    }

    /**
     * Returns, whether a run is indicated.
     * 
     * @return true, if a run is indicated
     */
    public boolean canRun() {
        checkState();
        boolean result = true;
        for (final String inputName : inputs.keySet()) {
            if (inputs.get(inputName).size() < 1) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Returns the {@link Input}s for the current run.
     * 
     * @return the {@link Input}s for the current run
     */
    public Map<String, List<Input>> peekInputs() {
        checkState();
        final Map<String, List<Input>> result = new HashMap<String, List<Input>>();
        for (final String inputName : inputs.keySet()) {
            final List<Input> inputList = new ArrayList<Input>(1);
            inputList.add(inputs.get(inputName).peek());
            result.put(inputName, inputList);
        }
        return result;
    }

    /**
     * Returns the {@link Input}s for the current run.
     * 
     * @return the {@link Input}s for the current run
     */
    public Map<String, List<Input>> popInputs() {
        checkState();
        final Map<String, List<Input>> result = new HashMap<String, List<Input>>();
        for (final String inputName : inputs.keySet()) {
            final List<Input> inputList = new ArrayList<Input>(1);
            inputList.add(inputs.get(inputName).pop());
            result.put(inputName, inputList);
        }
        return result;
    }

    private void checkState() {
        if (inputNames == null) {
            throw new IllegalStateException();
        }
    }
    
}
