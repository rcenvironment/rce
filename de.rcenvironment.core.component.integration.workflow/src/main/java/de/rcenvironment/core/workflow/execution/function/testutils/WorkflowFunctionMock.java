/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.workflow.execution.function.testutils;

import de.rcenvironment.core.workflow.execution.function.WorkflowFunction;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionException;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionInputs;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionResult;
import org.easymock.Capture;
import org.easymock.CaptureType;

public class WorkflowFunctionMock implements WorkflowFunction {

    private WorkflowFunctionResult result;

    private Capture<WorkflowFunctionInputs> capturedInputs = Capture.newInstance(CaptureType.FIRST);

    @Override
    public WorkflowFunctionResult execute(WorkflowFunctionInputs inputs) throws WorkflowFunctionException {
        capturedInputs.setValue(inputs);
        return result;
    }
    
    public void setResult(final WorkflowFunctionResult resultParam) {
        this.result = resultParam;
    }
    
    public WorkflowFunctionInputs getCapturedInputs() {
        return this.capturedInputs.getValue();
    }

}
