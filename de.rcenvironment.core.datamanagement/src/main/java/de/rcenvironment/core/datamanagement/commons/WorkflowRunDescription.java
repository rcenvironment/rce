/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.Map;

import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;

/**
 * Identifier for a workflow run.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 * @author Brigitte Boden
 */

public class WorkflowRunDescription implements Serializable, Comparable<WorkflowRunDescription> {

    private static final long serialVersionUID = 2449195140856943911L;

    private final Long workflowRunID;

    private final String workflowTitle;

    private final String controllerLogicalNodeId;

    // as this object is created from a string for on the server side anyway, only parse this id on demand on the client side
    private transient LogicalNodeId controllerLogicalNodeIdObject;

    private final String storageLogicalNodeId;

    // as this object is created from a string for on the server side anyway, only parse this id on demand on the client side
    private transient LogicalNodeId storageLogicalNodeIdObject;

    private final Long startTime;

    private final Long endTime;

    private final FinalWorkflowState finalState;

    private final Boolean areFilesDeleted;

    private final Boolean markedForDeletion;

    private final Map<String, String> metaData;

    public WorkflowRunDescription(Long workflowRunID, String workflowTitle, String controllerLogicalNodeId,
        String storageLogicalNodeId, Long startTime, Long endTime, FinalWorkflowState finalState, Boolean areFilesDeleted,
        Boolean markedForDeletion, Map<String, String> metaData) {
        this.workflowRunID = workflowRunID;
        this.workflowTitle = workflowTitle;
        this.controllerLogicalNodeId = controllerLogicalNodeId;
        this.storageLogicalNodeId = storageLogicalNodeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.finalState = finalState;
        this.areFilesDeleted = areFilesDeleted;
        this.markedForDeletion = markedForDeletion;
        this.metaData = metaData;
    }

    /**
     * Returns a copy of the given {@link WorkflowRunDescription} that shares all field values with the original, except for
     * {@link #controllerLogicalNodeId} and {@link #storageLogicalNodeId}, which are replaced by a new value. This method is used to fix
     * inconsistent node ids received from remote nodes, as they are returned by the remote data management as-is with no further checks.
     * 
     * @param original the original to copy values from
     * @param newNodeIdString the new {@link #controllerLogicalNodeId} and {@link #storageLogicalNodeId} to set
     * @return the copy
     */
    public static WorkflowRunDescription cloneAndReplaceNodeIds(WorkflowRunDescription original, String newNodeIdString) {
        return new WorkflowRunDescription(original.workflowRunID, original.workflowTitle, newNodeIdString, newNodeIdString,
            original.startTime, original.endTime, original.finalState, original.areFilesDeleted, original.markedForDeletion,
            original.getMetaData());
    }

    public String getWorkflowTitle() {
        return workflowTitle;
    }

    public String getControllerLogicalNodeIdString() {
        return controllerLogicalNodeId;
    }

    /**
     * @return returns the {@link LogicalNodeId} representation of the stored instance or logical node id representing the execution
     *         location; this should be used in place of {@link #getLogicalNodeIdString()} for type safety
     */
    public synchronized LogicalNodeId getControllerLogicalNodeId() {
        if (controllerLogicalNodeIdObject == null && controllerLogicalNodeId != null) { // also return null in case the string id is ever
                                                                                        // null
            try {
                controllerLogicalNodeIdObject = NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeId(controllerLogicalNodeId);
            } catch (IdentifierException e) {
                throw new RuntimeException("Failed to parse component run location string (expected an instance id or logical node id)", e);
            }
        }
        return controllerLogicalNodeIdObject;
    }

    public String getStorageLogicalNodeIdString() {
        return storageLogicalNodeId;
    }

    /**
     * @return returns the {@link LogicalNodeId} representation of the stored instance or logical node id representing the execution
     *         location; this should be used in place of {@link #getLogicalNodeIdString()} for type safety
     */
    public synchronized LogicalNodeId getStorageLogicalNodeId() {
     // return null in case the string id is ever
        // null
        if (storageLogicalNodeIdObject == null && storageLogicalNodeId != null) { 
            try {
                storageLogicalNodeIdObject = NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeId(storageLogicalNodeId);
            } catch (IdentifierException e) {
                throw new RuntimeException("Failed to parse component run location string (expected an instance id or logical node id)", e);
            }
        }
        return storageLogicalNodeIdObject;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public Long getWorkflowRunID() {
        return workflowRunID;
    }

    public FinalWorkflowState getFinalState() {
        return finalState;
    }

    public Boolean getAreFilesDeleted() {
        return areFilesDeleted;
    }

    public Boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    /**
     * Checks if the properties contain a field with additional information.
     * 
     * @return the value for additional information, if available, and null, else.
     */
    public String getAdditionalInformationIfAvailable() {
        if (metaData.containsKey(PropertiesKeys.ADDITIONAL_INFORMATION)) {
            return metaData.get(PropertiesKeys.ADDITIONAL_INFORMATION);
        }
        return null;
    }

    /**
     * Checks if the properties contain a field with an error log file.
     * 
     * @return the reference to the error log file, if available, and null, else.
     */
    public String getErrorLogFileReference() {
        return metaData.get(PropertiesKeys.ERROR_LOG_FILE);
    }

    @Override
    public int compareTo(WorkflowRunDescription arg0) {
        return getStartTime().compareTo(arg0.getStartTime());
    }

}
