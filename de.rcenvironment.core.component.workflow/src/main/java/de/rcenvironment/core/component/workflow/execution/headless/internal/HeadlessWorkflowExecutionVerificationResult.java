/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.File;
import java.util.List;

/**
 * Used to get the verification results of the workflow execution.
 * 
 * @author Doreen Seider
 */
public interface HeadlessWorkflowExecutionVerificationResult {

    /**
     * @return <code>true</code> if all of the workflows behaved as expected during workflow execution, otherwise <code>false</code>
     */
    boolean isVerified();

    /**
     * @return the verification results as text representation
     */
    String getVerificationReport();
    
    /**
     * @return list of workflows files that can be deleted as their execution behaved as expected
     */
    List<File> getWorkflowRelatedFilesToDelete();
    
}
