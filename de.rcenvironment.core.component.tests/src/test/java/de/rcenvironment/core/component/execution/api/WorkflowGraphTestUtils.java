/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

import de.rcenvironment.core.datamodel.api.EndpointCharacter;

/**
 * Utility class that simplifies creation of {@link WorkflowGraphNode}s and {@link WorkflowGraphEdge}s for testing.
 *
 * @author Alexander Weinert
 */
public final class WorkflowGraphTestUtils {

    /**
     * The prefix used for naming inputs of generated workflow nodes. E.g., if a node has two inputs, they will be named INPUT_PREFIX + "0"
     * and INPUT_PREFIX + "1".
     */
    public static final String INPUT_PREFIX = "inp_";

    /**
     * The prefix used for naming outputs of generated workflow nodes. E.g., if a node has two outputs, they will be named OUTPUT_PREFIX +
     * "0" and OUTPUT_PREFIX + "1".
     */
    public static final String OUTPUT_PREFIX = "out_";
    
    /**
     * Private standard constructor, since this is a utility class that only contains static methods and should not be instantiated.
     */
    private WorkflowGraphTestUtils() { }

    static WorkflowGraphEdge createEdge(WorkflowGraphNode source, int outputNumber, EndpointCharacter outputType,
        WorkflowGraphNode target,
        int inputNumber, EndpointCharacter inputType) {
        String outputIdentifier = null;
        for (String output : source.getOutputIdentifiers()) {
            if (source.getEndpointName(output).equals(OUTPUT_PREFIX + outputNumber)) {
                outputIdentifier = output;
            }
        }
        String inputIdentifier = null;
        for (String input : target.getInputIdentifiers()) {
            if (target.getEndpointName(input).equals(INPUT_PREFIX + inputNumber)) {
                inputIdentifier = input;
            }
        }
        if (outputIdentifier != null && inputIdentifier != null) {
            return new WorkflowGraphEdge(source.getExecutionIdentifier(), outputIdentifier, outputType, target.getExecutionIdentifier(),
                inputIdentifier, inputType);
        }
        return null;
    }

    static WorkflowGraphNode createNewNode(int inputCount, int outputCount, boolean isDriver, String name) {
        Set<String> inputs = new HashSet<>();
        for (int i = 0; i < inputCount; i++) {
            inputs.add(UUID.randomUUID().toString());
        }
        Set<String> outputs = new HashSet<>();
        for (int i = 0; i < outputCount; i++) {
            outputs.add(UUID.randomUUID().toString());
        }
        Map<String, String> endpointnames = new HashMap<>();
        int i = 0;
        for (String input : inputs) {
            endpointnames.put(input, INPUT_PREFIX + i);
            i++;
        }
        i = 0;
        for (String output : outputs) {
            endpointnames.put(output, OUTPUT_PREFIX + i);
            i++;
        }
        return new WorkflowGraphNode(UUID.randomUUID().toString(), new ComponentExecutionIdentifier(UUID.randomUUID().toString()), inputs,
            outputs, endpointnames, isDriver, false, name);
    }

    static WorkflowGraphNode createNewNode(int inputCount, int outputCount, boolean isDriver) {
        return createNewNode(inputCount, outputCount, isDriver, RandomStringUtils.randomAlphabetic(5));
    }

}
