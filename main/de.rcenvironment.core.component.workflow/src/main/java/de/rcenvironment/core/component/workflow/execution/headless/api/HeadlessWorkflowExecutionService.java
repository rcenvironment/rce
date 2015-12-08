/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.api;

import java.io.File;

import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;

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
        OnFinished;
    }

    /**
     * Delete behavior after workflow execution.
     * 
     * @author Sascha Zur
     */
    enum DeletionBehavior {
        Always,
        Never,
        OnFinished;
    }

    /**
     * Checks whether the given file is a proper placeholder values file.
     * 
     * @param placeholdersFile the file to check
     * @throws WorkflowFileException if the file is invalid
     */
    void validatePlaceholdersFile(File placeholdersFile) throws WorkflowFileException;

    /**
     * @param healdessWfExeContext {@link HeadlessWorkflowExecutionContext} with data used for
     *        headless workflow execution
     * @return the state that the workflow finished with
     * @throws WorkflowExecutionException on execution failure
     */
    FinalWorkflowState executeWorkflowSync(HeadlessWorkflowExecutionContext healdessWfExeContext) throws WorkflowExecutionException;

}
