/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;

public interface SynchronousWorkflowExecutionService {

    /**
     * Executes a workflow and awaits its termination. This termination may occur either because no components await execution (e.g., due to
     * normal termination or due to failure of some components), or because no regular heartbeat was received from the workflow
     * 
     * @param workflowExecutionContext The execution context describing the workflow to be executed
     * @return True if the workflow execution succeeded, false otherwise
     * @throws ComponentException // TODO
     */
    boolean executeWorkflow(WorkflowExecutionContext workflowExecutionContext) throws ComponentException;

}
