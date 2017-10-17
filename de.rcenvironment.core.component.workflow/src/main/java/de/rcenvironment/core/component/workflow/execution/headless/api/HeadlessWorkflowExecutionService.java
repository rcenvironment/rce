/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.api;

import java.io.File;
import java.util.Set;

import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationRecorder;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationResult;

/**
 * Service for executing workflow files without a graphical interface.
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
     * @param headlessWfExeContext {@link HeadlessWorkflowExecutionContext} with data used for headless workflow execution
     * @return the state that the workflow finished with
     * @throws WorkflowExecutionException on execution failure
     */
    FinalWorkflowState executeWorkflowSync(HeadlessWorkflowExecutionContext headlessWfExeContext) throws WorkflowExecutionException;

    /**
     * @param headlessWfExeContexts {@link HeadlessWorkflowExecutionContext}s with data used for headless workflow execution
     * @param wfVerificationResultReorder {@link HeadlessWorkflowExecutionVerificationRecorder} related to the given
     *        {@link HeadlessWorkflowExecutionContext}s
     * @return the state that the workflow finished with
     */
    HeadlessWorkflowExecutionVerificationResult executeWorkflowsAndVerify(Set<HeadlessWorkflowExecutionContext> headlessWfExeContexts,
        HeadlessWorkflowExecutionVerificationRecorder wfVerificationResultReorder);

    /**
     * Deletes workflow run from data management.
     * 
     * @param wfDataManagementId data management identifier of workflow run
     * @param nodeId {@link ResolvableNodeId} of data management the workflow is stored
     */
    void delete(Long wfDataManagementId, ResolvableNodeId nodeId);

}
