/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.datamodel.api.FinalWorkflowState;

/**
 * Representation for a workflow run holding its related {@link ComponentInstance}s and {@link ComponentRun}s.
 * 
 * @author Jan Flink
 * @author Brigitte Boden
 */
public class WorkflowRun extends WorkflowRunDescription implements Serializable {

    private static final long serialVersionUID = -6595800618979107754L;

    private Map<ComponentInstance, Set<ComponentRun>> componentRuns;
    
    private String wfFileReference;

    public WorkflowRun(Long workflowRunID, String workflowTitle, String controllerID, String datamanagementID, Long startTime,
        Long endtime, FinalWorkflowState finalState, Boolean hasDataReferences, Boolean markedForDeletion, Map<String, String> metaData,
        String wfFileReference) {
        super(workflowRunID, workflowTitle, controllerID, datamanagementID, startTime, endtime, finalState, hasDataReferences,
            markedForDeletion, metaData);
        this.wfFileReference = wfFileReference;
        componentRuns = new HashMap<ComponentInstance, Set<ComponentRun>>();
    }

    private void addComponentInstance(ComponentInstance componentInstance) {
        componentRuns.put(componentInstance, new HashSet<ComponentRun>());
    }

    /**
     * Adds a {@link ComponentRun} to the tree set related to the given {@link ComponentInstance}.
     * 
     * @param componentInstance The {@link ComponentInstance} key to add the {@link ComponentRun} to.
     * @param componentRun The {@link ComponentRun} to add.
     */
    public void addComponentRun(ComponentInstance componentInstance, ComponentRun componentRun) {
        if (!componentRuns.containsKey(componentInstance)) {
            addComponentInstance(componentInstance);
        }
        componentRuns.get(componentInstance).add(componentRun);
    }

    public Map<ComponentInstance, Set<ComponentRun>> getComponentRuns() {
        return componentRuns;
    }
    
    public String getWfFileReference() {
        return wfFileReference;
    }
}
