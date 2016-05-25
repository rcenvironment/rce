/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentState;

/**
 * Records stats related to component execution.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionStatsService {
    
    /**
     * Records stats at the time a component is started.
     * 
     * @param compExeCtx context of the workflow execution
     */
    void addStatsAtComponentStart(ComponentExecutionContext compExeCtx);

    /**
     * Records stats at the time a component run is started.
     * 
     * @param compExeCtx context of the workflow execution
     */
    void addStatsAtComponentRunStart(ComponentExecutionContext compExeCtx);

    /**
     * Records stats at the time a component run is terminated.
     * 
     * @param compExeCtx context of the workflow execution
     */
    void addStatsAtComponentRunTermination(ComponentExecutionContext compExeCtx);

    /**
     * Records stats at the time a component is terminated.
     * 
     * @param compExeCtx context of the component execution
     * @param finalWorkflowState final state of the component executed
     */
    void addStatsAtComponentTermination(ComponentExecutionContext compExeCtx, ComponentState finalWorkflowState);

}
