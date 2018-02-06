/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Represents an hop in a cyclic workflow graph to traverse.
 * 
 * TODO A transition in the Graph is also represented though a WorkflowGraphEdge. Is the definition of this class really necessary, or can
 * we reuse WorkflowGraphEdge instead of WorklflowGraphHop? - Jan 2018, rode_to
 * 
 * @author Doreen Seider
 */
public class WorkflowGraphHop implements Serializable {

    private static final long serialVersionUID = 2180475342338990576L;

    private final String executionIdentifier;

    private final String ouputName;

    private final String targetExecutionIdentifier;

    private final String targetInputName;

    private final String hopOutputId;

    public WorkflowGraphHop(String hopExecutionIdentifier, String hopOuputName, String targetExecutionIdentifier, String targetInputName) {
        this(hopExecutionIdentifier, hopOuputName, targetExecutionIdentifier, targetInputName, null);
    }

    public WorkflowGraphHop(String hopExecutionIdentifier, String hopOuputName, String targetExecutionIdentifier, String targetInputName,
        String hopOutputId) {
        this.executionIdentifier = hopExecutionIdentifier;
        this.ouputName = hopOuputName;
        this.targetExecutionIdentifier = targetExecutionIdentifier;
        this.targetInputName = targetInputName;
        this.hopOutputId = hopOutputId;
    }

    public String getHopExecutionIdentifier() {
        return executionIdentifier;
    }

    public String getHopOuputName() {
        return ouputName;
    }

    public String getTargetExecutionIdentifier() {
        return targetExecutionIdentifier;
    }

    public String getTargetInputName() {
        return targetInputName;
    }

    protected String getHopOutputIdentifier() {
        return hopOutputId;
    }

    @Override
    public String toString() {
        return StringUtils.format("%s@%s -> %s@%s", ouputName, executionIdentifier, targetInputName, targetExecutionIdentifier);
    }

}
