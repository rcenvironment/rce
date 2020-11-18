/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import de.rcenvironment.core.datamodel.api.TypedDatum;

public class WorkflowFunctionResult {
    private final boolean success;
    
    private final Map<String, TypedDatum> results = new HashMap<>();

    WorkflowFunctionResult(boolean success) {
        this.success = success;
    }

    public static final class Builder {
        private final WorkflowFunctionResult product = new WorkflowFunctionResult(true);
        
        private Builder() {}

        public Builder addResult(String name, TypedDatum value) {
            product.results.put(name, value);
            return this;
        }

        public WorkflowFunctionResult build() {
            return product;
        }
    }
    
    public static Builder successBuilder() {
        return new Builder();
    }

    public static WorkflowFunctionResult buildFailure() {
        return new WorkflowFunctionResult(false);
    }
    
    public boolean isFailure() {
        return !success;
    }
    
    public Map<String, TypedDatum> toMap() {
        return new HashMap<>(results);
    }

    public Collection<String> getResultIdentifiers() {
        return new HashSet<>(this.results.keySet());
    }
    
    public Optional<TypedDatum> getResultByIdentifier(String identifier) {
        return Optional.ofNullable(this.results.get(identifier));
    }
}
