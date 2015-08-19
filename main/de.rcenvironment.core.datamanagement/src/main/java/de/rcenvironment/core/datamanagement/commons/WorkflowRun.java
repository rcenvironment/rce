/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
 */
public class WorkflowRun extends WorkflowRunDescription implements Serializable {

    private static final long serialVersionUID = -6595800618979107754L;

    private Map<ComponentInstance, Set<ComponentRun>> componentRuns;

    public WorkflowRun(Long workflowRunID, String workflowTitle, String controllerID, String datamanagementID, Long startTime,
        Long endtime, FinalWorkflowState finalState, Boolean hasDataReferences, Boolean markedForDeletion) {
        super(workflowRunID, workflowTitle, controllerID, datamanagementID, startTime, endtime, finalState, hasDataReferences,
            markedForDeletion);
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
}
