/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.FinalWorkflowState;

/**
 * Identifier for a workflow run.
 * 
 * @author Jan Flink
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

    private final Boolean hasDataReferences;

    private final Boolean markedForDeletion;

    public WorkflowRunDescription(Long workflowRunID, String workflowTitle, String controllerNodeID,
        String datamanagementNodeID, Long startTime, Long endtime, FinalWorkflowState finalState, Boolean hasDataReferences,
        Boolean markedForDeletion) {
        this.workflowRunID = workflowRunID;
        this.workflowTitle = workflowTitle;
        this.controllerNodeID = controllerNodeID;
        this.datamanagementNodeID = datamanagementNodeID;
        this.startTime = startTime;
        this.endTime = endtime;
        this.finalState = finalState;
        this.hasDataReferences = hasDataReferences;
        this.markedForDeletion = markedForDeletion;
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

    public Long getWorkflowRunID() {
        return workflowRunID;
    }

    public FinalWorkflowState getFinalState() {
        return finalState;
    }

    public Boolean getHasDataReferences() {
        return hasDataReferences;
    }

    public Boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    @Override
    public int compareTo(WorkflowRunDescription arg0) {
        return getStartTime().compareTo(arg0.getStartTime());
    }
}
