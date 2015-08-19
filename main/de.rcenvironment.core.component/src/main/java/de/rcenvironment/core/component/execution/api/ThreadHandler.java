/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.util.concurrent.Future;

/**
 * Passed to {@link Component#onStartInterrupted(ThreadHandler)} and {@link Component#onProcessInputsInterrupted(ThreadHandler)} to enable a
 * component to interrupt the current thread executing {@link Component#start(ComponentContext)} or {@link Component#processInputs()}.
 * 
 * @author Doreen Seider
 */
public class ThreadHandler {
    
    private final Future<?> future;
    
    public ThreadHandler(Future<?> future) {
        this.future = future;
    }
    
    /**
     * Interrupts the associated thread.
     */
    public void interrupt() {
        future.cancel(true);
    }

}
