/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;

import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

public interface WorkflowFunction {

    public interface Builder {

        Builder withWorkflowDescription(WorkflowDescription description);

        Builder withEndpointAdapters(EndpointAdapters endpointAdapterDefinitionsMap);

        Builder withInternalName(String internalName);

        Builder withExternalName(String externalName);

        Builder withCallingWorkflowName(String callingWorkflowName);

        Builder setComponentContext(ComponentContext context);

        WorkflowFunction build();
    }

    WorkflowFunctionResult execute(WorkflowFunctionInputs inputs) throws WorkflowFunctionException;

}
