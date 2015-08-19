/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
     */
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
