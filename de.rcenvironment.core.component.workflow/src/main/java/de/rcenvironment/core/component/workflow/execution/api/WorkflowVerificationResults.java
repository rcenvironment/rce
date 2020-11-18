/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;

import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationResult;

public class WorkflowVerificationResults {
    
    private final HeadlessWorkflowExecutionVerificationResult result;
    
    WorkflowVerificationResults(HeadlessWorkflowExecutionVerificationResult wfVerificationResultRecorder) {
        this.result = wfVerificationResultRecorder;
    }

    public static WorkflowVerificationResults fromHeadlessWorkflowExecutionVerificationResult(
        HeadlessWorkflowExecutionVerificationResult wfVerificationResultRecorder) {
        return new WorkflowVerificationResults(wfVerificationResultRecorder);
    }


    public String getVerificationReport() {
        return result.getVerificationReport();
    }

    public boolean isVerified() {
        return result.isVerified();
    }

    public Iterable<File> getWorkflowRelatedFilesToDelete() {
        return result.getWorkflowRelatedFilesToDelete();
    }

}
