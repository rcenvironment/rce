/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;

import java.util.Objects;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * When constructing a function out of a workflow, one has to define the interface of the resulting function. This interface consists of a
 * number of endpoints of nodes of the encapsulated workflow, which may also be renamed. If, e.g., some workflow node `foo` has an input
 * called `bar`, the resulting function may expose that input as `baz`.
 * 
 * We call this renaming an EndpointAdapter. This value class encapsulates a mapping of a single such endpoint. Multiple
 * {@link EndpointAdapter}s are usually grouped together in an instance of {@link EndpointAdapters}.
 *
 * @author Alexander Weinert
 */
public class EndpointAdapter {

    public static class Builder {

        private final EndpointAdapter product = new EndpointAdapter();
        
        protected Builder() {}

        public Builder isInputAdapter(Boolean isInputAdapter) {
            this.product.isInputAdapter = isInputAdapter;
            return this;
        }

        public Builder internalEndpointName(String name) {
            this.product.internalEndpointName = name;
            return this;
        }

        public Builder externalEndpointName(String name) {
            this.product.externalEndpointName = name;
            return this;
        }

        public Builder workflowNodeIdentifier(String id) {
            this.product.workflowNodeIdentifier = id;
            return this;
        }

        public Builder dataType(DataType type) {
            this.product.adaptedDataType = type;
            return this;
        }

        public Builder inputHandling(InputDatumHandling inputDatumHandling) {
            this.product.datumHandling = inputDatumHandling;
            return this;
        }

        public Builder inputExecutionConstraint(InputExecutionContraint executionConstraint) {
            this.product.executionContraint = executionConstraint;
            return this;
        }

        private void assertProductIsValid() {
            Objects.requireNonNull(this.product.internalEndpointName);
            Objects.requireNonNull(this.product.externalEndpointName);
            Objects.requireNonNull(this.product.workflowNodeIdentifier);
//            Objects.requireNonNull(this.product.adaptedDataType); //TODO: Clarify if dataType is still required.

            if (product.isInputAdapter) {
                assertInputDefinitionsSet();
            }
        }

        private void assertInputDefinitionsSet() {
            if (this.product.datumHandling == null) {
                throw new IllegalStateException("Input handling must be set before constructing input adapter");
            }

            if (this.product.executionContraint == null) {
                throw new IllegalStateException("Input execution constraint must be set before constructing input adapter");
            }

        }

        public EndpointAdapter build() {
            assertProductIsValid();

            return this.product;
        }
    }

    private boolean isInputAdapter;

    private String workflowNodeIdentifier;

    private String internalEndpointName;

    private String externalEndpointName;

    private DataType adaptedDataType;

    private InputDatumHandling datumHandling;

    private InputExecutionContraint executionContraint;
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((workflowNodeIdentifier == null) ? 0 : workflowNodeIdentifier.hashCode());
        result = prime * result + ((internalEndpointName == null) ? 0 : internalEndpointName.hashCode());
        result = prime * result + ((String.valueOf(isInputAdapter) == null) ? 0 : String.valueOf(isInputAdapter).hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }
        EndpointAdapter other = (EndpointAdapter) obj;

        if (!workflowNodeIdentifier.equals(other.workflowNodeIdentifier)) {
            return false;
        }
        if (isInputAdapter != other.isInputAdapter) {
            return false;
        }
        if (!internalEndpointName.equals(other.internalEndpointName)) {
            return false;
        }
        return true;
    }

    public static Builder inputAdapterBuilder() {
        final Builder returnValue = new Builder();
        returnValue.product.isInputAdapter = true;
        return returnValue;
    }
    
    public static Builder outputAdapterBuilder() {
        final Builder returnValue = new Builder();
        returnValue.product.isInputAdapter = false;
        return returnValue;
    }

    public static Builder adapterBuilder() {
        return new Builder();

    }

    @Override
    public String toString() {
        if (this.isInputAdapter) {
            return StringUtils.format("%s --[%s,%s,%s]-> %s @ %s", this.getExternalName(), this.getDataType(), this.datumHandling,
                this.executionContraint, this.getInternalName(), this.getWorkflowNodeIdentifier());
        } else {
            return StringUtils.format("%s @ %s --[%s]-> %s", this.getInternalName(), this.getWorkflowNodeIdentifier(), this.getDataType(),
                this.getExternalName());
        }
    }

    public boolean isInputAdapter() {
        return isInputAdapter;
    }

    public boolean isOutputAdapter() {
        return !isInputAdapter;
    }

    /**
     * @return The identifier of the component whose endpoint is adapted
     */
    public String getWorkflowNodeIdentifier() {
        return this.workflowNodeIdentifier;
    }

    /**
     * @return The name of the adapted endpoint
     */
    public String getInternalName() {
        return this.internalEndpointName;
    }

    /**
     * @return The name of the exposed endpoint
     */
    public String getExternalName() {
        return this.externalEndpointName;
    }

    public DataType getDataType() {
        return this.adaptedDataType;
    }

    public InputExecutionContraint getInputExecutionConstraint() {
        return this.executionContraint;
    }

    public InputDatumHandling getInputDatumHandling() {
        return this.datumHandling;
    }

    public void setExternalName(String externalName) {
        this.externalEndpointName = externalName;
    }
}
