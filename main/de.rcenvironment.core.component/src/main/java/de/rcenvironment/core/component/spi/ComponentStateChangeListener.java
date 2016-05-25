/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.spi;

import de.rcenvironment.core.component.execution.api.ComponentState;

/**
 * Listener for {@link ComponentState} changes.
 *
 * @author Doreen Seider
 */
public interface ComponentStateChangeListener {

    /**
     * Called on {@link ComponentState} changes. If {@link ComponentState} changes to {@link ComponentState#FAILED}
     * {@link ComponentStateChangeListener#onComponentStateChangedToFailed(String, Throwable)} will be additionally called.
     * 
     * @param compInfoId identifier of component instance
     * @param newState new {@link ComponentState}
     */
    void onHealthyComponentStateChanged(String compInfoId, ComponentState newState);
    
    /**
     * Called on {@link ComponentState} changes.
     * 
     * @param compInfoId identifier of component instance
     * @param t optional {@link Throwable} if one was thrown. <code>null</code> if no {@link Throwable} was thrown
     */
    void onComponentStateChangedToFailed(String compInfoId, Throwable t);
}
