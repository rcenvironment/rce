/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

    private final ComponentExecutionIdentifier executionIdentifier;

    private final String ouputName;

    private final ComponentExecutionIdentifier targetExecutionIdentifier;

    private final String targetInputName;

    private final String hopOutputId;

    public WorkflowGraphHop(ComponentExecutionIdentifier sourceExecutionIdentifier, String sourceOutputName,
        ComponentExecutionIdentifier targetExecutionIdentifier, String targetInputName) {
        this(sourceExecutionIdentifier, sourceOutputName, targetExecutionIdentifier, targetInputName, null);
    }

    public WorkflowGraphHop(ComponentExecutionIdentifier hopExecutionIdentifier, String hopOuputName,
        ComponentExecutionIdentifier targetExecutionIdentifier, String targetInputName,
        String hopOutputId) {
        this.executionIdentifier = hopExecutionIdentifier;
        this.ouputName = hopOuputName;
        this.targetExecutionIdentifier = targetExecutionIdentifier;
        this.targetInputName = targetInputName;
        this.hopOutputId = hopOutputId;
    }

    public ComponentExecutionIdentifier getHopExecutionIdentifier() {
        return executionIdentifier;
    }

    public String getHopOuputName() {
        return ouputName;
    }

    public ComponentExecutionIdentifier getTargetExecutionIdentifier() {
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

    /**
     * @param other Some other graph hop in the same {@link WorkflowGraph} as this one.
     * @return True if the source node and the target node of this vertex and the other have the same execution identifiers. False
     *         otherwise.
     */
    public boolean connectsSameNodes(final WorkflowGraphHop other) {
        return this.executionIdentifier.equals(other.executionIdentifier)
            && this.targetExecutionIdentifier.equals(other.targetExecutionIdentifier);
    }

}
