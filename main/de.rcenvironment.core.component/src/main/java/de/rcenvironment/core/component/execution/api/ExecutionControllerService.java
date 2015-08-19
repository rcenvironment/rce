/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;


/**
 * Controls execution of workflows or components. There is one {@link ExecutionControllerService} per node, which delegates requests to
 * {@link ExecutionController}s.
 * 
 * @author Doreen Seider
 */
public interface ExecutionControllerService {

    /**
     * Disposes a workflow/component.
     * 
     * @param executionId execution identifier of the component/workflow
     */
    void performDispose(String executionId);
    
    /**
     * Starts a workflow/component.
     * 
     * @param executionId execution identifier of the component/workflow
     */
    void performStart(String executionId);
    
    /**
     * Cancels a workflow/component.
     * 
     * @param executionId execution identifier of the component/workflow
     */
    void performCancel(String executionId);
    
    /**
     * Pauses a workflow/component.
     * 
     * @param executionId execution identifier of the component/workflow
     */
    void performPause(String executionId);
    
    /**
     * Resumes a workflow/component if it is paused.
     * 
     * @param executionId execution identifier of the component/workflow
     */
    void performResume(String executionId);
    
}
