/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.StringUtils;

public class WorkflowFunctionInputs {

    private final Map<String, TypedDatum> inputs;

    protected WorkflowFunctionInputs(final Map<String, TypedDatum> inputs) {
        this.inputs = inputs;
    }

    public static WorkflowFunctionInputs createFromMap(Map<String, TypedDatum> inputs) {
        final String invalidEntries = inputs.entrySet().stream()
            .filter(entry -> entry.getKey() == null || entry.getValue() == null)
            .map(entry -> StringUtils.format("{%s=%s}", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(", "));
        
        if (!invalidEntries.isEmpty()) {
            throw new IllegalArgumentException("WorkflowFunctionInputs contains the following invalid entries: " + invalidEntries
                + "\nEntries may not contain `null` as key or value.");
        }

        return new WorkflowFunctionInputs(new HashMap<>(inputs));
    }

    public TypedDatum getValueByName(final String name) {
        if (!this.inputs.containsKey(name)) {
            throw new IllegalArgumentException(
                StringUtils.format("Attempted to access value of input %s, but that input was not set", name));
        }

        return this.inputs.get(name);
    }

    public Iterable<String> getInputNames() {
        return this.inputs.keySet();
    }
}
