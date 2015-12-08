/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.Map;

import de.rcenvironment.core.datamodel.api.FinalWorkflowState;

/**
 * Identifier for a workflow run.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 */

public class WorkflowRunDescription implements Serializable, Comparable<WorkflowRunDescription> {

    private static final long serialVersionUID = 2449195140856943911L;

    private final Long workflowRunID;

    private final String workflowTitle;

    private final String controllerNodeID;

    private final String datamanagementNodeID;

    private final Long startTime;

    private final Long endTime;

    private final FinalWorkflowState finalState;

    private final Boolean areFilesDeleted;

    private final Boolean markedForDeletion;

    private final Map<String, String> metaData;

    public WorkflowRunDescription(Long workflowRunID, String workflowTitle, String controllerNodeID,
        String datamanagementNodeID, Long startTime, Long endTime, FinalWorkflowState finalState, Boolean areFilesDeleted,
        Boolean markedForDeletion, Map<String, String> metaData) {
        this.workflowRunID = workflowRunID;
        this.workflowTitle = workflowTitle;
        this.controllerNodeID = controllerNodeID;
        this.datamanagementNodeID = datamanagementNodeID;
        this.startTime = startTime;
        this.endTime = endTime;
        this.finalState = finalState;
        this.areFilesDeleted = areFilesDeleted;
        this.markedForDeletion = markedForDeletion;
        this.metaData = metaData;
    }

    /**
     * Returns a copy of the given {@link WorkflowRunDescription} that shares all field values with the original, except for
     * {@link #controllerNodeID} and {@link #datamanagementNodeID}, which are replaced by a new value. This method is used to fix
     * inconsistent node ids received from remote nodes, as they are returned by the remote data management as-is with no further checks.
     * 
     * @param original the original to copy values from
     * @param newNodeIdString the new {@link #controllerNodeID} and {@link #datamanagementNodeID} to set
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

    public String getControllerNodeID() {
        return controllerNodeID;
    }

    public String getDatamanagementNodeID() {
        return datamanagementNodeID;
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
