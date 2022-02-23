/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.workflow.execution.function.testutils;

import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunction;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunction.Builder;

public class WorkflowFunctionBuilderMock implements WorkflowFunction.Builder {

    private WorkflowFunction product;

    @Override
    public Builder withWorkflowDescription(WorkflowDescription description) {
        return this;
    }

    @Override
    public Builder withEndpointAdapters(EndpointAdapters endpointAdapterDefinitionsMap) {
        return this;
    }

    @Override
    public Builder withInternalName(String internalName) {
        return this;
    }

    @Override
    public Builder withExternalName(String externalName) {
        return this;
    }

    @Override
    public Builder withCallingWorkflowName(String callingWorkflowName) {
        return this;
    }

    @Override
    public Builder setComponentContext(ComponentContext context) {
        return this;
    }

    public void setProduct(WorkflowFunction productParam) {
        this.product = productParam;
    }
    
    @Override
    public WorkflowFunction build() {
        return this.product;
    }
    
}