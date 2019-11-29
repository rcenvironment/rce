/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.impl;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.execution.api.ExecutionHandle;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;

/**
 * Common {@link ExecutionHandle} implementation. If workflow and component handle interfaces should diverge at some point, this class
 * should be made an abstract superclass implementing the common methods, with the specific implementations subclassing it.
 *
 * @author Robert Mischke
 */
public class ExecutionHandleImpl implements WorkflowExecutionHandle {

    private static final long serialVersionUID = 7692083783888766107L;

    private String identifier;

    private LogicalNodeId location;

    public ExecutionHandleImpl(String identifier, LogicalNodeId location) {
        this.identifier = identifier;
        this.location = location;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public LogicalNodeId getLocation() {
        return location;
    }

}
