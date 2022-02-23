/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.UUID;

import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;

/**
 * Mock for {@link ComponentExecutionContext}, which provides some basic information either as default values or on the base of the values
 * passed into the constructor.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionContextMock extends ComponentExecutionContextDefaultStub {

    private static final long serialVersionUID = 856095970702698352L;

    private final String executionId;

    private final String instanceName;

    private final String wfExecutionId;

    private final String wfInstanceName;

    public ComponentExecutionContextMock() {
        this("My Component", "My Workflow");
    }

    public ComponentExecutionContextMock(String instanceName, String wfInstanceName) {
        this.instanceName = instanceName;
        this.wfInstanceName = wfInstanceName;
        this.executionId = UUID.randomUUID().toString();
        this.wfExecutionId = UUID.randomUUID().toString();
    }
    
    public ComponentExecutionContextMock(String executionId) {
        this.instanceName = "My Component";
        this.wfInstanceName = "My Workflow";
        this.executionId = executionId;
        this.wfExecutionId = UUID.randomUUID().toString();
    }

    @Override
    public String getExecutionIdentifier() {
        return executionId;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return wfExecutionId;
    }

    @Override
    public String getWorkflowInstanceName() {
        return wfInstanceName;
    }

}
