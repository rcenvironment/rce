/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

/**
 * Controls execution of a workflow or a component.
 *
 * @author Doreen Seider
 */
public interface ExecutionController {

    /**
     * Starts the workflow/component.
     */
    void start();
    
    /**
     * Pauses the workflow/component.
     */
    void pause();

    /**
     * Resumes the workflow/component.
     */
    void resume();
    
    /**
     * Restarts the workflow/component.
     * 
     * @deprecated will be removed in 8.0
     */
    @Deprecated
    void restart();

    /**
     * Cancels the workflow/component.
     */
    void cancel();
    
    /**
     * Disposes the workflow/component.
     */
    void dispose();
        
}
