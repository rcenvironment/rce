/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Describes a workflow timeline.
 * 
 * @author Doreen Seider
 */
public class WorkflowRunTimline implements Serializable {

    private static final long serialVersionUID = 1579827010892774508L;

    private final String workflowRunName;

    private final TimelineInterval workflowRunInterval;

    private final List<ComponentRunInterval> componentRunIntervals;

    public WorkflowRunTimline(String workflowRunName, TimelineInterval workflowRunInterval,
        List<ComponentRunInterval> componentRunIntervals) {
        this.workflowRunName = workflowRunName;
        this.workflowRunInterval = workflowRunInterval;
        this.componentRunIntervals = componentRunIntervals;
    }

    /**
     * @return {@link TimelineInterval} related to this workflow run
     */
    public TimelineInterval getWorkflowRunInterval() {
        return workflowRunInterval;
    }

    /**
     * @return name of the workflow run
     */
    public String getWorkflowRunName() {
        return workflowRunName;
    }

    /**
     * @return {@link ComponentRunInterval}s related to components of this workflow run.
     */
    public List<ComponentRunInterval> getComponentRunIntervalsSortedByTime() {
        Collections.sort(componentRunIntervals);
        return componentRunIntervals;
    }
}
