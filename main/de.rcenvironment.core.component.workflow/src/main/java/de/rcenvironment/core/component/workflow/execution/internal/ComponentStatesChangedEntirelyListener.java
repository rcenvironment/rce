/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Set;

import de.rcenvironment.core.component.execution.api.ComponentState;

/**
 * Callback interface. Called on certain component lifecycle changes.
 * 
 * @author Doreen Seider
 */
public interface ComponentStatesChangedEntirelyListener {

    /**
     * Called if all relevant components are in {@link ComponentState#PREPARED}.
     */
    void onComponentStatesChangedCompletelyToPrepared();

    /**
     * Called if all relevant components are in {@link ComponentState#PAUSED}.
     */
    void onComponentStatesChangedCompletelyToPaused();
    
    /**
     * Called if all relevant components running are in any final {@link ComponentState}.
     */
    void onComponentStatesChangedCompletelyToResumed();
    
    /**
     * Called if all relevant components are in {@link ComponentState#FINISHED} or in {@link ComponentState#FINISHED_WITHOUT_EXECUTION}.
     */
    void onComponentStatesChangedCompletelyToFinished();

    /**
     * Called if all relevant components are in {@link ComponentState#DISPOSED}.
     */
    void onComponentStatesChangedCompletelyToDisposed();
    
    /**
     * Called if all relevant components are in any final {@link ComponentState}.
     */
    void onComponentStatesChangedCompletelyToAnyFinalState();
    
    /**
     * Called if last console row was received from all components.
     */
    void onLastConsoleRowsReceived();
    
    /**
     * Called if component(s) is/are lost.
     * 
     * @param componentsLost execution identifier of lost components
     */
    void onComponentsLost(Set<String> componentsLost);

}
