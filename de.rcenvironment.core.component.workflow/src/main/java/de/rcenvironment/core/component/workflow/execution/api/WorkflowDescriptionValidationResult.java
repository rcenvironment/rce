/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Container for a workflow description validation result. It contains, whether the validation succeeded. If it didn't, it contains the
 * {@link InstanceNodeSessionId} of the {@link WorkflowNode}s, which are not available (might be extended, if the validation covers others
 * things as well).
 * 
 * @author Doreen Seider
 */
public class WorkflowDescriptionValidationResult {

    private boolean succeeded;

    private LogicalNodeId missingControllerNodeId = null;

    private Map<String, LogicalNodeId> missingComponentsNodeIds = new HashMap<>();

    public WorkflowDescriptionValidationResult(boolean validationSucceeded, LogicalNodeId missingControllerNodeId,
        Map<String, LogicalNodeId> missingComponentsNodeIds) {
        this.succeeded = validationSucceeded;
        this.missingControllerNodeId = missingControllerNodeId;
        this.missingComponentsNodeIds = missingComponentsNodeIds;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public LogicalNodeId getMissingControllerNodeId() {
        return missingControllerNodeId;
    }

    public Map<String, LogicalNodeId> getMissingComponentsNodeIds() {
        return missingComponentsNodeIds;
    }

    @Override
    public String toString() {
        StringBuilder causeLogMsg = new StringBuilder();
        if (missingControllerNodeId != null) {
            causeLogMsg
                .append(StringUtils.format("target instance for workflow controller unknown: %s", missingControllerNodeId));
        }
        for (String compName : missingComponentsNodeIds.keySet()) {
            if (causeLogMsg.length() > 0) {
                causeLogMsg.append(", ");
            }
            causeLogMsg.append(StringUtils.format("target instance for component unknown: %s -> %s", compName,
                missingComponentsNodeIds.get(compName)));
        }

        return causeLogMsg.toString().trim();
    }

    /**
     * Creates {@link WorkflowDescriptionValidationResult} instance in case of failure.
     * 
     * @param missingControllerNodeId {@link InstanceNodeSessionId} if the controller {@link InstanceNodeSessionId} is not available
     * @param missingComponentsNodeIds all of the component's {@link InstanceNodeSessionId}s which are not available (workflow node id ->
     *        {@link InstanceNodeSessionId})
     * @return {@link WorkflowDescriptionValidationResult} instance, initiated properly
     */
    public static WorkflowDescriptionValidationResult createResultForFailure(LogicalNodeId missingControllerNodeId,
        Map<String, LogicalNodeId> missingComponentsNodeIds) {
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
