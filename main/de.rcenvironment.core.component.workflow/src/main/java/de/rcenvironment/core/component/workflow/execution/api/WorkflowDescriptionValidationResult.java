/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Container for a workflow description validation result. It contains, whether the validation succeeded. If it didn't, it contains the
 * {@link NodeIdentifier} of the {@link WorkflowNode}s, which are not available (might be extended, if the validation covers others things
 * as well).
 * 
 * @author Doreen Seider
 */
public class WorkflowDescriptionValidationResult {

    private boolean succeeded;
    
    private NodeIdentifier missingControllerNodeId = null;
    
    private Map<String, NodeIdentifier> missingComponentsNodeIds = null;

    public WorkflowDescriptionValidationResult(boolean validationSucceeded, NodeIdentifier missingControllerNodeId,
        Map<String, NodeIdentifier> missingComponentsNodeIds) {
        this.succeeded = validationSucceeded;
        this.missingControllerNodeId = missingControllerNodeId;
        this.missingComponentsNodeIds = missingComponentsNodeIds;
    }
    
    public boolean isSucceeded() {
        return succeeded;
    }

    public NodeIdentifier getMissingControllerNodeIds() {
        return missingControllerNodeId;
    }

    public Map<String, NodeIdentifier> getMissingComponentsNodeIds() {
        return missingComponentsNodeIds;
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (missingControllerNodeId != null) {
            buffer.append(StringUtils.format("Workflow controller -> %s\n", missingControllerNodeId.getAssociatedDisplayName()));
        }
        if (missingComponentsNodeIds != null) {
            for (String wfNodeName : missingComponentsNodeIds.keySet()) {
                buffer.append(StringUtils.format("%s -> %s\n", wfNodeName, 
                    missingComponentsNodeIds.get(wfNodeName).getAssociatedDisplayName()));
            }
        }
        return buffer.toString().trim();
    }

    /**
     * Creates {@link WorkflowDescriptionValidationResult} instance in case of failure.
     * 
     * @param missingControllerNodeId {@link NodeIdentifier} if the controller {@link NodeIdentifier} is not available
     * @param missingComponentsNodeIds all of the component's {@link NodeIdentifier}s which are not available (workflow node id ->
     *        {@link NodeIdentifier})
     * @return {@link WorkflowDescriptionValidationResult} instance, initiated properly
     */
    public static WorkflowDescriptionValidationResult createResultForFailure(NodeIdentifier missingControllerNodeId,
        Map<String, NodeIdentifier> missingComponentsNodeIds) {
        return new WorkflowDescriptionValidationResult(false, missingControllerNodeId, missingComponentsNodeIds);
    }
    
    /**
     * Creates {@link WorkflowDescriptionValidationResult} instance in case of success.
     * 
     * @return {@link WorkflowDescriptionValidationResult} instance, initiated properly
     */
    public static WorkflowDescriptionValidationResult createResultForSuccess() {
        return new WorkflowDescriptionValidationResult(true, null, null);
    }
    
}
