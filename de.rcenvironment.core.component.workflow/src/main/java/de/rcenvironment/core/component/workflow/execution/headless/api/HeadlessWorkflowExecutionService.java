/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.api;

import java.io.File;
import java.util.Set;

import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationRecorder;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationResult;

/**
 * Service for executing workflows with predefined disposal and delete behaviors, and additional features like collecting workflow results
 * for verification.
 * 
 * Note that the naming scheme of this interface and its related classes is somewhat misleading: The additional features provided by them
 * are not actually related to non-GUI ("headless") execution, although they are frequently used in related scenarios like console command
 * execution. -- misc_ro
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface HeadlessWorkflowExecutionService extends WorkflowExecutionService {

    /**
     * Dispose behavior after workflow execution.
     * 
     * @author Doreen Seider
     */
    enum DisposalBehavior {
        Always,
        Never,
        OnExpected;
    }

    /**
     * Delete behavior after workflow execution.
     * 
     * @author Sascha Zur
     */
    enum DeletionBehavior {
        Always,
        Never,
        OnExpected;
    }

    /**
     * Checks whether the given file is a proper placeholder values file.
     * 
     * @param placeholdersFile the file to check
     * @throws WorkflowFileException if the file is invalid
     */
    void validatePlaceholdersFile(File placeholdersFile) throws WorkflowFileException;

    /**
     * Starts the given workflow execution and waits for its termination.
     * 
     * @param headlessWfExeContext {@link HeadlessWorkflowExecutionContext} with data used for headless workflow execution
     * @return the state that the workflow finished with
     * @throws WorkflowExecutionException on execution failure
     */
    FinalWorkflowState executeWorkflow(HeadlessWorkflowExecutionContext headlessWfExeContext) throws WorkflowExecutionException;

    /**
     * Starts workflow execution (prepareContextForExecution has to be run first to get extended execution context).
     * 
     * @param headlessWfExeContext {@link HeadlessWorkflowExecutionContext} with data used for headless workflow execution
     * @return workflow execution information object
     * @throws WorkflowExecutionException on execution failure
     */
    WorkflowExecutionInformation startHeadlessWorkflowExecution(HeadlessWorkflowExecutionContext headlessWfExeContext)
        throws WorkflowExecutionException;

    /**
     * Waits for termination of workflow and performs cleanup operations. Workflow has to be started first by method startWorkflowExecution.
     * 
     * @param headlessWfExeContext {@link HeadlessWorkflowExecutionContext} with data used for headless workflow execution
     * @return the state that the workflow finished with
     * @throws WorkflowExecutionException on execution failure
     */
    FinalWorkflowState waitForWorkflowTerminationAndCleanup(HeadlessWorkflowExecutionContext headlessWfExeContext)
        throws WorkflowExecutionException;

    /**
     * @param headlessWfExeContexts {@link HeadlessWorkflowExecutionContext}s with data used for headless workflow execution
     * @param wfVerificationResultReorder {@link HeadlessWorkflowExecutionVerificationRecorder} related to the given
     *        {@link HeadlessWorkflowExecutionContext}s
     * @return the state that the workflow finished with
     */
    HeadlessWorkflowExecutionVerificationResult executeWorkflowsAndVerify(Set<HeadlessWorkflowExecutionContext> headlessWfExeContexts,
        HeadlessWorkflowExecutionVerificationRecorder wfVerificationResultReorder);


}
